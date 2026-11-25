package lk.sliit.ieee.eventfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "budget_categories")
public class BudgetCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(nullable = false, length = 50)
    private String categoryKey;
    @Column(nullable = false, length = 150)
    private String owner;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedAmount;

    protected BudgetCategory() {}
    public BudgetCategory(Event event, String category, String owner, BigDecimal plannedAmount) {
        this.event = event;
        this.category = category.strip();
        this.categoryKey = key(category);
        update(owner, plannedAmount);
    }
    public static String key(String category) { return category.strip().toLowerCase(Locale.ROOT); }
    public void update(String owner, BigDecimal plannedAmount) {
        this.owner = owner.strip();
        this.plannedAmount = plannedAmount;
    }
    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getCategoryKey() { return categoryKey; }
    public String getOwner() { return owner; }
    public BigDecimal getPlannedAmount() { return plannedAmount; }
}
