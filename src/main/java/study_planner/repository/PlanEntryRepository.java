package study_planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study_planner.model.PlanEntry;

import java.util.List;

public interface PlanEntryRepository extends JpaRepository<PlanEntry, Long> {

    List<PlanEntry> findByUserIdOrderByDateAscStartTimeAsc(Long userId);

    List<PlanEntry> findByUserIdAndDateGreaterThanEqualAndCompletedFalse(Long userId, String date);
}
