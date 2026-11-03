package ctw.gardeners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity public class GameCard extends PanacheEntity {
  @ManyToOne @JsonIgnore public GameInstance game;
  @ManyToOne public Curiosity curiosity;
  public Boolean correct;                 // null = not answered yet
  public boolean helped;
}
