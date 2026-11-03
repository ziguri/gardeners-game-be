package ctw.gardeners;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class Seed {
  @Transactional void onStart(@Observes StartupEvent e) {
    if (Company.count() == 0) { Company co = new Company(); co.name = "CTW"; co.description = ""; co.persist(); }
    if (Member.count() > 0) return;
    Member.of("Administrator", "admin", "admin1234", "admin", "Game admin").persist();   // change on first login
    Member margarida = Member.of("Margarida Oliveira", "margarida", "changeme1", "member", "Head of Interaction");
    Member joao = Member.of("João Dias", "joao", "changeme1", "member", "Technical Titan");
    margarida.persist(); joao.persist();
    AdminResource.curiosity("I lead the Gardeners' interaction work as Head of Interaction.", "MEMBER", margarida.id.toString());
    AdminResource.curiosity("I'm the Technical Titan of the Gardeners.", "MEMBER", joao.id.toString());
  }
}
