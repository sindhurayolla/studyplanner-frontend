package study_planner.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Long userId;
    private Integer difficulty;
    private String examDate;
    private Double feedbackWeight;

    public Subject() {
    }

    public Subject(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getDifficulty() {
        return difficulty == null ? 3 : difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty == null ? null : Math.max(1, Math.min(5, difficulty));
    }

    public String getExamDate() {
        return examDate;
    }

    public void setExamDate(String examDate) {
        this.examDate = (examDate == null || examDate.isBlank()) ? null : examDate;
    }

    public double getFeedbackWeight() {
        return feedbackWeight == null ? 1.0 : feedbackWeight;
    }

    public void setFeedbackWeight(Double feedbackWeight) {
        this.feedbackWeight = feedbackWeight;
    }
}
