package study_planner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study_planner.ai.RecommendationEngine;
import study_planner.ai.RecommendationEngine.Allocation;
import study_planner.ai.RecommendationEngine.Scored;
import study_planner.ai.RecommendationEngine.SubjectInfo;
import study_planner.model.*;
import study_planner.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/** Builds personalised plans, records feedback and produces insights and reminders. */
@Service
public class PlanService {

    public record SubjectInsight(Long subjectId, String name, Double avgScore, String trend,
                                 double priority, boolean weak, Integer daysToExam,
                                 int pendingTasks, int studiedMinutes, String advice) {
    }

    public record Insights(List<SubjectInsight> subjects, int weekPlanned, int weekCompleted,
                           int minutesStudied, int streak) {
    }

    public record Reminder(String severity, String message) {
    }

    private final UserRepository users;
    private final SubjectRepository subjects;
    private final StudyTaskRepository tasks;
    private final ScoreRecordRepository scores;
    private final PlanEntryRepository plan;
    private final RecommendationEngine engine;

    public PlanService(UserRepository users, SubjectRepository subjects, StudyTaskRepository tasks,
                       ScoreRecordRepository scores, PlanEntryRepository plan, RecommendationEngine engine) {
        this.users = users;
        this.subjects = subjects;
        this.tasks = tasks;
        this.scores = scores;
        this.plan = plan;
        this.engine = engine;
    }

    // ---------- data preprocessing ----------

    private List<SubjectInfo> subjectInfos(Long userId) {
        Map<Long, List<Integer>> scoresBySubject = new HashMap<>();
        for (ScoreRecord r : scores.findByUserIdOrderByIdDesc(userId)) {
            scoresBySubject.computeIfAbsent(r.getSubjectId(), k -> new ArrayList<>()).add(r.getScore());
        }
        Map<Long, Long> pending = tasks.findByUserId(userId).stream()
                .filter(t -> !t.isCompleted() && t.getSubjectId() != null)
                .collect(Collectors.groupingBy(StudyTask::getSubjectId, Collectors.counting()));

        List<SubjectInfo> out = new ArrayList<>();
        for (Subject s : subjects.findByUserId(userId)) {
            List<Integer> recent = scoresBySubject.getOrDefault(s.getId(), List.of());
            recent = recent.subList(0, Math.min(5, recent.size()));
            out.add(new SubjectInfo(s.getId(), s.getName(), s.getDifficulty(), s.getExamDate(),
                    s.getFeedbackWeight(), pending.getOrDefault(s.getId(), 0L).intValue(), recent));
        }
        return out;
    }

    private int slotStartMinutes(String slot) {
        return switch (slot) {
            case "Morning" -> 7 * 60;
            case "Afternoon" -> 13 * 60;
            case "Night" -> 20 * 60;
            default -> 17 * 60; // Evening
        };
    }

    private String hhmm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    // ---------- plan generation ----------

    /** Regenerates the plan for day offsets [fromOffset, toOffset) from today. Completed sessions are kept. */
    @Transactional
    public List<PlanEntry> generate(Long userId, int fromOffset, int toOffset) {
        User user = users.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.plusDays(fromOffset);
        plan.deleteAll(plan.findByUserIdAndDateGreaterThanEqualAndCompletedFalse(userId, firstDay.toString()));

        List<SubjectInfo> infos = subjectInfos(userId);
        String tip = engine.studyTip(user.getLearningStyle());
        List<PlanEntry> created = new ArrayList<>();

        for (int offset = fromOffset; offset < toOffset; offset++) {
            LocalDate day = today.plusDays(offset);
            int budget = (int) Math.round(user.getDailyHours() * 60);
            if (offset == 0) {
                budget -= plan.findByUserIdOrderByDateAscStartTimeAsc(userId).stream()
                        .filter(e -> e.isCompleted() && e.getDate().equals(day.toString()))
                        .mapToInt(PlanEntry::getDurationMinutes).sum();
            }
            if (budget < RecommendationEngine.MIN_SESSION_MINUTES) {
                continue;
            }
            List<Allocation> allocations = engine.allocate(engine.rank(infos, day), budget);

            int start = slotStartMinutes(user.getPreferredSlot());
            if (offset == 0) {
                LocalTime now = LocalTime.now();
                int next = (now.getHour() * 60 + now.getMinute() + 5) / 5 * 5;
                start = Math.max(start, next);
            }
            for (Allocation a : allocations) {
                if (start + a.minutes() > 24 * 60) {
                    break;
                }
                Scored sc = a.scored();
                PlanEntry e = new PlanEntry();
                e.setUserId(userId);
                e.setSubjectId(sc.subject().id());
                e.setSubjectName(sc.subject().name());
                e.setDate(day.toString());
                e.setStartTime(hhmm(start));
                e.setDurationMinutes(a.minutes());
                e.setPriorityScore(Math.round(sc.score() * 100) / 100.0);
                e.setReason(sc.reason());
                e.setTip(tip);
                created.add(plan.save(e));
                start += a.minutes() + 10; // 10 minute break
            }
        }
        return created;
    }

