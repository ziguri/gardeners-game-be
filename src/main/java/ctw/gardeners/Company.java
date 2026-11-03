package ctw.gardeners;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity public class Company extends PanacheEntity {
  public String name;
  @Column(length = 2000) public String description;
}
