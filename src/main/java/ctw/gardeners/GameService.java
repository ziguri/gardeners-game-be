package ctw.gardeners;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@ApplicationScoped
public class GameService {
  public record Target(String type, String id, String label, String group) {}
  public record Option(String id, String label) {}
  public record State(Long id, int level, int score, int helpsLeft, String status, Long cardId, String card, int remaining, int total, int stars, long seconds,
      boolean quiz, List<Option> answers, String explanation, List<Target> targets) {}
  public record Answer(boolean correct, String correctType, String correctId, String explanation) {}
  public record Row(Long gameId, String member, String team, int level, int score, int total, long correct, long seconds, String date) {}
  public record Lvl(int level, String scope, boolean unlocked, int stars, int best, long seconds) {}
  public record Levels(int seeds, int current, List<Lvl> levels) {}
  static final int CARDS = 10, POINTS = 10, LEVELS = 30;

  static String scopeOf(int l) { return l <= 10 ? "COMPANY" : l <= 20 ? "UNIT" : "PEOPLE"; }
  /** 50% correct = 1 star (unlocks next level), 70% = 2, 90% = 3. */
  static int stars(int score, int total) { if (total == 0) return 0; double r = (double) score / (total * POINTS); return r >= .9 ? 3 : r >= .7 ? 2 : r >= .5 ? 1 : 0; }
  static long secs(GameInstance g) { return Duration.between(g.createdAt, g.finishedAt == null ? LocalDateTime.now() : g.finishedAt).getSeconds(); }

  @Transactional public GameInstance create(Member m, int level) {
    if (level < 1 || level > LEVELS) throw new jakarta.ws.rs.BadRequestException("Unknown level");
    List<Curiosity> pool = new ArrayList<>(Curiosity.<Curiosity>list("scope", scopeOf(level)));
    if (pool.isEmpty()) throw new jakarta.ws.rs.BadRequestException("No facts for this level yet. Ask the admin to add some.");
    Collections.shuffle(pool);
    GameInstance g = new GameInstance(); g.player = Member.findById(m.id); g.level = level; g.persist();
    for (Curiosity c : pool.stream().limit(CARDS).toList()) { GameCard gc = new GameCard(); gc.game = g; gc.curiosity = c; gc.persist(); g.cards.add(gc); }
    return g;
  }

  public List<Target> targets(String scope) {
    if (!"PEOPLE".equals(scope))
      return Curiosity.<Curiosity>list("scope", scope).stream().filter(c -> !c.isQuiz()).map(c -> c.topic).filter(Objects::nonNull).distinct().map(t -> new Target("TOPIC", t, t, "COMPANY".equals(scope) ? "CTW" : "Gardeners")).toList();
    List<Target> t = new ArrayList<>();
    Team.<Team>listAll().stream().map(x -> x.location).distinct().forEach(l -> t.add(new Target("LOCATION", l, l, "Locations")));
    Team.<Team>listAll().forEach(x -> t.add(new Target("TEAM", x.id.toString(), x.name, x.location)));
    Member.<Member>listAll().stream().filter(x -> !"admin".equals(x.role)).forEach(x -> t.add(new Target("MEMBER", x.id.toString(), x.name, x.team == null ? "Leadership" : x.team.name)));
    return t;
  }

  private List<Option> answers(Curiosity c) {
    if (c == null || !c.isQuiz()) return List.of();
    List<Option> out = new ArrayList<>(List.of(
      new Option("A", c.answerA),
      new Option("B", c.answerB),
      new Option("C", c.answerC)
    ));
    out.removeIf(o -> o.label() == null || o.label().isBlank());
    Collections.shuffle(out);
    return out;
  }

  @Transactional public State state(GameInstance g) {
    GameCard next = g.cards.stream().filter(c -> c.correct == null).findFirst().orElse(null);
    long left = g.cards.stream().filter(c -> c.correct == null).count();
    Curiosity c = next == null ? null : next.curiosity;
    boolean quiz = c != null && c.isQuiz();
    List<Option> answers = quiz ? answers(c) : List.of();
    String explanation = quiz && c != null ? c.explanation : null;
    return new State(g.id, g.level, g.score, g.helpsLeft, g.status, next == null ? null : next.id, next == null ? null : next.curiosity.text,
      (int) left, g.cards.size(), stars(g.score, g.cards.size()), secs(g), quiz, answers, explanation, targets(scopeOf(g.level)));
  }