    public List<PlanEntry> getPlan(Long userId) {
        String from = LocalDate.now().minusDays(6).toString();
        return plan.findByUserIdOrderByDateAscStartTimeAsc(userId).stream()
                .filter(e -> e.getDate().compareTo(from) >= 0).toList();
    }

    // ---------- progress & feedback loop ----------

    @Transactional
    public Optional<PlanEntry> complete(Long userId, Long entryId, String feedback, Integer actualMinutes) {
        Optional<PlanEntry> found = plan.findById(entryId).filter(e -> userId.equals(e.getUserId()));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        PlanEntry e = found.get();
        e.setCompleted(true);
        e.setFeedback(feedback);
        e.setActualMinutes(actualMinutes);
        plan.save(e);

        subjects.findById(e.getSubjectId()).filter(s -> userId.equals(s.getUserId())).ifPresent(s -> {
            s.setFeedbackWeight(engine.adjustWeight(s.getFeedbackWeight(), feedback));
            subjects.save(s);
        });
        // feedback updates the recommendations: re-plan the coming days
        generate(userId, 1, 7);
        return Optional.of(e);
    }

    @Transactional
    public Optional<PlanEntry> undo(Long userId, Long entryId) {
        return plan.findById(entryId).filter(e -> userId.equals(e.getUserId())).map(e -> {
            e.setCompleted(false);
            e.setFeedback(null);
            e.setActualMinutes(null);
            return plan.save(e);
        });
    }

    /** Logs a test score; returns false if the subject isn't the user's or the score is invalid. */
    @Transactional
    public boolean logScore(Long userId, Long subjectId, Integer score) {
        boolean owned = subjects.findById(subjectId).filter(s -> userId.equals(s.getUserId())).isPresent();
        if (!owned || score == null || score < 0 || score > 100) {
            return false;
        }
        scores.save(new ScoreRecord(userId, subjectId, score, LocalDate.now().toString()));
        generate(userId, 0, 7); // new performance data -> refresh the plan
        return true;
    }

    // ---------- demo data (for presentations) ----------

    private Subject demoSubject(Long userId, String name, String desc, int difficulty, Integer examInDays) {
        Subject s = new Subject(name, desc);
        s.setUserId(userId);
        s.setDifficulty(difficulty);
        s.setExamDate(examInDays == null ? null : LocalDate.now().plusDays(examInDays).toString());
        s.setFeedbackWeight(1.0);
        return subjects.save(s);
    }

    private void demoScores(Long userId, Subject s, int... oldestToNewest) {
        for (int score : oldestToNewest) {
            scores.save(new ScoreRecord(userId, s.getId(), score, LocalDate.now().toString()));
        }
    }

    private void demoTask(Long userId, Subject s, String title, int dueInDays, String priority) {
        StudyTask t = new StudyTask(title, "", LocalDate.now().plusDays(dueInDays).toString(), priority);
        t.setUserId(userId);
        t.setSubjectId(s.getId());
        tasks.save(t);
    }

    /** Fills an empty account with realistic sample data and builds a plan. False if the user already has subjects. */
    @Transactional
    public boolean seedDemo(Long userId) {
        Optional<User> found = users.findById(userId);
        if (found.isEmpty() || !subjects.findByUserId(userId).isEmpty()) {
            return false;
        }
        User u = found.get();
        u.setGoal("Score 85% in semester exams");
        u.setDailyHours(3.0);
        u.setPreferredSlot("Evening");
        u.setLearningStyle("Visual");
        users.save(u);

        Subject maths = demoSubject(userId, "Mathematics", "Calculus and linear algebra", 5, 6);
        Subject physics = demoSubject(userId, "Physics", "Mechanics and optics", 4, 12);
        Subject ds = demoSubject(userId, "Data Structures", "Trees, graphs and hashing", 4, 20);
        Subject english = demoSubject(userId, "English", "Technical writing", 2, null);

        demoScores(userId, maths, 38, 45);
        demoScores(userId, physics, 62, 55, 48);
        demoScores(userId, ds, 70, 78);
        demoScores(userId, english, 88, 92);

        demoTask(userId, maths, "Finish integration worksheet", -1, "High");
        demoTask(userId, maths, "Solve previous year paper", 2, "High");
        demoTask(userId, physics, "Revise optics formulas", 3, "Medium");
        demoTask(userId, ds, "Implement BST in Java", 5, "Medium");
        demoTask(userId, english, "Write lab report", 7, "Low");

        generate(userId, 0, 7);
        return true;
    }

