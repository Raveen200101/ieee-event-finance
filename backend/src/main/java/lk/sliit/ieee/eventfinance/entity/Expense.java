package lk.sliit.ieee.eventfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate expenseDate;

    protected Expense() {}

    public Expense(Event event, String description, String category, BigDecimal amount, LocalDate expenseDate) {
        this.event = event;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
}
