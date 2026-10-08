package study_planner;

import org.junit.jupiter.api.Test;
import study_planner.ai.RecommendationEngine;
import study_planner.ai.RecommendationEngine.Scored;
import study_planner.ai.RecommendationEngine.SubjectInfo;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {

	private final RecommendationEngine engine = new RecommendationEngine();
	private final LocalDate today = LocalDate.now();

	@Test
	void weakSubjectWithNearExamRanksFirst() {
		SubjectInfo weak = new SubjectInfo(1L, "Maths", 4, today.plusDays(5).toString(), 1.0, 2, List.of(40, 50));
		SubjectInfo strong = new SubjectInfo(2L, "English", 2, null, 1.0, 0, List.of(90, 85));
		List<Scored> ranked = engine.rank(List.of(strong, weak), today);
		assertEquals("Maths", ranked.get(0).subject().name());
	}

	@Test
	void pastExamsAreSkippedAndTimeIsAllocatedWithinBudget() {
		SubjectInfo past = new SubjectInfo(1L, "Old", 3, today.minusDays(1).toString(), 1.0, 0, List.of());
		SubjectInfo a = new SubjectInfo(2L, "A", 3, null, 1.0, 0, List.of(50));
		SubjectInfo b = new SubjectInfo(3L, "B", 3, null, 1.0, 0, List.of(80));
		List<Scored> ranked = engine.rank(List.of(past, a, b), today);
		assertEquals(2, ranked.size());
		int total = engine.allocate(ranked, 120).stream().mapToInt(x -> x.minutes()).sum();
		assertTrue(Math.abs(total - 120) <= 10);
	}

	@Test
	void feedbackAdjustsWeightWithinBounds() {
		assertEquals(1.1, engine.adjustWeight(1.0, "TOO_HARD"), 1e-9);
		assertEquals(1.5, engine.adjustWeight(1.5, "TOO_HARD"), 1e-9);
		assertEquals(0.9, engine.adjustWeight(1.0, "TOO_EASY"), 1e-9);
		assertEquals("declining", engine.trend(List.of(40, 60, 70)));
	}
}
