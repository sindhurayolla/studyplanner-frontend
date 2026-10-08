package study_planner.ai;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The "AI layer": performance analysis, priority scoring and time allocation.
 *
 * It is a transparent, explainable model (weighted scoring with recency-weighted
 * performance analysis and a feedback-adjusted multiplier) rather than a black box,
 * so every recommendation can be explained to the student.
 *
 * priority = (0.25*difficulty + 0.35*weakness + 0.25*examUrgency + 0.15*pendingWork)
 *            * feedbackWeight
 */
@Component
public class RecommendationEngine {

    public static final int MIN_SESSION_MINUTES = 20;
    private static final double W_DIFFICULTY = 0.25;
    private static final double W_WEAKNESS = 0.35;
    private static final double W_URGENCY = 0.25;
    private static final double W_PENDING = 0.15;

    /** Everything the engine needs to know about one subject (no database types). */
    public record SubjectInfo(Long id, String name, int difficulty, String examDate,
                              double feedbackWeight, int pendingTasks,
                              List<Integer> scoresNewestFirst) {
    }

    public record Scored(SubjectInfo subject, double score, double weakness, double urgency,
                         Integer daysToExam, Double avgScore, String reason) {
    }

    public record Allocation(Scored scored, int minutes) {
    }

    /** Recency-weighted average (newest counts most). Null if there is no data. */
    public Double weightedAverage(List<Integer> newestFirst) {
        if (newestFirst == null || newestFirst.isEmpty()) {
            return null;
        }
        double sum = 0;
        double weights = 0;
        double w = 1;
        for (int s : newestFirst) {
            sum += s * w;
            weights += w;
            w *= 0.6;
        }
        return sum / weights;
    }

    /** "improving", "declining", "steady" or "no data". */
    public String trend(List<Integer> newestFirst) {
        if (newestFirst == null || newestFirst.size() < 2) {
            return "no data";
        }
        double rest = newestFirst.subList(1, newestFirst.size()).stream()
                .mapToInt(Integer::intValue).average().orElse(newestFirst.get(0));
        double diff = newestFirst.get(0) - rest;
        if (diff >= 5) {
            return "improving";
        }
        if (diff <= -5) {
            return "declining";
        }
        return "steady";
    }

    /** 0 (strong) .. 1 (weak). Unknown performance is treated as neutral (0.5). */
    public double weakness(List<Integer> newestFirst) {
        Double avg = weightedAverage(newestFirst);
        if (avg == null) {
            return 0.5;
        }
        double weakness = 1 - avg / 100.0;
        if (trend(newestFirst).equals("declining")) {
            weakness += 0.1;
        }
        return Math.max(0, Math.min(1, weakness));
    }

    public Integer daysToExam(String examDate, LocalDate day) {
        if (examDate == null || examDate.isBlank()) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(day, LocalDate.parse(examDate));
    }

    /** 1 on exam day, decaying with distance; subjects without an exam get a low default. */
    public double urgency(Integer daysToExam) {
        if (daysToExam == null) {
            return 0.2;
        }
        return Math.max(0.1, Math.exp(-daysToExam / 14.0));
    }

    public Scored score(SubjectInfo s, LocalDate day) {
        Integer days = daysToExam(s.examDate(), day);
        double weakness = weakness(s.scoresNewestFirst());
        double urgency = urgency(days);
        double pending = Math.min(1.0, s.pendingTasks() / 5.0);
        double base = W_DIFFICULTY * (s.difficulty() / 5.0)
                + W_WEAKNESS * weakness
                + W_URGENCY * urgency
                + W_PENDING * pending;
        double score = Math.max(0.05, base * s.feedbackWeight());
        Double avg = weightedAverage(s.scoresNewestFirst());
        return new Scored(s, score, weakness, urgency, days, avg, reason(s, avg, days));
    }

    private String reason(SubjectInfo s, Double avg, Integer days) {
        List<String> parts = new ArrayList<>();
        if (days != null && days <= 14) {
            parts.add(days == 0 ? "exam today" : "exam in " + days + " day" + (days == 1 ? "" : "s"));
        }
        if (avg != null && avg < 60) {
            parts.add("weak area (avg " + Math.round(avg) + "%)");
        }
        if (s.difficulty() >= 4) {
            parts.add("difficult subject");
        }
        if (s.pendingTasks() > 0) {
            parts.add(s.pendingTasks() + " pending task" + (s.pendingTasks() == 1 ? "" : "s"));
        }
        if (s.feedbackWeight() > 1.05) {
            parts.add("you found it hard recently");
        } else if (s.feedbackWeight() < 0.95) {
            parts.add("you found it easy recently");
        }
        return parts.isEmpty() ? "balanced revision" : String.join(", ", parts);
    }

    /** Ranks subjects for a day, highest priority first. Subjects whose exam has passed are skipped. */
    public List<Scored> rank(List<SubjectInfo> subjects, LocalDate day) {
        List<Scored> out = new ArrayList<>();
        for (SubjectInfo s : subjects) {
            Integer days = daysToExam(s.examDate(), day);
            if (days != null && days < 0) {
                continue;
            }
            out.add(score(s, day));
        }
        out.sort(Comparator.comparingDouble(Scored::score).reversed());
        return out;
    }

    /**
     * Splits a daily time budget between subjects in proportion to priority.
     * Subjects that would get less than a useful session are dropped and their time
     * is redistributed. Minutes are rounded to 5.
     */
    public List<Allocation> allocate(List<Scored> ranked, int budgetMinutes) {
        List<Allocation> result = new ArrayList<>();
        if (ranked.isEmpty() || budgetMinutes < 5) {
            return result;
        }
        List<Scored> kept = new ArrayList<>(ranked);
        while (kept.size() > 1) {
            double sum = kept.stream().mapToDouble(Scored::score).sum();
            Scored last = kept.get(kept.size() - 1);
            if (round5(budgetMinutes * last.score() / sum) < MIN_SESSION_MINUTES) {
                kept.remove(kept.size() - 1);
            } else {
                break;
            }
        }
        double sum = kept.stream().mapToDouble(Scored::score).sum();
        for (Scored s : kept) {
            int minutes = kept.size() == 1 ? round5(budgetMinutes) : round5(budgetMinutes * s.score() / sum);
            result.add(new Allocation(s, Math.max(5, minutes)));
        }
        return result;
    }

    /** New feedback weight after a session: "TOO_HARD", "TOO_EASY" or "JUST_RIGHT". */
    public double adjustWeight(double current, String feedback) {
        if (feedback == null) {
            return current;
        }
        return switch (feedback) {
            case "TOO_HARD" -> Math.min(1.5, current + 0.1);
            case "TOO_EASY" -> Math.max(0.6, current - 0.1);
            case "JUST_RIGHT" -> current + (1.0 - current) * 0.5;
            default -> current;
        };
    }

    public String studyTip(String learningStyle) {
        if (learningStyle == null) {
            return "";
        }
        return switch (learningStyle) {
            case "Visual" -> "Use diagrams, charts and mind maps";
            case "Reading/Writing" -> "Make concise notes and written summaries";
            case "Practice-based" -> "Solve problems and past papers";
            case "Group discussion" -> "Explain the topic to a friend or study group";
            default -> "";
        };
    }

    private int round5(double x) {
        return (int) (Math.round(x / 5.0) * 5);
    }
}
