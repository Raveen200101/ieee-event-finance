package lk.sliit.ieee.eventfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "incomes")
public class Income {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false, length = 150)
    private String source;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate incomeDate;

    protected Income() {}

    public Income(Event event, String description, String source, BigDecimal amount, LocalDate incomeDate) {
        this.event = event;
        this.description = description;
        this.source = source;
        this.amount = amount;
        this.incomeDate = incomeDate;
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getIncomeDate() { return incomeDate; }
}