    // ---------- insights ----------

    public Insights insights(Long userId) {
        LocalDate today = LocalDate.now();
        List<PlanEntry> all = plan.findByUserIdOrderByDateAscStartTimeAsc(userId);
        Map<Long, Integer> studied = new HashMap<>();
        for (PlanEntry e : all) {
            if (e.isCompleted()) {
                int m = e.getActualMinutes() != null ? e.getActualMinutes() : e.getDurationMinutes();
                studied.merge(e.getSubjectId(), m, Integer::sum);
            }
        }

        List<SubjectInsight> list = new ArrayList<>();
        for (SubjectInfo s : subjectInfos(userId)) {
            Scored sc = engine.score(s, today);
            Double avg = engine.weightedAverage(s.scoresNewestFirst());
            String trend = engine.trend(s.scoresNewestFirst());
            boolean weak = avg != null && avg < 60;
            list.add(new SubjectInsight(s.id(), s.name(), avg == null ? null : Math.round(avg * 10) / 10.0,
                    trend, Math.round(sc.score() * 100) / 100.0, weak, sc.daysToExam(),
                    s.pendingTasks(), studied.getOrDefault(s.id(), 0), advice(avg, trend, sc.daysToExam())));
        }
        list.sort(Comparator.comparingDouble(SubjectInsight::priority).reversed());

        String weekStart = today.minusDays(6).toString();
        List<PlanEntry> week = all.stream()
                .filter(e -> e.getDate().compareTo(weekStart) >= 0 && e.getDate().compareTo(today.toString()) <= 0)
                .toList();
        int minutes = all.stream().filter(PlanEntry::isCompleted)
                .mapToInt(e -> e.getActualMinutes() != null ? e.getActualMinutes() : e.getDurationMinutes()).sum();

        Set<String> days = all.stream().filter(PlanEntry::isCompleted).map(PlanEntry::getDate).collect(Collectors.toSet());
        int streak = 0;
        LocalDate d = days.contains(today.toString()) ? today : today.minusDays(1);
        while (days.contains(d.toString())) {
            streak++;
            d = d.minusDays(1);
        }
        return new Insights(list, week.size(), (int) week.stream().filter(PlanEntry::isCompleted).count(), minutes, streak);
    }

    private String advice(Double avg, String trend, Integer daysToExam) {
        boolean weak = avg != null && avg < 60;
        if (weak && daysToExam != null && daysToExam <= 7) {
            return "Exam soon and scores are low: revise daily and practise past questions.";
        }
        if (weak) {
            return "Scores are below 60%: revisit the basics, then log a new test score.";
        }
        if (trend.equals("declining")) {
            return "Your latest score dropped: review recent mistakes.";
        }
        if (avg == null) {
            return "Log a test score so the planner can measure this subject.";
        }
        return "On track: keep your routine going.";
    }

    // ---------- reminders ----------

    public List<Reminder> reminders(Long userId) {
        LocalDate today = LocalDate.now();
        String t = today.toString();
        String tomorrow = today.plusDays(1).toString();
        List<Reminder> out = new ArrayList<>();

        List<Subject> subs = subjects.findByUserId(userId);
        List<PlanEntry> all = plan.findByUserIdOrderByDateAscStartTimeAsc(userId);
        boolean planToday = all.stream().anyMatch(e -> e.getDate().equals(t));
        if (!subs.isEmpty() && !planToday) {
            out.add(new Reminder("info", "No plan for today yet. Generate your study plan."));
        }
        for (PlanEntry e : all) {
            if (e.getDate().equals(t) && !e.isCompleted()) {
                out.add(new Reminder("info", "Today " + e.getStartTime() + ": " + e.getSubjectName()
                        + " (" + e.getDurationMinutes() + " min)"));
            }
        }
        for (StudyTask task : tasks.findByUserId(userId)) {
            if (task.isCompleted() || task.getDate() == null || task.getDate().isBlank()) {
                continue;
            }
            if (task.getDate().compareTo(t) < 0) {
                out.add(new Reminder("danger", "Overdue task: " + task.getTitle() + " (due " + task.getDate() + ")"));
            } else if (task.getDate().equals(t) || task.getDate().equals(tomorrow)) {
                out.add(new Reminder("warning", "Task due " + (task.getDate().equals(t) ? "today" : "tomorrow")
                        + ": " + task.getTitle()));
            }
        }
        for (Subject s : subs) {
            Integer days = engine.daysToExam(s.getExamDate(), today);
            if (days != null && days >= 0 && days <= 7) {
                out.add(new Reminder(days <= 3 ? "danger" : "warning", s.getName() + " exam "
                        + (days == 0 ? "is today" : "in " + days + " day" + (days == 1 ? "" : "s"))));
            }
        }
        return out;
    }
}
