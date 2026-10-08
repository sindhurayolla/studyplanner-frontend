package study_planner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Academic profile & preferences (used by the recommendation engine)
    private String goal;
    private Double dailyHours;
    private String preferredSlot;
    private String learningStyle;

    public User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public double getDailyHours() {
        return dailyHours == null ? 2.0 : dailyHours;
    }

    public void setDailyHours(Double dailyHours) {
        this.dailyHours = dailyHours;
    }

    public String getPreferredSlot() {
        return preferredSlot == null ? "Evening" : preferredSlot;
    }

    public void setPreferredSlot(String preferredSlot) {
        this.preferredSlot = preferredSlot;
    }

    public String getLearningStyle() {
        return learningStyle == null ? "" : learningStyle;
    }

    public void setLearningStyle(String learningStyle) {
        this.learningStyle = learningStyle;
    }
}
