package ctw.gardeners;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("/api/me") @RolesAllowed({"member", "admin"}) @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class MemberResource {
  @Inject GameService svc; @Context SecurityContext sec;
  public record Password(String newPassword) {}
  public record Drop(Long cardId, String type, String id) {}
  public record Help(Long cardId) {}
  public record Start(Integer level) {}

  Member me() { return Member.find("username", sec.getUserPrincipal().getName()).firstResult(); }
  GameInstance mine(Long id) { GameInstance g = GameInstance.findById(id); if (g == null || !g.player.id.equals(me().id)) throw new ForbiddenException(); return g; }

  @GET public Member profile() { return me(); }
  @POST @Path("password") @Transactional public void password(Password p) {
    if (p.newPassword() == null || p.newPassword().length() < 8) throw new BadRequestException("Minimum 8 characters");
    Member m = Member.findById(me().id); m.password = BcryptUtil.bcryptHash(p.newPassword());
  }
  @POST @Path("games") public GameService.State start(Start s) {
    Member m = me(); GameService.Levels lv = svc.levels(m);
    int level = s == null || s.level() == null ? lv.current() : s.level();
    if (level < 1 || level > 30 || !lv.levels().get(level - 1).unlocked()) throw new ForbiddenException("Level locked");
    return svc.state(svc.create(m, level));
  }
  @GET @Path("levels") public GameService.Levels levels() { return svc.levels(me()); }
  @GET @Path("games/{id}") public GameService.State game(@PathParam("id") Long id) { return svc.state(mine(id)); }
  @POST @Path("games/{id}/drop") public GameService.Answer drop(@PathParam("id") Long id, Drop d) { return svc.drop(mine(id), d.cardId(), d.type(), d.id()); }
  @POST @Path("games/{id}/help") public List<GameService.Target> help(@PathParam("id") Long id, Help h) { return svc.help(mine(id), h.cardId()); }
  @GET @Path("stats") public List<GameService.Row> stats() { return svc.scores(null, me().id); }
}
