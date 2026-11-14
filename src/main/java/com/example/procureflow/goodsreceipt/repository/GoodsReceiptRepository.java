package com.example.procureflow.goodsreceipt.repository;

import com.example.procureflow.goodsreceipt.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
}
