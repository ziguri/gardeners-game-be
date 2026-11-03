package ctw.gardeners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity public class GameInstance extends PanacheEntity {
		@ManyToOne public Member player;
		public int level = 1, score, helpsLeft = 3;
		public String status = "OPEN";          // OPEN | FINISHED
		public LocalDateTime createdAt = LocalDateTime.now(), finishedAt;
		@OneToMany(cascade = CascadeType.ALL, mappedBy = "game") @OrderBy("id") @JsonIgnore public List<GameCard> cards = new ArrayList<>();
}
