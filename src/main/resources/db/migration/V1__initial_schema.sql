CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE departments (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL UNIQUE,
    budget     NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (budget >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    department_id BIGINT REFERENCES departments(id),
    role_id       BIGINT NOT NULL REFERENCES roles(id),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE suppliers (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(150) NOT NULL UNIQUE,
    contact_name VARCHAR(150),
    email        VARCHAR(180),
    phone        VARCHAR(40),
    address      VARCHAR(255),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE item_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE supplier_categories (
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES item_categories(id),
    PRIMARY KEY (supplier_id, category_id)
);

CREATE TABLE items (
    id            BIGSERIAL PRIMARY KEY,
    sku           VARCHAR(60) NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    category_id   BIGINT NOT NULL REFERENCES item_categories(id),
    unit          VARCHAR(30) NOT NULL DEFAULT 'pcs',
    unit_price    NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    reorder_level INT NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE requisitions (
    id                 BIGSERIAL PRIMARY KEY,
    requisition_number VARCHAR(40) NOT NULL UNIQUE,
    employee_id        BIGINT NOT NULL REFERENCES users(id),
    department_id      BIGINT NOT NULL REFERENCES departments(id),
    justification      TEXT NOT NULL,
    priority           VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    required_date      DATE,
    status             VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    version            BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE requisition_items (
    id                    BIGSERIAL PRIMARY KEY,
    requisition_id        BIGINT NOT NULL REFERENCES requisitions(id) ON DELETE CASCADE,
    item_id               BIGINT NOT NULL REFERENCES items(id),
    quantity              INT NOT NULL CHECK (quantity > 0),
    estimated_unit_price  NUMERIC(14,2) NOT NULL CHECK (estimated_unit_price >= 0)
);

CREATE TABLE approvals (
    id             BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL REFERENCES requisitions(id) ON DELETE CASCADE,
    stage          VARCHAR(30) NOT NULL,
    decision       VARCHAR(20) NOT NULL,
    actor_id       BIGINT NOT NULL REFERENCES users(id),
    comment        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reject_reason CHECK (decision <> 'REJECTED' OR (comment IS NOT NULL AND length(trim(comment)) > 0))
);

CREATE TABLE quotations (
    id             BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL REFERENCES requisitions(id) ON DELETE CASCADE,
    supplier_id    BIGINT NOT NULL REFERENCES suppliers(id),
    total_amount   NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    valid_until    DATE,
    selected       BOOLEAN NOT NULL DEFAULT FALSE,
    notes          TEXT,
    created_by     BIGINT NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE purchase_orders (
    id                     BIGSERIAL PRIMARY KEY,
    po_number              VARCHAR(40) NOT NULL UNIQUE,
    requisition_id         BIGINT NOT NULL REFERENCES requisitions(id),
    supplier_id            BIGINT NOT NULL REFERENCES suppliers(id),
    status                 VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    expected_delivery_date DATE,
    created_by             BIGINT NOT NULL REFERENCES users(id),
    version                BIGINT NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE purchase_order_items (
    id                BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item_id           BIGINT NOT NULL REFERENCES items(id),
    quantity          INT NOT NULL CHECK (quantity > 0),
    unit_price        NUMERIC(14,2) NOT NULL CHECK (unit_price >= 0)
);

-- Duplicate-receipt protection: one receipt per PO and a unique idempotency key.
CREATE TABLE goods_receipts (
    id                BIGSERIAL PRIMARY KEY,
    receipt_number    VARCHAR(40) NOT NULL UNIQUE,
    purchase_order_id BIGINT NOT NULL UNIQUE REFERENCES purchase_orders(id),
    idempotency_key   VARCHAR(80) UNIQUE,
    received_by       BIGINT NOT NULL REFERENCES users(id),
    received_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified          BOOLEAN NOT NULL DEFAULT FALSE,
    notes             TEXT
);

CREATE TABLE goods_receipt_items (
    id                     BIGSERIAL PRIMARY KEY,
    goods_receipt_id       BIGINT NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    purchase_order_item_id BIGINT NOT NULL REFERENCES purchase_order_items(id),
    item_id                BIGINT NOT NULL REFERENCES items(id),
    quantity_received      INT NOT NULL CHECK (quantity_received > 0)
);

CREATE TABLE inventory (
    id         BIGSERIAL PRIMARY KEY,
    item_id    BIGINT NOT NULL UNIQUE REFERENCES items(id),
    quantity   INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    location   VARCHAR(80),
    version    BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stock_movements (
    id             BIGSERIAL PRIMARY KEY,
    item_id        BIGINT NOT NULL REFERENCES items(id),
    movement_type  VARCHAR(20) NOT NULL,
    quantity       INT NOT NULL CHECK (quantity <> 0),
    reference_type VARCHAR(40),
    reference_id   BIGINT,
    department_id  BIGINT REFERENCES departments(id),
    recipient_id   BIGINT REFERENCES users(id),
    performed_by   BIGINT NOT NULL REFERENCES users(id),
    note           TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
