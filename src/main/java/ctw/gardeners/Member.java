package ctw.gardeners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity @UserDefinition public class Member extends PanacheEntity {
  public String name, jobTitle;           // jobTitle e.g. "Head of Interaction" (leadership has no team)
  @Username @Column(unique = true) public String username;
  @Password(PasswordType.MCF) @JsonIgnore public String password;
  @Roles public String role = "member";   // "admin" | "member"
  @ManyToOne public Team team;
  public static Member of(String n, String u, String pw, String role, String title) {
    Member m = new Member(); m.name = n; m.username = u; m.password = BcryptUtil.bcryptHash(pw); m.role = role; m.jobTitle = title; return m;
  }
}
