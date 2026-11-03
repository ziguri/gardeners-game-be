package ctw.gardeners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/** Unit "Gardeners" teams. Locations are Lisboa / Porto. */
@Entity public class Team extends PanacheEntity {
  public String name, location, productType;
  @Column(length = 2000) public String description;
}
