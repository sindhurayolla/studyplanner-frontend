package study_planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study_planner.model.ScoreRecord;

import java.util.List;

public interface ScoreRecordRepository extends JpaRepository<ScoreRecord, Long> {

    List<ScoreRecord> findByUserIdOrderByIdDesc(Long userId);
}
