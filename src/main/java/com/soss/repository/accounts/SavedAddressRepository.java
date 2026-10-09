package com.soss.repository.accounts;
import com.soss.model.accounts.SavedAddress;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
//loads saved queries and address to their owner.
public interface SavedAddressRepository extends JpaRepository<SavedAddress,Long> {
    List<SavedAddress> findByOwnerIdOrderByIdAsc(Long ownerId);
    Optional<SavedAddress> findByIdAndOwnerId(Long id, Long ownerId);
}
