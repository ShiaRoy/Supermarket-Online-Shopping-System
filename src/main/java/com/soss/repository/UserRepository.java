package com.soss.repository;
import com.soss.model.User;
import com.soss.model.Role;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User,Long> {
 Optional<User> findByEmail(String email);
 boolean existsByEmail(String email);
 long countByRole(Role role);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select u from User u order by u.id")
 List<User> lockAllForRoleUpdate();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select u from User u where u.email = :email")
 Optional<User> lockByEmail(@Param("email") String email);
}
