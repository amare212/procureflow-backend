package com.example.procureflow.requisition.entity;

import com.example.procureflow.item.entity.Item;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "requisition_items")
public class RequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private Requisition requisition;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "estimated_unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedUnitPrice;

    public RequisitionItem() {
    }

    public RequisitionItem(Item item, int quantity, BigDecimal estimatedUnitPrice) {
        this.item = item;
        this.quantity = quantity;
        this.estimatedUnitPrice = estimatedUnitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Requisition getRequisition() {
        return requisition;
    }

    public void setRequisition(Requisition requisition) {
        this.requisition = requisition;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getEstimatedUnitPrice() {
        return estimatedUnitPrice;
    }

    public void setEstimatedUnitPrice(BigDecimal estimatedUnitPrice) {
        this.estimatedUnitPrice = estimatedUnitPrice;
    }
}
