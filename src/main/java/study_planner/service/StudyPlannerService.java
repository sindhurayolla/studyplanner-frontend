package study_planner.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import study_planner.model.StudyTask;
import study_planner.model.Subject;
import study_planner.model.User;
import study_planner.repository.StudyTaskRepository;
import study_planner.repository.SubjectRepository;
import study_planner.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StudyPlannerService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final StudyTaskRepository studyTaskRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public StudyPlannerService(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            StudyTaskRepository studyTaskRepository) {

        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.studyTaskRepository = studyTaskRepository;
    }

    /** Returns empty if the email is already taken. */
    public Optional<User> registerUser(User user) {
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        if (email.isEmpty() || user.getPassword() == null || user.getPassword().isBlank()
                || userRepository.findByEmail(email).isPresent()) {
            return Optional.empty();
        }
        user.setEmail(email);
        user.setPassword(encoder.encode(user.getPassword()));
        return Optional.of(userRepository.save(user));
    }

    public Optional<User> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> encoder.matches(password, u.getPassword()));
    }

    public Subject addSubject(Subject subject) {
        subject.setFeedbackWeight(1.0);
        return subjectRepository.save(subject);
    }

    public Optional<Subject> updateSubject(Long id, Long userId, Subject changes) {
        return subjectRepository.findById(id)
                .filter(s -> userId.equals(s.getUserId()))
                .map(s -> {
                    s.setName(changes.getName());
                    s.setDescription(changes.getDescription());
                    s.setDifficulty(changes.getDifficulty());
                    s.setExamDate(changes.getExamDate());
                    return subjectRepository.save(s);
                });
    }

    public Optional<User> updateProfile(Long userId, String goal, Double dailyHours,
                                        String preferredSlot, String learningStyle) {
        return userRepository.findById(userId).map(u -> {
            u.setGoal(goal);
            if (dailyHours != null) {
                u.setDailyHours(Math.max(0.5, Math.min(12, dailyHours)));
            }
            if (preferredSlot != null) {
                u.setPreferredSlot(preferredSlot);
            }
            u.setLearningStyle(learningStyle);
            return userRepository.save(u);
        });
    }

    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }

    public List<Subject> getSubjects(Long userId) {
        return subjectRepository.findByUserId(userId);
    }

    public void deleteSubject(Long id, Long userId) {
        subjectRepository.findById(id)
                .filter(s -> userId.equals(s.getUserId()))
                .ifPresent(subjectRepository::delete);
    }

    public StudyTask addTask(StudyTask task) {
        return studyTaskRepository.save(task);
    }

    public List<StudyTask> getTasks(Long userId) {
        return studyTaskRepository.findByUserId(userId);
    }

    public Optional<StudyTask> toggleTask(Long id, Long userId) {
        return studyTaskRepository.findById(id)
                .filter(t -> userId.equals(t.getUserId()))
                .map(t -> {
                    t.setCompleted(!t.isCompleted());
                    return studyTaskRepository.save(t);
                });
    }

    public void deleteTask(Long id, Long userId) {
        studyTaskRepository.findById(id)
                .filter(t -> userId.equals(t.getUserId()))
                .ifPresent(studyTaskRepository::delete);
    }
}