  @Transactional public Answer drop(GameInstance g, Long cardId, String type, String id) {
    g = GameInstance.findById(g.id);
    GameCard c = GameCard.findById(cardId);
    if (c == null || !c.game.id.equals(g.id) || c.correct != null) throw new jakarta.ws.rs.BadRequestException("Card not available");
    if (c.curiosity.isQuiz()) {
      boolean ok = c.curiosity.isCorrectAnswer(id);
      c.correct = ok;
      if (ok) g.score += POINTS;
      if (g.cards.stream().allMatch(x -> x.correct != null)) { g.status = "FINISHED"; g.finishedAt = LocalDateTime.now(); }
      return new Answer(ok, "ANSWER", c.curiosity.correctKey(), c.curiosity.explanation);
    }
    c.correct = c.curiosity.targetType.equals(type) && c.curiosity.targetId.equals(id);
    if (c.correct) g.score += POINTS;
    if (g.cards.stream().allMatch(x -> x.correct != null)) { g.status = "FINISHED"; g.finishedAt = LocalDateTime.now(); }
    return new Answer(c.correct, c.curiosity.targetType, c.curiosity.targetId, c.curiosity.explanation);
  }

  /** Help: 3 options of the same kind as the answer, exactly one is correct. */
  @Transactional public List<Target> help(GameInstance g, Long cardId) {
    g = GameInstance.findById(g.id);
    GameCard c = GameCard.findById(cardId);
    if (c == null || g == null) throw new jakarta.ws.rs.BadRequestException("No help available");
    if (c.curiosity.isQuiz()) {
      List<Target> out = new ArrayList<>();
      out.add(new Target("ANSWER", "A", c.curiosity.answerA == null ? "A" : c.curiosity.answerA, "Answer"));
      out.add(new Target("ANSWER", "B", c.curiosity.answerB == null ? "B" : c.curiosity.answerB, "Answer"));
      out.add(new Target("ANSWER", "C", c.curiosity.answerC == null ? "C" : c.curiosity.answerC, "Answer"));
      out.removeIf(t -> t.label() == null || t.label().isBlank());
      Collections.shuffle(out);
      return out;
    }
    if (g.helpsLeft <= 0 || c.helped || c.correct != null) throw new jakarta.ws.rs.BadRequestException("No help available");
    g.helpsLeft--; c.helped = true;
    List<Target> same = targets(scopeOf(g.level)).stream().filter(t -> t.type().equals(c.curiosity.targetType)).collect(Collectors.toList());
    Target right = same.stream().filter(t -> t.id().equals(c.curiosity.targetId)).findFirst().orElseThrow();
    same.remove(right); Collections.shuffle(same);
    List<Target> out = new ArrayList<>(same.stream().limit(2).toList()); out.add(right); Collections.shuffle(out);
    return out;
  }

  /** Seeds = sum of the best score per level. A level unlocks when the previous one has at least 1 star. */
  @Transactional public Levels levels(Member m) {
    List<GameInstance> done = GameInstance.<GameInstance>list("player.id", m.id).stream().filter(g -> "FINISHED".equals(g.status)).toList();
    List<Lvl> out = new ArrayList<>(); int seeds = 0, current = LEVELS, prev = 1; boolean found = false;
    for (int i = 1; i <= LEVELS; i++) {
      final int lv = i;
      List<GameInstance> gs = done.stream().filter(g -> g.level == lv).toList();
      int best = gs.stream().mapToInt(g -> g.score).max().orElse(0);
      int st = gs.stream().mapToInt(g -> stars(g.score, g.cards.size())).max().orElse(0);
      long sec = gs.stream().mapToLong(GameService::secs).min().orElse(0);
      boolean open = i == 1 || prev > 0;
      out.add(new Lvl(i, scopeOf(i), open, st, best, sec)); seeds += best;
      if (!found && open && st == 0) { current = i; found = true; }
      prev = st;
    }
    return new Levels(seeds, current, out);
  }

  @Transactional public List<Row> scores(Long teamId, Long memberId) {
    return GameInstance.<GameInstance>listAll().stream()
      .filter(g -> memberId == null || g.player.id.equals(memberId))
      .filter(g -> teamId == null || (g.player.team != null && g.player.team.id.equals(teamId)))
      .map(g -> new Row(g.id, g.player.name, g.player.team == null ? "Leadership" : g.player.team.name, g.level, g.score, g.cards.size(),
        g.cards.stream().filter(c -> Boolean.TRUE.equals(c.correct)).count(), "FINISHED".equals(g.status) ? secs(g) : 0, g.createdAt.toLocalDate().toString())).toList();
  }
}
