package com.example.procureflow.goodsreceipt.repository;

import com.example.procureflow.goodsreceipt.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long>, JpaSpecificationExecutor<GoodsReceipt> {
    Optional<GoodsReceipt> findByIdempotencyKey(String idempotencyKey);
    Optional<GoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);
    Optional<GoodsReceipt> findByPurchaseOrderIdAndDeliveryNoteNumber(Long purchaseOrderId, String deliveryNoteNumber);

    @Query(value = "SELECT nextval('receipt_seq')", nativeQuery = true)
    Long getNextSequenceValue();
}
