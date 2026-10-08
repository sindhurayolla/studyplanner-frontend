package study_planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study_planner.model.PlanEntry;
import study_planner.model.Subject;
import study_planner.model.User;
import study_planner.service.PlanService;
import study_planner.service.StudyPlannerService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StudyPlannerApplicationTests {

	@Autowired
	StudyPlannerService service;

	@Autowired
	PlanService planService;

	@Test
	void registerAndLogin() {
		assertTrue(service.registerUser(new User("A", "A@x.com", "pw")).isPresent());
		assertTrue(service.registerUser(new User("B", "a@x.com", "pw")).isEmpty());
		assertTrue(service.login("a@x.com", "pw").isPresent());
		assertTrue(service.login("a@x.com", "bad").isEmpty());
	}

	@Test
	void generatesPersonalisedPlanAndAdaptsToFeedback() {
		User u = service.registerUser(new User("P", "plan@x.com", "pw")).orElseThrow();
		Long uid = u.getId();

		Subject maths = new Subject("Maths", "");
		maths.setUserId(uid);
		maths.setDifficulty(5);
		maths.setExamDate(LocalDate.now().plusDays(4).toString());
		maths = service.addSubject(maths);

		Subject english = new Subject("English", "");
		english.setUserId(uid);
		english.setDifficulty(2);
		english = service.addSubject(english);

		assertTrue(planService.logScore(uid, maths.getId(), 35));
		assertTrue(planService.logScore(uid, english.getId(), 90));
		assertFalse(planService.logScore(uid, maths.getId(), 150));

		List<PlanEntry> plan = planService.getPlan(uid);
		assertFalse(plan.isEmpty());

		// the weak, difficult subject with a near exam gets more time than the strong one
		int mathsMinutes = plan.stream().filter(e -> e.getSubjectName().equals("Maths"))
				.mapToInt(PlanEntry::getDurationMinutes).sum();
		int englishMinutes = plan.stream().filter(e -> e.getSubjectName().equals("English"))
				.mapToInt(PlanEntry::getDurationMinutes).sum();
		assertTrue(mathsMinutes > englishMinutes);

		// completing a session with "TOO_HARD" feedback raises the subject's weight
		PlanEntry first = plan.stream().filter(e -> e.getSubjectName().equals("Maths")).findFirst().orElseThrow();
		assertTrue(planService.complete(uid, first.getId(), "TOO_HARD", null).isPresent());
		assertTrue(service.getSubjects(uid).stream()
				.filter(s -> s.getName().equals("Maths")).findFirst().orElseThrow().getFeedbackWeight() > 1.0);
		assertTrue(planService.insights(uid).minutesStudied() > 0);
	}
}
