package ctw.gardeners;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/admin") @RolesAllowed("admin") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {
  @Inject GameService games;
  public record NewTeam(String name, String location, String productType, String description, List<String> curiosities) {}
  public record NewMember(String name, String username, String password, String jobTitle, Long teamId, String curiosity) {}
  public record NewGame(Long memberId, Integer level) {}
  public record NewFact(String scope, String topic, String text, String targetId,
      String question, String answerA, String answerB, String answerC, Integer correctAnswer, String explanation) {}
  public record FactView(Long id, String scope, String topic, String text, String targetType, String label,
      String question, String answerA, String answerB, String answerC, Integer correctAnswer, String explanation) {}
  public record EditFact(String scope, String topic, String text,
      String question, String answerA, String answerB, String answerC, Integer correctAnswer, String explanation) {}
  public record EditTeam(String name, String location, String productType, String description) {}
  public record EditMember(String name, String jobTitle, String password, Long teamId) {}
  public record GameRef(Long id) {}

  @GET @Path("teams") public List<Team> teams() { return Team.listAll(); }
  @GET @Path("members") public List<Member> members() { return Member.list("role", "member"); }

  @POST @Path("teams") @Transactional public Team createTeam(NewTeam n) {
    Team t = new Team(); t.name = n.name(); t.location = n.location(); t.productType = n.productType(); t.description = n.description(); t.persist();
    if (n.curiosities() != null) n.curiosities().forEach(c -> curiosity(c, "TEAM", t.id.toString()));
    return t;
  }
  @POST @Path("members") @Transactional public Member createMember(NewMember n) {
    Member m = Member.of(n.name(), n.username(), n.password(), "member", n.jobTitle());
    if (n.teamId() != null) m.team = Team.findById(n.teamId());
    m.persist();
    if (n.curiosity() != null && !n.curiosity().isBlank()) curiosity(n.curiosity(), "MEMBER", m.id.toString());
    return m;
  }
  /** Assign or move a member (teamId = 0 removes the member from any team). */
  @PUT @Path("members/{id}/team/{teamId}") @Transactional public Member move(@PathParam("id") Long id, @PathParam("teamId") Long teamId) {
    Member m = Member.findById(id); m.team = teamId == 0 ? null : Team.findById(teamId); return m;
  }
  @POST @Path("games") public GameRef createGame(NewGame n) { return new GameRef(games.create(Member.findById(n.memberId()), n.level() == null ? 1 : n.level()).id); }
  @GET @Path("scores") public List<GameService.Row> scores(@QueryParam("teamId") Long teamId, @QueryParam("memberId") Long memberId) { return games.scores(teamId, memberId); }

  @GET @Path("company") public Company company() { return Company.findAll().firstResult(); }
  @PUT @Path("company") @Transactional public Company saveCompany(Company c) { Company x = Company.findAll().firstResult(); if (x == null) { x = new Company(); x.persist(); } x.name = c.name; x.description = c.description; return x; }
  @GET @Path("facts") public List<FactView> facts() {
    return Curiosity.<Curiosity>listAll().stream().map(c -> new FactView(c.id, c.scope, c.topic, c.text, c.targetType, label(c),
      c.question, c.answerA, c.answerB, c.answerC, c.correctAnswer, c.explanation)).toList();
  }
  @POST @Path("facts") @Transactional public Curiosity addFact(NewFact f) {
    Curiosity c = new Curiosity();
    if (f.question() != null && !f.question().isBlank()) {
      c.question = f.question().trim();
      c.text = c.question;
      c.answerA = f.answerA() == null ? null : f.answerA().trim();
      c.answerB = f.answerB() == null ? null : f.answerB().trim();
      c.answerC = f.answerC() == null ? null : f.answerC().trim();
      c.correctAnswer = f.correctAnswer();
      c.explanation = f.explanation() == null ? null : f.explanation().trim();
      c.scope = "COMPANY";
      c.topic = "Company fact";
      c.targetType = "ANSWER";
      c.targetId = c.correctKey();
      c.company = Company.findAll().firstResult();
    } else {
      c.text = f.text();
      if ("TEAM".equals(f.scope()) || "MEMBER".equals(f.scope())) { c.scope = "PEOPLE"; c.targetType = f.scope(); c.targetId = String.valueOf(f.targetId()); }
      else {
        c.scope = f.scope(); c.topic = f.topic() == null ? "" : f.topic().trim(); c.targetType = "TOPIC"; c.targetId = c.topic;
        if ("COMPANY".equals(c.scope)) c.company = Company.findAll().firstResult();
      }
    }
    c.persist(); return c;
  }
  @PUT @Path("facts/{id}") @Transactional public void editFact(@PathParam("id") Long id, EditFact f) {
    Curiosity c = Curiosity.findById(id); 
    if (f.question() != null && !f.question().isBlank()) {
      c.question = f.question().trim();
      c.text = c.question;
      c.answerA = f.answerA() == null ? null : f.answerA().trim();
      c.answerB = f.answerB() == null ? null : f.answerB().trim();
      c.answerC = f.answerC() == null ? null : f.answerC().trim();
      c.correctAnswer = f.correctAnswer();
      c.explanation = f.explanation() == null ? null : f.explanation().trim();
      c.scope = "COMPANY";
      c.topic = "Company fact";
      c.targetType = "ANSWER";
      c.targetId = c.correctKey();
      c.company = Company.findAll().firstResult();
      return;
    }
    c.text = f.text();
    if ("TOPIC".equals(c.targetType)) {
      c.scope = f.scope(); c.topic = f.topic() == null ? "" : f.topic().trim(); c.targetId = c.topic;
      c.company = "COMPANY".equals(c.scope) ? Company.findAll().firstResult() : null;
    }
  }
  @DELETE @Path("facts/{id}") @Transactional public void delFact(@PathParam("id") Long id) { removeFact(Curiosity.findById(id)); }

  @PUT @Path("teams/{id}") @Transactional public Team editTeam(@PathParam("id") Long id, EditTeam e) {
    Team t = Team.findById(id); t.name = e.name(); t.location = e.location(); t.productType = e.productType(); t.description = e.description(); return t;
  }
  /** Removes the team's facts (and their game cards); its members stay, without a team. */
  @DELETE @Path("teams/{id}") @Transactional public void delTeam(@PathParam("id") Long id) {
    Curiosity.<Curiosity>list("targetType = 'TEAM' and targetId = ?1", String.valueOf(id)).forEach(AdminResource::removeFact);
    Member.<Member>list("team.id", id).forEach(m -> m.team = null);
    Team.deleteById(id);
  }
  @PUT @Path("members/{id}") @Transactional public Member editMember(@PathParam("id") Long id, EditMember e) {
    Member m = Member.findById(id); m.name = e.name(); m.jobTitle = e.jobTitle();
    m.team = e.teamId() == null ? null : Team.findById(e.teamId());
    if (e.password() != null && !e.password().isBlank()) {
      if (e.password().length() < 8) throw new BadRequestException("Minimum 8 characters");
      m.password = BcryptUtil.bcryptHash(e.password());
    }
    return m;
  }
  /** Removes the member, their games and the facts about them. */
  @DELETE @Path("members/{id}") @Transactional public void delMember(@PathParam("id") Long id) {
    Member m = Member.findById(id); if (m == null || "admin".equals(m.role)) throw new BadRequestException("Cannot delete this user");
    GameInstance.<GameInstance>list("player.id", id).forEach(g -> g.delete());
    Curiosity.<Curiosity>list("targetType = 'MEMBER' and targetId = ?1", String.valueOf(id)).forEach(AdminResource::removeFact);
    m.delete();
  }

  static String label(Curiosity c) {
    return switch (c.targetType) {
      case "TOPIC" -> c.topic;
      case "TEAM" -> { Team t = Team.findById(Long.valueOf(c.targetId)); yield t == null ? "?" : t.name; }
      case "MEMBER" -> { Member m = Member.findById(Long.valueOf(c.targetId)); yield m == null ? "?" : m.name; }
      default -> c.targetId;
    };
  }
  /** Deletes a fact and its cards in existing games; games left with no open card are finished. */
  static void removeFact(Curiosity c) {
    if (c == null) return;
    for (GameCard gc : GameCard.<GameCard>list("curiosity.id", c.id)) {
      GameInstance g = gc.game; g.cards.remove(gc); gc.delete();
      if (g.status.equals("OPEN") && g.cards.stream().allMatch(x -> x.correct != null)) { g.status = "FINISHED"; g.finishedAt = java.time.LocalDateTime.now(); }
    }
    c.delete();
  }

  static void curiosity(String text, String type, String id) { Curiosity c = new Curiosity(); c.text = text; c.targetType = type; c.targetId = id; c.persist(); }
}
