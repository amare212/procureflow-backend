-- Seed demo departments, users, categories, items, suppliers, and initial inventory

-- Departments
INSERT INTO departments (name, code, budget) VALUES
    ('Information Technology', 'IT', 100000.00),
    ('Finance', 'FIN', 50000.00),
    ('Procurement', 'PROC', 30000.00),
    ('Warehouse & Logistics', 'WHL', 25000.00),
    ('Human Resources', 'HR', 20000.00)
ON CONFLICT (name) DO NOTHING;

-- Demo Users (Password: password123)
-- BCrypt: $2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi
INSERT INTO users (full_name, email, password_hash, department_id, role_id, enabled) VALUES
    ('System Administrator', 'admin@procureflow.com', '$2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi',
     (SELECT id FROM departments WHERE code = 'IT'),
     (SELECT id FROM roles WHERE name = 'ADMIN'), TRUE),
    ('Sarah Jenkins', 'manager@procureflow.com', '$2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi',
     (SELECT id FROM departments WHERE code = 'IT'),
     (SELECT id FROM roles WHERE name = 'DEPARTMENT_MANAGER'), TRUE),
    ('Alex Rivera', 'employee@procureflow.com', '$2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi',
     (SELECT id FROM departments WHERE code = 'IT'),
     (SELECT id FROM roles WHERE name = 'EMPLOYEE'), TRUE),
    ('David Kim', 'finance@procureflow.com', '$2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi',
     (SELECT id FROM departments WHERE code = 'FIN'),
     (SELECT id FROM roles WHERE name = 'FINANCE_OFFICER'), TRUE),
    ('Elena Rostova', 'procurement@procureflow.com', '$2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi',
     (SELECT id FROM departments WHERE code = 'PROC'),
     (SELECT id FROM roles WHERE name = 'PROCUREMENT_OFFICER'), TRUE),
    ('Marcus Vance', 'storekeeper@procureflow.com', '$2a$10$WnLhXqo3Pv4tvuievsS1YeklVV/SsGWBS2NzlFXh/dp4cYTyxOOTi',
     (SELECT id FROM departments WHERE code = 'WHL'),
     (SELECT id FROM roles WHERE name = 'STORE_KEEPER'), TRUE)
ON CONFLICT (email) DO NOTHING;

-- Item Categories
INSERT INTO item_categories (name, description) VALUES
    ('Hardware', 'Workstations, laptops, accessories, and peripheral equipment'),
    ('Networking', 'Switches, routers, patch cables, and access points'),
    ('Office Supplies', 'Paper, stationery, pens, toner, and general office materials'),
    ('Furniture', 'Desks, chairs, filing cabinets, and ergonomic furniture')
ON CONFLICT (name) DO NOTHING;

-- Items
INSERT INTO items (sku, name, description, category_id, unit, unit_price, reorder_level, active) VALUES
    ('HW-DESK-01', 'Workstation PC Pro', 'High-performance desktop computer with Core i7, 32GB RAM, 1TB SSD',
     (SELECT id FROM item_categories WHERE name = 'Hardware'), 'unit', 1200.00, 5, TRUE),
    ('NET-SW-01', 'Gigabit Switch 24-Port', 'Managed 24-port Gigabit Ethernet switch with PoE support',
     (SELECT id FROM item_categories WHERE name = 'Networking'), 'unit', 450.00, 3, TRUE),
    ('HW-KB-01', 'Ergonomic Mechanical Keyboard', 'USB-C backlit mechanical keyboard with wrist rest',
     (SELECT id FROM item_categories WHERE name = 'Hardware'), 'pcs', 85.00, 10, TRUE),
    ('HW-UPS-01', 'Smart UPS 1500VA', 'Battery backup uninterruptible power supply LCD 1500VA/900W',
     (SELECT id FROM item_categories WHERE name = 'Hardware'), 'unit', 320.00, 4, TRUE),
    ('OFF-PPR-01', 'A4 Copier Paper Box (5 Reams)', 'High opacity 80gsm multipurpose A4 white copier paper',
     (SELECT id FROM item_categories WHERE name = 'Office Supplies'), 'box', 35.00, 20, TRUE),
    ('FUR-CHR-01', 'Executive Mesh Chair', 'Ergonomic high-back breathable mesh office chair with lumbar support',
     (SELECT id FROM item_categories WHERE name = 'Furniture'), 'unit', 250.00, 5, TRUE)
ON CONFLICT (sku) DO NOTHING;

-- Suppliers
INSERT INTO suppliers (name, contact_name, email, phone, address, status, notes) VALUES
    ('Apex Technologies Ltd', 'John Tech', 'sales@apextech.example', '+1-555-0101', '100 Silicon Way, Tech Park, CA', 'ACTIVE', 'Primary vendor for computing hardware and peripherals'),
    ('Office Solutions Direct', 'Mary Supplies', 'orders@officesolutions.example', '+1-555-0102', '45 Commercial Blvd, Chicago, IL', 'ACTIVE', 'Preferred vendor for furniture and stationery'),
    ('Global Network Gear', 'Robert Switch', 'robert@globalnet.example', '+1-555-0103', '88 Enterprise Drive, Austin, TX', 'ACTIVE', 'Specialist in enterprise networking equipment')
ON CONFLICT (name) DO NOTHING;

-- Supplier Categories
INSERT INTO supplier_categories (supplier_id, category_id)
SELECT s.id, c.id FROM suppliers s, item_categories c
WHERE s.name = 'Apex Technologies Ltd' AND c.name IN ('Hardware')
ON CONFLICT DO NOTHING;

INSERT INTO supplier_categories (supplier_id, category_id)
SELECT s.id, c.id FROM suppliers s, item_categories c
WHERE s.name = 'Office Solutions Direct' AND c.name IN ('Office Supplies', 'Furniture')
ON CONFLICT DO NOTHING;

INSERT INTO supplier_categories (supplier_id, category_id)
SELECT s.id, c.id FROM suppliers s, item_categories c
WHERE s.name = 'Global Network Gear' AND c.name IN ('Networking', 'Hardware')
ON CONFLICT DO NOTHING;

-- Initial Inventory
INSERT INTO inventory (item_id, quantity, location)
SELECT id, 12, 'Shelf A-01' FROM items WHERE sku = 'HW-DESK-01'
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO inventory (item_id, quantity, location)
SELECT id, 6, 'Rack N-02' FROM items WHERE sku = 'NET-SW-01'
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO inventory (item_id, quantity, location)
SELECT id, 25, 'Shelf B-04' FROM items WHERE sku = 'HW-KB-01'
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO inventory (item_id, quantity, location)
SELECT id, 2, 'Shelf A-03' FROM items WHERE sku = 'HW-UPS-01'
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO inventory (item_id, quantity, location)
SELECT id, 40, 'Pallet C-01' FROM items WHERE sku = 'OFF-PPR-01'
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO inventory (item_id, quantity, location)
SELECT id, 8, 'Bay F-01' FROM items WHERE sku = 'FUR-CHR-01'
ON CONFLICT (item_id) DO NOTHING;
