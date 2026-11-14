package com.example.procureflow.goodsreceipt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    // TODO: add fields matching db/migration/V1__initial_schema.sql
}
