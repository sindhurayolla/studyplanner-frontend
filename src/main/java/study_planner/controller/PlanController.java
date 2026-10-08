package study_planner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import study_planner.model.PlanEntry;
import study_planner.service.PlanService;

import java.util.List;
import java.util.Map;

/** Personalised plan, progress tracking, insights and reminders. */
@RestController
@RequestMapping("/api")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    public record CompleteRequest(String feedback, Integer actualMinutes) {
    }

    public record ScoreRequest(Integer score) {
    }

    @GetMapping("/plan")
    public List<PlanEntry> plan(@RequestHeader("X-User-Id") Long userId) {
        return planService.getPlan(userId);
    }

    @PostMapping("/plan/generate")
    public List<PlanEntry> generate(@RequestHeader("X-User-Id") Long userId) {
        planService.generate(userId, 0, 7);
        return planService.getPlan(userId);
    }

    @PutMapping("/plan/{id}/complete")
    public ResponseEntity<PlanEntry> complete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                              @RequestBody CompleteRequest req) {
        return planService.complete(userId, id, req.feedback(), req.actualMinutes())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/plan/{id}/undo")
    public ResponseEntity<PlanEntry> undo(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return planService.undo(userId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/subjects/{id}/scores")
    public ResponseEntity<?> logScore(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                      @RequestBody ScoreRequest req) {
        if (planService.logScore(userId, id, req.score())) {
            return ResponseEntity.ok(Map.of("ok", true));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Score must be 0-100 for one of your subjects"));
    }

    @PostMapping("/demo/seed")
    public ResponseEntity<?> seedDemo(@RequestHeader("X-User-Id") Long userId) {
        if (planService.seedDemo(userId)) {
            return ResponseEntity.ok(Map.of("ok", true));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Demo data can only be loaded into an empty account"));
    }

    @GetMapping("/insights")
    public PlanService.Insights insights(@RequestHeader("X-User-Id") Long userId) {
        return planService.insights(userId);
    }

    @GetMapping("/reminders")
    public List<PlanService.Reminder> reminders(@RequestHeader("X-User-Id") Long userId) {
        return planService.reminders(userId);
    }
}
