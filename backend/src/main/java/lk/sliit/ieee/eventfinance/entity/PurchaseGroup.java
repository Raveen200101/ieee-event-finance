package lk.sliit.ieee.eventfinance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_groups")
public class PurchaseGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false) private Event event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_category_id", nullable = false) private BudgetCategory budgetCategory;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 1000) private String notes;
    protected PurchaseGroup() {}
    public PurchaseGroup(Event event, BudgetCategory budgetCategory, String name, String notes) {
        this.event = event;
        this.budgetCategory = budgetCategory;
        this.name = name.strip();
        this.notes = notes == null ? "" : notes.strip();
    }
    public Long getId() { return id; }
    public BudgetCategory getBudgetCategory() { return budgetCategory; }
    public String getName() { return name; }
    public String getNotes() { return notes; }
}
