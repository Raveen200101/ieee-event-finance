package lk.sliit.ieee.eventfinance.repository;
import java.util.*;
import jakarta.persistence.LockModeType;
import lk.sliit.ieee.eventfinance.entity.ChapterUser;
import org.springframework.data.jpa.repository.*;
public interface ChapterUserRepository extends JpaRepository<ChapterUser, Long> {
    @EntityGraph(attributePaths = "role") Optional<ChapterUser> findByEmail(String email);
    @Override @EntityGraph(attributePaths = "role") Optional<ChapterUser> findById(Long id);
    boolean existsByEmail(String email);
    @EntityGraph(attributePaths = "role") List<ChapterUser> findAllByOrderByIdAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths = "role")
    @Query("select u from ChapterUser u where u.id = :id") Optional<ChapterUser> lockById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from ChapterUser u order by u.id")
    @EntityGraph(attributePaths = "role")
    List<ChapterUser> lockAllForAccessUpdate();
}
