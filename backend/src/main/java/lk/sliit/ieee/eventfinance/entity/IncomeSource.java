package lk.sliit.ieee.eventfinance.entity;
import jakarta.persistence.*;
@Entity @Table(name="income_sources")
public class IncomeSource {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="event_id") private Event event;
    @Column(nullable=false,length=150) private String name;
    @Column(nullable=false,length=150) private String nameKey;
    protected IncomeSource() {}
    public IncomeSource(Event event,String name) { this.event=event;this.name=name.strip();nameKey=key(name); }
    public static String key(String value) { return value.strip().toLowerCase(java.util.Locale.ROOT); }
    public String getName() { return name; }
}
