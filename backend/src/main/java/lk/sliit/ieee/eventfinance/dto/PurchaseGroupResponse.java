package lk.sliit.ieee.eventfinance.dto;
import java.math.BigDecimal;
import java.util.List;
public record PurchaseGroupResponse(Long id, String name, String notes, Long budgetCategoryId,
        String category, BigDecimal categoryBudget, BigDecimal categorySelectedTotal,
        BigDecimal categoryRemaining, Long ownerId, List<Quote> quotations) {
    public record Quote(Long id, String supplier, Integer quantity, BigDecimal unitPrice,
            BigDecimal total, String notes, boolean selected) {}
}
