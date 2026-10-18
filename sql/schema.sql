-- ============================================================
-- Warehouse Management - Schema MySQL
-- Charset: utf8mb4 để hỗ trợ tiếng Việt
-- ============================================================

CREATE DATABASE IF NOT EXISTS warehouse_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE warehouse_db;

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100),
    role          ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- categories
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- products
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    sku         VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    category_id INT,
    unit        VARCHAR(20)  NOT NULL DEFAULT 'cái',
    price       DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_stock   INT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- suppliers
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS suppliers (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(20),
    address VARCHAR(255),
    email   VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- customers
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(20),
    address VARCHAR(255),
    email   VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- stock_transactions
-- partner_type: SUPPLIER (khi IMPORT) hoặc CUSTOMER (khi EXPORT)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_transactions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    type         ENUM('IMPORT','EXPORT') NOT NULL,
    partner_id   INT,
    partner_type ENUM('SUPPLIER','CUSTOMER'),
    user_id      INT NOT NULL,
    note         VARCHAR(255),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- stock_transaction_details
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_transaction_details (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    product_id     INT NOT NULL,
    quantity       INT NOT NULL,
    unit_price     DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_detail_tx   FOREIGN KEY (transaction_id) REFERENCES stock_transactions(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_detail_prod FOREIGN KEY (product_id)     REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Seed data
-- 2 tài khoản mặc định, mật khẩu đã được hash bằng BCrypt (cost=10):
--   admin / admin123  -> role ADMIN
--   staff / staff123  -> role STAFF
-- Hash được sinh bởi BCrypt.hashpw("<password>", BCrypt.gensalt(10))
-- và verify OK = true.
-- ------------------------------------------------------------
INSERT INTO users (username, password_hash, full_name, role, active) VALUES
    ('admin', '$2a$10$AI5b52vaw7SbLbJx9xvtKerR8bJQ7FXQ8dyWf5LRnrBjAybSVm2kS', 'Quản trị viên',  'ADMIN', 1),
    ('staff', '$2a$10$xhStfJZ6aqEDVPihkXHTyuUSkUJX5Qn4XnWPQ4R8gorTBfWb8.ywq', 'Nhân viên kho', 'STAFF', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    full_name     = VALUES(full_name),
    role          = VALUES(role),
    active        = VALUES(active);

INSERT INTO categories (name, description) VALUES
    ('Đồ uống', 'Nước giải khát các loại'),
    ('Thực phẩm khô', 'Gạo, mì, ngũ cốc')
ON DUPLICATE KEY UPDATE name = name;

INSERT INTO products (sku, name, category_id, unit, price, min_stock) VALUES
    ('SP001', 'Coca Cola 330ml',         1, 'lon',  10000.00, 50),
    ('SP002', 'Pepsi 330ml',             1, 'lon',  10000.00, 50),
    ('SP003', 'Nước suối Aquafina 500ml', 1, 'chai',  5000.00, 100),
    ('SP004', 'Gạo ST25 5kg',            2, 'túi', 150000.00, 10),
    ('SP005', 'Mì Hảo Hảo',             2, 'thùng', 95000.00, 20)
ON DUPLICATE KEY UPDATE sku = sku;

INSERT INTO suppliers (name, phone, address, email) VALUES
    ('Cty TNHH Nước Giải Khát ABC',  '0901234567', '12 Nguyễn Trãi, Q.5, HCM', 'contact@abc.vn'),
    ('Cty CP Lương Thực Miền Nam',  '0907654321', '88 Trần Hưng Đạo, Q.1, HCM', 'sales@ltmn.vn')
ON DUPLICATE KEY UPDATE name = name;

INSERT INTO customers (name, phone, address, email) VALUES
    ('Khách lẻ Nguyễn Văn A',  '0912345678', '45 Lê Lợi, Q.1, HCM', NULL),
    ('Cửa hàng Bách Hóa Xanh', '0987654321', '99 CMT8, Q.3, HCM', 'bhx@example.vn')
ON DUPLICATE KEY UPDATE name = name;
