package lk.sliit.ieee.eventfinance.entity;
import jakarta.persistence.*;
@Entity @Table(name = "roles")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true, length = 40) private ChapterRole name;
    protected Role() {}
    public Long getId() { return id; }
    public ChapterRole getName() { return name; }
}
