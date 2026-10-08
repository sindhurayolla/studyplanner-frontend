package study_planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study_planner.model.StudyTask;

import java.util.List;

public interface StudyTaskRepository extends JpaRepository<StudyTask, Long> {

    List<StudyTask> findByUserId(Long userId);
}
