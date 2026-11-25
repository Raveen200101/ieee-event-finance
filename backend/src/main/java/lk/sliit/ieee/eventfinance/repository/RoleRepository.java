package lk.sliit.ieee.eventfinance.repository;
import java.util.Optional;
import lk.sliit.ieee.eventfinance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoleRepository extends JpaRepository<Role, Long> { Optional<Role> findByName(ChapterRole name); }
