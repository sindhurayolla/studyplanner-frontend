package study_planner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import study_planner.model.StudyTask;
import study_planner.model.Subject;
import study_planner.model.User;
import study_planner.service.StudyPlannerService;

import java.util.List;
import java.util.Map;

/**
 * REST API consumed by the React frontend (proxied from Vite under /api).
 * The client sends its user id in the X-User-Id header after login.
 */
@RestController
@RequestMapping("/api")
public class StudyPlannerController {

    private final StudyPlannerService service;

    public StudyPlannerController(StudyPlannerService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        return service.registerUser(user)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already registered or details missing")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return service.login(body.get("email"), body.get("password"))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid email or password")));
    }

    @GetMapping("/subjects")
    public List<Subject> subjects(@RequestHeader("X-User-Id") Long userId) {
        return service.getSubjects(userId);
    }

    @PostMapping("/subjects")
    public Subject addSubject(@RequestHeader("X-User-Id") Long userId, @RequestBody Subject subject) {
        subject.setId(null);
        subject.setUserId(userId);
        return service.addSubject(subject);
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<Subject> updateSubject(@RequestHeader("X-User-Id") Long userId,
                                                 @PathVariable Long id, @RequestBody Subject subject) {
        return service.updateSubject(id, userId, subject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ProfileRequest(String goal, Double dailyHours, String preferredSlot, String learningStyle) {
    }

    @GetMapping("/profile")
    public ResponseEntity<User> profile(@RequestHeader("X-User-Id") Long userId) {
        return service.getUser(userId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                              @RequestBody ProfileRequest req) {
        return service.updateProfile(userId, req.goal(), req.dailyHours(), req.preferredSlot(), req.learningStyle())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        service.deleteSubject(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks")
    public List<StudyTask> tasks(@RequestHeader("X-User-Id") Long userId) {
        return service.getTasks(userId);
    }

    @PostMapping("/tasks")
    public StudyTask addTask(@RequestHeader("X-User-Id") Long userId, @RequestBody StudyTask task) {
        task.setId(null);
        task.setUserId(userId);
        return service.addTask(task);
    }

    @PutMapping("/tasks/{id}/toggle")
    public ResponseEntity<StudyTask> toggle(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return service.toggleTask(id, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        service.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }
}
