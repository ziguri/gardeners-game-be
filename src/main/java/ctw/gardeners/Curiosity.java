package ctw.gardeners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.Locale;
import java.util.Objects;

/** A fact. scope: COMPANY (levels 1-10) | UNIT (11-20) | PEOPLE (21-30). COMPANY/UNIT facts are dropped on a TOPIC;
 *  PEOPLE facts on a LOCATION, TEAM or MEMBER (targetType/targetId). */
@Entity public class Curiosity extends PanacheEntity {
  @Column(length = 1000) public String text;
  @Column(length = 1000) public String question;
  @Column(length = 1000) public String answerA;
  @Column(length = 1000) public String answerB;
  @Column(length = 1000) public String answerC;
  public Integer correctAnswer;
  @Column(length = 2000) public String explanation;
  public String targetType, targetId, scope = "PEOPLE", topic;
  @ManyToOne @JsonIgnore public Company company;

  public boolean isQuiz() {
    return question != null || answerA != null || answerB != null || answerC != null || correctAnswer != null;
  }

  public boolean isCorrectAnswer(String answerId) {
    if (answerId == null || correctAnswer == null) return false;
    return switch (answerId.trim().toUpperCase(Locale.ROOT)) {
      case "A" -> Objects.equals(correctAnswer, 1);
      case "B" -> Objects.equals(correctAnswer, 2);
      case "C" -> Objects.equals(correctAnswer, 3);
      default -> Objects.equals(String.valueOf(correctAnswer), answerId.trim());
    };
  }

  public String correctKey() {
    if (correctAnswer == null) return null;
    return switch (correctAnswer) {
      case 1 -> "A";
      case 2 -> "B";
      case 3 -> "C";
      default -> String.valueOf(correctAnswer);
    };
  }
}
