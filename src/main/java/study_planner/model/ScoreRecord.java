package study_planner.model;

import jakarta.persistence.*;

/** A test / quiz score (0-100) logged by the student for a subject. */
@Entity
@Table(name = "score_records")
public class ScoreRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long subjectId;
    private int score;
    private String date;

    public ScoreRecord() {
    }

    public ScoreRecord(Long userId, Long subjectId, int score, String date) {
        this.userId = userId;
        this.subjectId = subjectId;
        this.score = score;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public int getScore() {
        return score;
    }

    public String getDate() {
        return date;
    }
}
