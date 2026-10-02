package com.soss.checkout;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface CartRepository extends JpaRepository<Cart,Long> {
 Optional<Cart> findByOwner(String owner);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Cart c where c.owner = :owner")
 Optional<Cart> lockByOwner(@org.springframework.data.repository.query.Param("owner") String owner);
}

