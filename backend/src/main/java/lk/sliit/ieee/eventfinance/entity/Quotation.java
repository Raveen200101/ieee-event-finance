package lk.sliit.ieee.eventfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quotations")
public class Quotation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_group_id", nullable = false) private PurchaseGroup purchaseGroup;
    @Column(nullable = false, length = 150) private String supplier;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, length = 1000) private String notes;
    @Column(nullable = false) private boolean selected;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="created_by_id") private ChapterUser createdBy;
    public void setCreatedBy(ChapterUser user) { createdBy=user; }
    public Long getCreatedById() { return createdBy==null?null:createdBy.getId(); }
    protected Quotation() {}
    public Quotation(PurchaseGroup group, String supplier, Integer quantity, BigDecimal unitPrice, String notes) {
        this.purchaseGroup = group;
        update(supplier, quantity, unitPrice, notes);
    }
    public void update(String supplier, Integer quantity, BigDecimal unitPrice, String notes) {
        this.supplier = supplier.strip();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.notes = notes == null ? "" : notes.strip();
        this.selected = false; // Changed terms require a new purchasing decision.
    }
    public Long getId() { return id; }
    public Long getGroupId() { return purchaseGroup.getId(); }
    public String getSupplier() { return supplier; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    public String getNotes() { return notes; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
