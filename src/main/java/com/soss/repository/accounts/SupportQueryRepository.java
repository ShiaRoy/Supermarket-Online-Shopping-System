package com.soss.repository.accounts;
import com.soss.model.accounts.SupportQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
// loads customer support histories and paginated staff query lists.
public interface SupportQueryRepository extends JpaRepository<SupportQuery,Long> {
    Page<SupportQuery> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    Page<SupportQuery> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
