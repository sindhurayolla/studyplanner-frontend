package study_planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study_planner.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}