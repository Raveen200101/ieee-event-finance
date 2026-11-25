package lk.sliit.ieee.eventfinance.repository;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.LockModeType;
import lk.sliit.ieee.eventfinance.entity.RefreshToken;
import org.springframework.data.jpa.repository.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Query("select t.user.id from RefreshToken t where t.tokenHash = :hash") Optional<Long> ownerOf(String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<RefreshToken> findByTokenHash(String hash);
    @Modifying @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :family and t.revokedAt is null")
    int revokeFamily(String family, Instant now);
    @Modifying @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeUser(Long userId, Instant now);
    void deleteByUserId(Long id);
}
