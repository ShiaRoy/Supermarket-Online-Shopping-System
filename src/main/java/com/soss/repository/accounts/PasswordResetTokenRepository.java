package com.soss.repository.accounts;
import com.soss.model.accounts.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
// provides database access to hashed reset token, expiry details, and request history. 
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findByTokenHash(String hash);
    Optional<PasswordResetToken> findFirstByOwnerIdOrderByCreatedAtDesc(Long id);
    long countByOwnerIdAndCreatedAtAfter(Long id, Instant since);
    void deleteByOwnerId(Long id);
}
