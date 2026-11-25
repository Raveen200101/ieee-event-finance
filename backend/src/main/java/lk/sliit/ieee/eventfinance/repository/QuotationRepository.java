package lk.sliit.ieee.eventfinance.repository;

import java.util.*;
import lk.sliit.ieee.eventfinance.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByPurchaseGroupIdOrderByIdAsc(Long groupId);
    List<Quotation> findByPurchaseGroupEventIdOrderByIdAsc(Long eventId);
    Optional<Quotation> findByIdAndPurchaseGroupId(Long id, Long groupId);
}
