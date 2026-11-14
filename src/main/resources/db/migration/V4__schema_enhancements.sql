-- Enhancements for PO items received_quantity, PO totals/quotation link, and sequences

ALTER TABLE purchase_order_items
    ADD COLUMN IF NOT EXISTS received_quantity INT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_po_item_received_le_ordered'
    ) THEN
        ALTER TABLE purchase_order_items
            ADD CONSTRAINT chk_po_item_received_le_ordered CHECK (received_quantity <= quantity);
    END IF;
END $$;

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS quotation_id BIGINT REFERENCES quotations(id),
    ADD COLUMN IF NOT EXISTS total_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0);

ALTER TABLE goods_receipts
    ADD COLUMN IF NOT EXISTS delivery_note_number VARCHAR(100);

ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS code VARCHAR(20);

CREATE SEQUENCE IF NOT EXISTS requisition_seq START WITH 1001;
CREATE SEQUENCE IF NOT EXISTS po_seq START WITH 1001;
CREATE SEQUENCE IF NOT EXISTS receipt_seq START WITH 1001;
