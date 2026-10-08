package study_planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study_planner.model.Subject;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByUserId(Long userId);
}
