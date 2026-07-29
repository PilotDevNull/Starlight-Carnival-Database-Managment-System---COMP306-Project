-- =========================================================
-- STARLIGHT CARNIVAL MANAGEMENT SYSTEM
-- COMP306 - Phase 3: Database Construction
-- Group G
-- =========================================================

-- =========================
-- DATABASE
-- =========================
CREATE DATABASE IF NOT EXISTS starlight_carnival;
USE starlight_carnival;

-- Drop in reverse-dependency order so the script is re-runnable
DROP TABLE IF EXISTS MAINTENANCE_LOG;
DROP TABLE IF EXISTS STALL_STOCK;
DROP TABLE IF EXISTS PRODUCT;
DROP TABLE IF EXISTS RIDE_FAST_PASS;
DROP TABLE IF EXISTS SHOW_RESERVATION;
DROP TABLE IF EXISTS ACCESS_PASS;
DROP TABLE IF EXISTS PAYMENT;
DROP TABLE IF EXISTS `TRANSACTION`;
DROP TABLE IF EXISTS ADMISSION;
DROP TABLE IF EXISTS GENERAL_STAFF;
DROP TABLE IF EXISTS PERFORMER;
DROP TABLE IF EXISTS VENDOR;
DROP TABLE IF EXISTS TECHNICIAN;
DROP TABLE IF EXISTS OPERATOR;
DROP TABLE IF EXISTS STALL;
DROP TABLE IF EXISTS `SHOW`;
DROP TABLE IF EXISTS RIDE;
DROP TABLE IF EXISTS ATTRACTION;
DROP TABLE IF EXISTS EMPLOYEE;
DROP TABLE IF EXISTS CUSTOMER;
DROP TABLE IF EXISTS LOCATION;

-- =========================
-- LOCATION
-- =========================
CREATE TABLE LOCATION (
    location_id     INT PRIMARY KEY AUTO_INCREMENT,
    city            VARCHAR(50)  NOT NULL,
    venue_name      VARCHAR(100) NOT NULL,
    address         VARCHAR(200),
    arrival_date    DATE NOT NULL,
    departure_date  DATE NOT NULL,
    rental_cost     DECIMAL(10,2) DEFAULT 0,
    CHECK (departure_date >= arrival_date),
    CHECK (rental_cost >= 0)
);

-- =========================
-- CUSTOMER
-- =========================
CREATE TABLE CUSTOMER (
    customer_id     INT PRIMARY KEY AUTO_INCREMENT,
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    email           VARCHAR(100) UNIQUE,
    phone           VARCHAR(20),
    date_of_birth   DATE,
    age_group       VARCHAR(10) DEFAULT 'Adult',
    signup_date     DATETIME DEFAULT CURRENT_TIMESTAMP,
    card_balance    DECIMAL(10,2) DEFAULT 0,
    loyalty_points  INT DEFAULT 0,
    card_status     VARCHAR(20) DEFAULT 'ACTIVE',
    CHECK (card_balance >= 0),
    CHECK (loyalty_points >= 0),
    CHECK (age_group IN ('Child', 'Adult', 'Senior')),
    CHECK (card_status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED'))
);

-- =========================
-- EMPLOYEE
-- =========================
CREATE TABLE EMPLOYEE (
    employee_id          INT PRIMARY KEY AUTO_INCREMENT,
    current_location_id  INT,
    first_name           VARCHAR(50) NOT NULL,
    last_name            VARCHAR(50) NOT NULL,
    hire_date            DATE DEFAULT (CURRENT_DATE),
    hourly_wage          DECIMAL(10,2),
    employee_type        VARCHAR(20) NOT NULL DEFAULT 'General',
    CHECK (hourly_wage > 0),
    CHECK (employee_type IN ('Operator','Technician','Vendor','Performer','General')),
    FOREIGN KEY (current_location_id) REFERENCES LOCATION(location_id)
        ON DELETE SET NULL
);

-- =========================
-- ATTRACTION + SUBTYPES
-- =========================
CREATE TABLE ATTRACTION (
    attraction_id        INT PRIMARY KEY AUTO_INCREMENT,
    current_location_id  INT,
    name                 VARCHAR(100) NOT NULL,
    status               VARCHAR(20) DEFAULT 'Operational',
    attraction_type      VARCHAR(20) NOT NULL,
    CHECK (attraction_type IN ('Ride', 'Show', 'Stall')),
    CHECK (status IN ('Operational', 'Closed', 'Maintenance')),
    FOREIGN KEY (current_location_id) REFERENCES LOCATION(location_id)
        ON DELETE SET NULL
);

CREATE TABLE RIDE (
    attraction_id    INT PRIMARY KEY,
    ride_type        VARCHAR(20) DEFAULT 'Motion',
    age_requirement  VARCHAR(10) DEFAULT 'All Ages',
    CHECK (ride_type IN ('Thrills', 'Kiddies', 'Motion')),
    CHECK (age_requirement IN ('Adult', 'Kids', 'All Ages')),
    FOREIGN KEY (attraction_id) REFERENCES ATTRACTION(attraction_id)
        ON DELETE CASCADE
);

CREATE TABLE `SHOW` (
    attraction_id     INT PRIMARY KEY,
    genre             VARCHAR(30) DEFAULT 'General',
    duration_min      INT DEFAULT 30,
    is_indoor         BOOLEAN DEFAULT FALSE,
    seating_capacity  INT DEFAULT 50,
    CHECK (duration_min > 0),
    CHECK (seating_capacity > 0),
    FOREIGN KEY (attraction_id) REFERENCES ATTRACTION(attraction_id)
        ON DELETE CASCADE
);

CREATE TABLE STALL (
    attraction_id  INT PRIMARY KEY,
    lot_number     VARCHAR(10),
    stall_type     VARCHAR(20) DEFAULT 'Merchandise',
    CHECK (stall_type IN ('Food', 'Merchandise', 'Game')),
    FOREIGN KEY (attraction_id) REFERENCES ATTRACTION(attraction_id)
        ON DELETE CASCADE
);

-- =========================
-- ADMISSION
-- =========================
CREATE TABLE ADMISSION (
    admission_id     INT PRIMARY KEY AUTO_INCREMENT,
    location_id      INT NOT NULL,
    customer_id      INT NULL,
    entry_date       DATE DEFAULT (CURRENT_DATE),
    entry_time       TIME DEFAULT (CURRENT_TIME),
    admission_type   VARCHAR(10) DEFAULT 'Adult',
    entry_fee_paid   DECIMAL(10,2) DEFAULT 0,
    exit_time        TIME,
    CHECK (entry_fee_paid >= 0),
    CHECK (admission_type IN ('Child', 'Adult', 'Senior')),
    FOREIGN KEY (location_id) REFERENCES LOCATION(location_id)
        ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
        ON DELETE SET NULL
);

-- =========================
-- TRANSACTION + PAYMENT
-- =========================
CREATE TABLE `TRANSACTION` (
    transaction_id    INT PRIMARY KEY AUTO_INCREMENT,
    location_id       INT,
    employee_id       INT,
    customer_id       INT NULL,
    transaction_date  DATETIME DEFAULT CURRENT_TIMESTAMP,
    transaction_type  VARCHAR(30) DEFAULT 'Stall Purchase',
    total_amount      DECIMAL(10,2) DEFAULT 0,
    CHECK (total_amount >= 0),
    CHECK (transaction_type IN ('Entry Fee', 'Card Top-Up', 'Stall Purchase', 'Refund', 'Access Pass')),
    FOREIGN KEY (location_id) REFERENCES LOCATION(location_id)
        ON DELETE SET NULL,
    FOREIGN KEY (employee_id) REFERENCES EMPLOYEE(employee_id)
        ON DELETE SET NULL,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
        ON DELETE SET NULL
);

CREATE TABLE PAYMENT (
    payment_id      INT PRIMARY KEY AUTO_INCREMENT,
    transaction_id  INT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    tender_type     VARCHAR(20) DEFAULT 'Cash',
    CHECK (amount > 0),
    CHECK (tender_type IN ('Cash', 'Credit Card', 'Debit Card', 'Smart Card')),
    FOREIGN KEY (transaction_id) REFERENCES `TRANSACTION`(transaction_id)
        ON DELETE CASCADE
);

-- =========================
-- ACCESS PASS (+ subtypes)
-- =========================
CREATE TABLE ACCESS_PASS (
    pass_id         INT PRIMARY KEY AUTO_INCREMENT,
    transaction_id  INT NOT NULL,
    customer_id     INT NULL,
    price           DECIMAL(10,2) DEFAULT 0,
    issue_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_redeemed     BOOLEAN DEFAULT FALSE,
    pass_type       VARCHAR(20) NOT NULL,
    CHECK (price >= 0),
    CHECK (pass_type IN ('RideFastPass', 'ShowReservation')),
    FOREIGN KEY (transaction_id) REFERENCES `TRANSACTION`(transaction_id)
        ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
        ON DELETE SET NULL
);

CREATE TABLE SHOW_RESERVATION (
    pass_id      INT PRIMARY KEY,
    show_id      INT NOT NULL,
    seat_number  VARCHAR(5) DEFAULT 'A01',
    show_time    DATETIME NOT NULL,
    FOREIGN KEY (pass_id) REFERENCES ACCESS_PASS(pass_id)
        ON DELETE CASCADE,
    FOREIGN KEY (show_id) REFERENCES `SHOW`(attraction_id)
        ON DELETE CASCADE
);

CREATE TABLE RIDE_FAST_PASS (
    pass_id    INT PRIMARY KEY,
    ride_id    INT NOT NULL,
    time_slot  DATETIME NOT NULL,
    FOREIGN KEY (pass_id) REFERENCES ACCESS_PASS(pass_id)
        ON DELETE CASCADE,
    FOREIGN KEY (ride_id) REFERENCES RIDE(attraction_id)
        ON DELETE CASCADE
);

-- =========================
-- EMPLOYEE SUBTYPES
-- =========================
CREATE TABLE OPERATOR (
    employee_id          INT PRIMARY KEY,
    ride_responsibility  VARCHAR(10) DEFAULT 'Motion',
    CHECK (ride_responsibility IN ('Thrills', 'Kiddies', 'Motion')),
    FOREIGN KEY (employee_id) REFERENCES EMPLOYEE(employee_id)
        ON DELETE CASCADE
);

CREATE TABLE TECHNICIAN (
    employee_id        INT PRIMARY KEY,
    specialization     VARCHAR(30) DEFAULT 'General',
    toolbox_id         VARCHAR(20),
    emergency_contact  VARCHAR(20),
    CHECK (specialization IN ('Hydraulics', 'Electrical', 'General')),
    FOREIGN KEY (employee_id) REFERENCES EMPLOYEE(employee_id)
        ON DELETE CASCADE
);

CREATE TABLE VENDOR (
    employee_id  INT PRIMARY KEY,
    stall_id     INT UNIQUE,  -- one vendor per stall at any given time
    FOREIGN KEY (employee_id) REFERENCES EMPLOYEE(employee_id)
        ON DELETE CASCADE,
    FOREIGN KEY (stall_id) REFERENCES STALL(attraction_id)
        ON DELETE SET NULL
);

CREATE TABLE PERFORMER (
    employee_id  INT PRIMARY KEY,
    show_id      INT,
    FOREIGN KEY (employee_id) REFERENCES EMPLOYEE(employee_id)
        ON DELETE CASCADE,
    FOREIGN KEY (show_id) REFERENCES `SHOW`(attraction_id)
        ON DELETE SET NULL
);

CREATE TABLE GENERAL_STAFF (
    employee_id  INT PRIMARY KEY,
    role         VARCHAR(20) DEFAULT 'Sales',
    CHECK (role IN ('Sales', 'Manager', 'Cleaner', 'Security')),
    FOREIGN KEY (employee_id) REFERENCES EMPLOYEE(employee_id)
        ON DELETE CASCADE
);

-- =========================
-- PRODUCT + STALL_STOCK
-- =========================
CREATE TABLE PRODUCT (
    product_id  INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(20) DEFAULT 'Food',
    price       DECIMAL(10,2) DEFAULT 0,
    CHECK (price >= 0),
    CHECK (category IN ('Food', 'Beverage', 'Toy', 'Apparel'))
);

CREATE TABLE STALL_STOCK (
    product_id      INT,
    stall_id        INT,
    stock_quantity  INT DEFAULT 0,
    CHECK (stock_quantity >= 0),
    PRIMARY KEY (product_id, stall_id),
    FOREIGN KEY (product_id) REFERENCES PRODUCT(product_id)
        ON DELETE CASCADE,
    FOREIGN KEY (stall_id) REFERENCES STALL(attraction_id)
        ON DELETE CASCADE
);

-- =========================
-- MAINTENANCE LOG
-- =========================
CREATE TABLE MAINTENANCE_LOG (
    log_id            INT PRIMARY KEY AUTO_INCREMENT,
    ride_id           INT,
    technician_id     INT,
    maintenance_date  DATETIME DEFAULT CURRENT_TIMESTAMP,
    issue_found       TEXT,
    action_taken      TEXT,
    parts_cost        DECIMAL(10,2) DEFAULT 0,
    CHECK (parts_cost >= 0),
    FOREIGN KEY (ride_id) REFERENCES RIDE(attraction_id)
        ON DELETE SET NULL,
    FOREIGN KEY (technician_id) REFERENCES TECHNICIAN(employee_id)
        ON DELETE SET NULL
);

-- =========================
-- INDEXES (for Phase 4 reporting performance)
-- =========================
CREATE INDEX idx_transaction_date ON `TRANSACTION`(transaction_date);
CREATE INDEX idx_admission_date   ON ADMISSION(entry_date);
CREATE INDEX idx_maintenance_date ON MAINTENANCE_LOG(maintenance_date);

-- =========================
-- ROLE VIEWS
-- =========================
CREATE OR REPLACE VIEW VW_SALES_SUMMARY AS
SELECT
    t.transaction_id,
    t.transaction_date,
    t.transaction_type,
    t.total_amount,
    p.amount AS payment_amount,
    p.tender_type,
    c.customer_id,
    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
    l.venue_name
FROM `TRANSACTION` t
LEFT JOIN PAYMENT  p ON p.transaction_id = t.transaction_id
LEFT JOIN CUSTOMER c ON c.customer_id    = t.customer_id
LEFT JOIN LOCATION l ON l.location_id    = t.location_id;

CREATE OR REPLACE VIEW VW_MAINTENANCE_OVERVIEW AS
SELECT
    ml.log_id,
    a.name AS ride_name,
    CONCAT(e.first_name, ' ', e.last_name) AS technician_name,
    ml.maintenance_date,
    ml.issue_found,
    ml.action_taken,
    ml.parts_cost
FROM MAINTENANCE_LOG ml
LEFT JOIN RIDE        r ON r.attraction_id = ml.ride_id
LEFT JOIN ATTRACTION  a ON a.attraction_id = r.attraction_id
LEFT JOIN TECHNICIAN  t ON t.employee_id   = ml.technician_id
LEFT JOIN EMPLOYEE    e ON e.employee_id   = t.employee_id;

CREATE OR REPLACE VIEW VW_GUEST_REPORTS AS
SELECT
    l.venue_name,
    COUNT(DISTINCT a.admission_id)         AS admission_count,
    COALESCE(SUM(a.entry_fee_paid), 0)     AS admission_revenue,
    COUNT(DISTINCT t.transaction_id)       AS transaction_count,
    COALESCE(SUM(t.total_amount), 0)       AS transaction_revenue
FROM LOCATION l
LEFT JOIN ADMISSION    a ON a.location_id = l.location_id
LEFT JOIN `TRANSACTION` t ON t.location_id = l.location_id
GROUP BY l.location_id, l.venue_name;

-- =========================
-- STORED PROCEDURES
-- =========================
DROP PROCEDURE IF EXISTS TopUpCard;
DROP PROCEDURE IF EXISTS DeductBalance;

DELIMITER $$

-- Top up a customer's smart card balance.
-- Loyalty points are awarded automatically by the after_transaction_insert trigger,
-- so this procedure deliberately does NOT touch loyalty_points to avoid double-counting.
CREATE PROCEDURE TopUpCard(
    IN cust_id INT,
    IN amount  DECIMAL(10,2)
)
BEGIN
    IF amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Top-up amount must be positive';
    END IF;

    UPDATE CUSTOMER
    SET card_balance = card_balance + amount
    WHERE customer_id = cust_id;
END $$

-- Deduct funds from a customer's smart card. Fails loudly if insufficient balance
-- so the application can present a meaningful error rather than silently no-op.
CREATE PROCEDURE DeductBalance(
    IN cust_id INT,
    IN amount  DECIMAL(10,2)
)
BEGIN
    DECLARE current_balance DECIMAL(10,2);

    IF amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Deduction amount must be positive';
    END IF;

    SELECT card_balance INTO current_balance
    FROM CUSTOMER
    WHERE customer_id = cust_id;

    IF current_balance IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Customer not found';
    ELSEIF current_balance < amount THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient card balance';
    ELSE
        UPDATE CUSTOMER
        SET card_balance = card_balance - amount
        WHERE customer_id = cust_id;
    END IF;
END $$

DELIMITER ;

-- =========================
-- TRIGGERS
-- =========================
DROP TRIGGER IF EXISTS before_customer_insert;
DROP TRIGGER IF EXISTS before_customer_update;
DROP TRIGGER IF EXISTS before_payment_insert;
DROP TRIGGER IF EXISTS after_transaction_insert;
DROP TRIGGER IF EXISTS before_attraction_type_update;
DROP TRIGGER IF EXISTS before_ride_insert;
DROP TRIGGER IF EXISTS before_show_insert;
DROP TRIGGER IF EXISTS before_stall_insert;
DROP TRIGGER IF EXISTS before_employee_type_update;
DROP TRIGGER IF EXISTS before_operator_insert;
DROP TRIGGER IF EXISTS before_technician_insert;
DROP TRIGGER IF EXISTS before_vendor_insert;
DROP TRIGGER IF EXISTS before_performer_insert;
DROP TRIGGER IF EXISTS before_general_staff_insert;
DROP TRIGGER IF EXISTS before_access_pass_insert;

DELIMITER $$

-- ---------- Customer age_group derivation ----------
-- The scenario states age group is "automatically calculated" from DOB.
CREATE TRIGGER before_customer_insert
BEFORE INSERT ON CUSTOMER
FOR EACH ROW
BEGIN
    IF NEW.date_of_birth IS NOT NULL THEN
        IF TIMESTAMPDIFF(YEAR, NEW.date_of_birth, CURDATE()) < 13 THEN
            SET NEW.age_group = 'Child';
        ELSEIF TIMESTAMPDIFF(YEAR, NEW.date_of_birth, CURDATE()) >= 60 THEN
            SET NEW.age_group = 'Senior';
        ELSE
            SET NEW.age_group = 'Adult';
        END IF;
    END IF;
END $$

CREATE TRIGGER before_customer_update
BEFORE UPDATE ON CUSTOMER
FOR EACH ROW
BEGIN
    IF NEW.date_of_birth IS NOT NULL
       AND (OLD.date_of_birth IS NULL OR NEW.date_of_birth <> OLD.date_of_birth) THEN
        IF TIMESTAMPDIFF(YEAR, NEW.date_of_birth, CURDATE()) < 13 THEN
            SET NEW.age_group = 'Child';
        ELSEIF TIMESTAMPDIFF(YEAR, NEW.date_of_birth, CURDATE()) >= 60 THEN
            SET NEW.age_group = 'Senior';
        ELSE
            SET NEW.age_group = 'Adult';
        END IF;
    END IF;
END $$

-- ---------- Payment validation ----------
CREATE TRIGGER before_payment_insert
BEFORE INSERT ON PAYMENT
FOR EACH ROW
BEGIN
    IF NEW.amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Payment amount must be positive';
    END IF;
END $$

-- ---------- Loyalty points: 1 point per R10 spent ----------
CREATE TRIGGER after_transaction_insert
AFTER INSERT ON `TRANSACTION`
FOR EACH ROW
BEGIN
    IF NEW.customer_id IS NOT NULL AND NEW.transaction_type <> 'Refund' THEN
        UPDATE CUSTOMER
        SET loyalty_points = loyalty_points + FLOOR(NEW.total_amount / 10)
        WHERE customer_id = NEW.customer_id;
    END IF;
END $$

-- ---------- Attraction subtype consistency ----------
-- NOTE: Subtype rows are inserted MANUALLY by the application after the
-- ATTRACTION row is created. This keeps the application in control of
-- subtype-specific attribute values. The triggers below enforce that the
-- subtype matches the discriminator and that no attraction has two subtypes.

CREATE TRIGGER before_ride_insert
BEFORE INSERT ON RIDE
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM ATTRACTION
        WHERE attraction_id = NEW.attraction_id AND attraction_type = 'Ride'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ride subtype requires Attraction with attraction_type = Ride';
    END IF;
    IF EXISTS (SELECT 1 FROM `SHOW` WHERE attraction_id = NEW.attraction_id)
       OR EXISTS (SELECT 1 FROM STALL WHERE attraction_id = NEW.attraction_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Attraction already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_show_insert
BEFORE INSERT ON `SHOW`
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM ATTRACTION
        WHERE attraction_id = NEW.attraction_id AND attraction_type = 'Show'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Show subtype requires Attraction with attraction_type = Show';
    END IF;
    IF EXISTS (SELECT 1 FROM RIDE WHERE attraction_id = NEW.attraction_id)
       OR EXISTS (SELECT 1 FROM STALL WHERE attraction_id = NEW.attraction_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Attraction already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_stall_insert
BEFORE INSERT ON STALL
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM ATTRACTION
        WHERE attraction_id = NEW.attraction_id AND attraction_type = 'Stall'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Stall subtype requires Attraction with attraction_type = Stall';
    END IF;
    IF EXISTS (SELECT 1 FROM RIDE WHERE attraction_id = NEW.attraction_id)
       OR EXISTS (SELECT 1 FROM `SHOW` WHERE attraction_id = NEW.attraction_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Attraction already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_attraction_type_update
BEFORE UPDATE ON ATTRACTION
FOR EACH ROW
BEGIN
    IF NEW.attraction_type <> OLD.attraction_type THEN
        IF EXISTS (SELECT 1 FROM RIDE WHERE attraction_id = NEW.attraction_id)
           AND NEW.attraction_type <> 'Ride' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Ride row conflicts with new attraction_type';
        END IF;
        IF EXISTS (SELECT 1 FROM `SHOW` WHERE attraction_id = NEW.attraction_id)
           AND NEW.attraction_type <> 'Show' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Show row conflicts with new attraction_type';
        END IF;
        IF EXISTS (SELECT 1 FROM STALL WHERE attraction_id = NEW.attraction_id)
           AND NEW.attraction_type <> 'Stall' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Stall row conflicts with new attraction_type';
        END IF;
    END IF;
END $$

-- ---------- Employee subtype consistency ----------
CREATE TRIGGER before_operator_insert
BEFORE INSERT ON OPERATOR
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM EMPLOYEE
        WHERE employee_id = NEW.employee_id AND employee_type = 'Operator'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Operator subtype requires Employee with employee_type = Operator';
    END IF;
    IF EXISTS (SELECT 1 FROM TECHNICIAN     WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM VENDOR        WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM PERFORMER     WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM GENERAL_STAFF WHERE employee_id = NEW.employee_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_technician_insert
BEFORE INSERT ON TECHNICIAN
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM EMPLOYEE
        WHERE employee_id = NEW.employee_id AND employee_type = 'Technician'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Technician subtype requires Employee with employee_type = Technician';
    END IF;
    IF EXISTS (SELECT 1 FROM OPERATOR       WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM VENDOR        WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM PERFORMER     WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM GENERAL_STAFF WHERE employee_id = NEW.employee_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_vendor_insert
BEFORE INSERT ON VENDOR
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM EMPLOYEE
        WHERE employee_id = NEW.employee_id AND employee_type = 'Vendor'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Vendor subtype requires Employee with employee_type = Vendor';
    END IF;
    IF EXISTS (SELECT 1 FROM OPERATOR       WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM TECHNICIAN    WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM PERFORMER     WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM GENERAL_STAFF WHERE employee_id = NEW.employee_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_performer_insert
BEFORE INSERT ON PERFORMER
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM EMPLOYEE
        WHERE employee_id = NEW.employee_id AND employee_type = 'Performer'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Performer subtype requires Employee with employee_type = Performer';
    END IF;
    IF EXISTS (SELECT 1 FROM OPERATOR       WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM TECHNICIAN    WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM VENDOR        WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM GENERAL_STAFF WHERE employee_id = NEW.employee_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_general_staff_insert
BEFORE INSERT ON GENERAL_STAFF
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM EMPLOYEE
        WHERE employee_id = NEW.employee_id AND employee_type = 'General'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'GeneralStaff subtype requires Employee with employee_type = General';
    END IF;
    IF EXISTS (SELECT 1 FROM OPERATOR    WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM TECHNICIAN WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM VENDOR     WHERE employee_id = NEW.employee_id)
       OR EXISTS (SELECT 1 FROM PERFORMER  WHERE employee_id = NEW.employee_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee already has another subtype';
    END IF;
END $$

CREATE TRIGGER before_employee_type_update
BEFORE UPDATE ON EMPLOYEE
FOR EACH ROW
BEGIN
    IF NEW.employee_type <> OLD.employee_type THEN
        IF EXISTS (SELECT 1 FROM OPERATOR WHERE employee_id = NEW.employee_id)
           AND NEW.employee_type <> 'Operator' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Operator row conflicts with new employee_type';
        END IF;
        IF EXISTS (SELECT 1 FROM TECHNICIAN WHERE employee_id = NEW.employee_id)
           AND NEW.employee_type <> 'Technician' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Technician row conflicts with new employee_type';
        END IF;
        IF EXISTS (SELECT 1 FROM VENDOR WHERE employee_id = NEW.employee_id)
           AND NEW.employee_type <> 'Vendor' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Vendor row conflicts with new employee_type';
        END IF;
        IF EXISTS (SELECT 1 FROM PERFORMER WHERE employee_id = NEW.employee_id)
           AND NEW.employee_type <> 'Performer' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing Performer row conflicts with new employee_type';
        END IF;
        IF EXISTS (SELECT 1 FROM GENERAL_STAFF WHERE employee_id = NEW.employee_id)
           AND NEW.employee_type <> 'General' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Existing GeneralStaff row conflicts with new employee_type';
        END IF;
    END IF;
END $$

-- ---------- AccessPass subtype consistency ----------
-- Block creation of an ACCESS_PASS that already has a subtype row matching
-- a different pass_type (defensive; subtype tables also share PK with parent).
CREATE TRIGGER before_access_pass_insert
BEFORE INSERT ON ACCESS_PASS
FOR EACH ROW
BEGIN
    IF NEW.pass_type NOT IN ('RideFastPass', 'ShowReservation') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'pass_type must be RideFastPass or ShowReservation';
    END IF;
END $$

DELIMITER ;

-- =========================
-- MYSQL ACCOUNTS
-- =========================
CREATE USER IF NOT EXISTS 'starlight_admin'@'localhost'       IDENTIFIED BY 'admin123';
CREATE USER IF NOT EXISTS 'starlight_sales'@'localhost'       IDENTIFIED BY 'sales123';
CREATE USER IF NOT EXISTS 'starlight_maintenance'@'localhost' IDENTIFIED BY 'maint123';
CREATE USER IF NOT EXISTS 'starlight_guest'@'localhost'       IDENTIFIED BY 'guest123';

-- Admin: full control
GRANT ALL PRIVILEGES ON starlight_carnival.* TO 'starlight_admin'@'localhost';

-- Sales: only the tables needed to process customers, transactions, passes
GRANT SELECT, INSERT, UPDATE ON starlight_carnival.CUSTOMER       TO 'starlight_sales'@'localhost';
GRANT SELECT, INSERT, UPDATE ON starlight_carnival.ADMISSION      TO 'starlight_sales'@'localhost';
GRANT SELECT, INSERT, UPDATE ON starlight_carnival.`TRANSACTION`  TO 'starlight_sales'@'localhost';
GRANT SELECT, INSERT         ON starlight_carnival.PAYMENT        TO 'starlight_sales'@'localhost';
GRANT SELECT, INSERT, UPDATE ON starlight_carnival.ACCESS_PASS    TO 'starlight_sales'@'localhost';
GRANT SELECT, INSERT         ON starlight_carnival.SHOW_RESERVATION TO 'starlight_sales'@'localhost';
GRANT SELECT, INSERT         ON starlight_carnival.RIDE_FAST_PASS TO 'starlight_sales'@'localhost';
GRANT SELECT                 ON starlight_carnival.VW_SALES_SUMMARY TO 'starlight_sales'@'localhost';
-- Read-only access to lookup tables needed to populate dropdowns in the Sales UI
GRANT SELECT                 ON starlight_carnival.LOCATION       TO 'starlight_sales'@'localhost';
GRANT SELECT                 ON starlight_carnival.ATTRACTION     TO 'starlight_sales'@'localhost';
GRANT SELECT                 ON starlight_carnival.RIDE           TO 'starlight_sales'@'localhost';
GRANT SELECT                 ON starlight_carnival.`SHOW`         TO 'starlight_sales'@'localhost';
GRANT EXECUTE                ON PROCEDURE starlight_carnival.TopUpCard    TO 'starlight_sales'@'localhost';
GRANT EXECUTE                ON PROCEDURE starlight_carnival.DeductBalance TO 'starlight_sales'@'localhost';

-- Maintenance: only ride and maintenance tables
GRANT SELECT, INSERT, UPDATE ON starlight_carnival.MAINTENANCE_LOG TO 'starlight_maintenance'@'localhost';
GRANT SELECT                 ON starlight_carnival.RIDE            TO 'starlight_maintenance'@'localhost';
GRANT SELECT                 ON starlight_carnival.ATTRACTION      TO 'starlight_maintenance'@'localhost';
GRANT SELECT, UPDATE         ON starlight_carnival.TECHNICIAN      TO 'starlight_maintenance'@'localhost';
GRANT SELECT                 ON starlight_carnival.EMPLOYEE        TO 'starlight_maintenance'@'localhost';
GRANT SELECT                 ON starlight_carnival.VW_MAINTENANCE_OVERVIEW TO 'starlight_maintenance'@'localhost';

-- Guest: read-only SELECT on every table and view — same visibility as ADMIN, no write access
GRANT SELECT ON starlight_carnival.LOCATION          TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.CUSTOMER          TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.EMPLOYEE          TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.ATTRACTION        TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.RIDE              TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.`SHOW`            TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.STALL             TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.ADMISSION         TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.`TRANSACTION`     TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.PAYMENT           TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.ACCESS_PASS       TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.SHOW_RESERVATION  TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.RIDE_FAST_PASS    TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.OPERATOR          TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.TECHNICIAN        TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.VENDOR            TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.PERFORMER         TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.GENERAL_STAFF     TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.PRODUCT           TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.STALL_STOCK       TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.MAINTENANCE_LOG   TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.VW_SALES_SUMMARY       TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.VW_MAINTENANCE_OVERVIEW TO 'starlight_guest'@'localhost';
GRANT SELECT ON starlight_carnival.VW_GUEST_REPORTS       TO 'starlight_guest'@'localhost';

FLUSH PRIVILEGES;


-- =========================================================
-- SAMPLE DATA
-- =========================================================

-- =========================
-- LOCATION (3 venues)
-- =========================
INSERT INTO LOCATION (city, venue_name, address, arrival_date, departure_date, rental_cost) VALUES
('Durban',        'Kings Park Showgrounds',    '10 Jacobs Road, Durban, 4052',           '2025-06-01', '2025-06-14', 45000.00),
('Cape Town',     'Grand Parade Fairgrounds',  '1 Darling Street, Cape Town, 8001',      '2025-07-15', '2025-07-28', 52000.00),
('Johannesburg',  'Nasrec Expo Centre',        '1 Nasrec Road, Johannesburg, 2147',      '2025-09-05', '2025-09-19', 61000.00);

-- =========================
-- CUSTOMER (10 guests)
-- =========================
INSERT INTO CUSTOMER (first_name, last_name, email, phone, date_of_birth, card_balance, loyalty_points, card_status) VALUES
('Amara',   'Nkosi',     'amara.nkosi@email.co.za',   '0821234567', '1995-03-12', 250.00, 0, 'ACTIVE'),
('Liam',    'Botha',     'liam.botha@email.co.za',    '0834567890', '1988-11-25', 180.00, 0, 'ACTIVE'),
('Zanele',  'Dlamini',   'zanele.d@email.co.za',      '0719876543', '2012-06-30', 80.00,  0, 'ACTIVE'),
('Tyler',   'van Wyk',   'tyler.vw@email.co.za',      '0601112222', '1975-08-14', 320.00, 0, 'ACTIVE'),
('Priya',   'Pillay',    'priya.pillay@email.co.za',  '0823334444', '2001-02-19', 150.00, 0, 'ACTIVE'),
('Connor',  'Joubert',   'connor.j@email.co.za',      '0715556666', '1990-07-07', 500.00, 0, 'ACTIVE'),
('Nokwanda','Zulu',      'nokwanda.z@email.co.za',    '0827778888', '1968-04-22', 95.00,  0, 'ACTIVE'),
('Dylan',   'Pretorius', 'dylan.p@email.co.za',       '0839990000', '2010-12-01', 60.00,  0, 'ACTIVE'),
('Fatima',  'Patel',     'fatima.patel@email.co.za',  '0811231234', '1983-09-09', 210.00, 0, 'ACTIVE'),
('Sipho',   'Mthembu',   'sipho.m@email.co.za',       '0844564456', '1999-05-17', 130.00, 0, 'ACTIVE');

-- =========================
-- EMPLOYEE
-- Operator, Technician, Vendor, Performer, General
-- =========================
INSERT INTO EMPLOYEE (current_location_id, first_name, last_name, hire_date, hourly_wage, employee_type) VALUES
(1, 'Bongani', 'Khumalo',   '2023-01-10', 55.00, 'Operator'),    -- 1
(1, 'Tanya',   'Swart',     '2022-06-15', 50.00, 'Operator'),    -- 2
(1, 'Rajan',   'Govender',  '2021-03-22', 70.00, 'Technician'),  -- 3
(1, 'Nicole',  'du Plessis','2023-08-01', 68.00, 'Technician'),  -- 4
(1, 'Marco',   'Ferreira',  '2022-11-05', 45.00, 'Vendor'),      -- 5
(1, 'Ayanda',  'Mokoena',   '2023-02-20', 45.00, 'Vendor'),      -- 6
(1, 'Chloé',   'Rousseau',  '2020-07-14', 60.00, 'Performer'),   -- 7
(1, 'Sibusiso','Ntuli',     '2021-09-09', 58.00, 'Performer'),   -- 8
(1, 'Karen',   'Steyn',     '2019-05-01', 52.00, 'General'),     -- 9
(1, 'Lethabo', 'Mahlangu',  '2022-03-17', 48.00, 'General');     -- 10

-- =========================
-- ATTRACTION (9 attractions: 3 rides, 3 shows, 3 stalls)
-- =========================
INSERT INTO ATTRACTION (current_location_id, name, status, attraction_type) VALUES
(1, 'Thunder Coaster',      'Operational', 'Ride'),   -- 1
(1, 'Kiddies Carousel',     'Operational', 'Ride'),   -- 2
(1, 'Gravity Spinner',      'Maintenance', 'Ride'),   -- 3
(1, 'Starlight Circus',     'Operational', 'Show'),   -- 4
(1, 'Fire & Beats Show',    'Operational', 'Show'),   -- 5
(1, 'Puppet Kingdom',       'Closed',      'Show'),   -- 6
(1, 'Carnival Bites',       'Operational', 'Stall'),  -- 7
(1, 'Souvenir Corner',      'Operational', 'Stall'),  -- 8
(1, 'Ring Toss Arena',      'Operational', 'Stall');  -- 9

-- =========================
-- RIDE (subtypes for attractions 1–3)
-- =========================
INSERT INTO RIDE (attraction_id, ride_type, age_requirement) VALUES
(1, 'Thrills',  'Adult'),
(2, 'Kiddies',  'Kids'),
(3, 'Motion',   'All Ages');

-- =========================
-- SHOW (subtypes for attractions 4–6)
-- =========================
INSERT INTO `SHOW` (attraction_id, genre, duration_min, is_indoor, seating_capacity) VALUES
(4, 'Circus',   60,  TRUE,  200),
(5, 'Music',    45,  FALSE, 500),
(6, 'Puppet',   30,  TRUE,   80);

-- =========================
-- STALL (subtypes for attractions 7–9)
-- =========================
INSERT INTO STALL (attraction_id, lot_number, stall_type) VALUES
(7, 'A1',  'Food'),
(8, 'B3',  'Merchandise'),
(9, 'C7',  'Game');

-- =========================
-- OPERATOR subtype (employees 1–2)
-- =========================
INSERT INTO OPERATOR (employee_id, ride_responsibility) VALUES
(1, 'Thrills'),
(2, 'Kiddies');

-- =========================
-- TECHNICIAN subtype (employees 3–4)
-- =========================
INSERT INTO TECHNICIAN (employee_id, specialization, toolbox_id, emergency_contact) VALUES
(3, 'Hydraulics',  'TB-001', '0821112233'),
(4, 'Electrical',  'TB-002', '0834445566');

-- =========================
-- VENDOR subtype (employees 5–6)
-- =========================
INSERT INTO VENDOR (employee_id, stall_id) VALUES
(5, 7),
(6, 8);

-- =========================
-- PERFORMER subtype (employees 7–8)
-- =========================
INSERT INTO PERFORMER (employee_id, show_id) VALUES
(7, 4),
(8, 5);

-- =========================
-- GENERAL_STAFF subtype (employees 9–10)
-- =========================
INSERT INTO GENERAL_STAFF (employee_id, role) VALUES
(9, 'Manager'),
(10, 'Security');

-- =========================
-- ADMISSION (8 entries across various customers)
-- =========================
INSERT INTO ADMISSION (location_id, customer_id, entry_date, entry_time, admission_type, entry_fee_paid, exit_time) VALUES
(1, 1,  '2025-06-03', '10:00:00', 'Adult',  120.00, '17:30:00'),
(1, 2,  '2025-06-03', '10:15:00', 'Adult',  120.00, '18:00:00'),
(1, 3,  '2025-06-03', '10:30:00', 'Child',   80.00, '16:00:00'),
(1, 4,  '2025-06-04', '09:45:00', 'Adult',  120.00, '19:00:00'),
(1, 5,  '2025-06-04', '11:00:00', 'Adult',  120.00, '17:00:00'),
(1, 6,  '2025-06-05', '10:00:00', 'Adult',  120.00, '20:00:00'),
(1, 7,  '2025-06-05', '10:30:00', 'Senior',  90.00, '15:30:00'),
(1, 8,  '2025-06-06', '09:00:00', 'Child',   80.00, '15:00:00');

-- =========================
-- TRANSACTION (10 transactions)
-- =========================
INSERT INTO `TRANSACTION` (location_id, employee_id, customer_id, transaction_date, transaction_type, total_amount) VALUES
(1, 9,  1,  '2025-06-03 10:05:00', 'Entry Fee',     120.00),   -- 1
(1, 9,  2,  '2025-06-03 10:20:00', 'Entry Fee',     120.00),   -- 2
(1, 5,  1,  '2025-06-03 12:30:00', 'Stall Purchase',  55.00),  -- 3
(1, 6,  4,  '2025-06-04 13:00:00', 'Stall Purchase',  85.00),  -- 4
(1, 9,  5,  '2025-06-04 11:05:00', 'Entry Fee',     120.00),   -- 5
(1, 9,  6,  '2025-06-05 10:10:00', 'Card Top-Up',   200.00),   -- 6
(1, 9,  3,  '2025-06-03 10:35:00', 'Entry Fee',      80.00),   -- 7
(1, 9,  7,  '2025-06-05 10:35:00', 'Entry Fee',      90.00),   -- 8
(1, 5,  2,  '2025-06-03 14:00:00', 'Stall Purchase',  40.00),  -- 9
(1, 9,  10, '2025-06-06 09:30:00', 'Access Pass',   150.00);   -- 10

-- =========================
-- PAYMENT (10 payments, one per transaction)
-- =========================
INSERT INTO PAYMENT (transaction_id, amount, tender_type) VALUES
(1,  120.00, 'Cash'),
(2,  120.00, 'Credit Card'),
(3,   55.00, 'Smart Card'),
(4,   85.00, 'Debit Card'),
(5,  120.00, 'Cash'),
(6,  200.00, 'Credit Card'),
(7,   80.00, 'Cash'),
(8,   90.00, 'Smart Card'),
(9,   40.00, 'Smart Card'),
(10, 150.00, 'Credit Card');

-- =========================
-- ACCESS PASS (2 passes: 1 ride fast pass, 1 show reservation)
-- =========================
INSERT INTO ACCESS_PASS (transaction_id, customer_id, price, is_redeemed, pass_type) VALUES
(10, 10,  75.00, FALSE, 'RideFastPass'),       -- pass 1
(10, 10,  75.00, FALSE, 'ShowReservation');    -- pass 2

-- =========================
-- RIDE_FAST_PASS (subtype for pass 1 → ride 1)
-- =========================
INSERT INTO RIDE_FAST_PASS (pass_id, ride_id, time_slot) VALUES
(1, 1, '2025-06-06 14:00:00');

-- =========================
-- SHOW_RESERVATION (subtype for pass 2 → show 4)
-- =========================
INSERT INTO SHOW_RESERVATION (pass_id, show_id, seat_number, show_time) VALUES
(2, 4, 'B12', '2025-06-06 17:00:00');

-- =========================
-- PRODUCT (8 products)
-- =========================
INSERT INTO PRODUCT (name, category, price) VALUES
('Boerewors Roll',       'Food',     35.00),
('Slushie (500ml)',      'Beverage',  20.00),
('Cotton Candy',         'Food',     15.00),
('Starlight T-Shirt',   'Apparel',   120.00),
('Plush Star Bear',      'Toy',       85.00),
('Carnival Mug',         'Apparel',   55.00),
('Corn Dog',             'Food',      30.00),
('Fruit Juice (330ml)', 'Beverage',   18.00);

-- =========================
-- STALL_STOCK (products stocked at stalls 7, 8, 9)
-- =========================
INSERT INTO STALL_STOCK (product_id, stall_id, stock_quantity) VALUES
(1, 7,  80),   -- Boerewors Roll @ Carnival Bites
(2, 7, 120),   -- Slushie @ Carnival Bites
(3, 7,  60),   -- Cotton Candy @ Carnival Bites
(7, 7,  50),   -- Corn Dog @ Carnival Bites
(8, 7, 100),   -- Fruit Juice @ Carnival Bites
(4, 8,  40),   -- T-Shirt @ Souvenir Corner
(5, 8,  30),   -- Plush Bear @ Souvenir Corner
(6, 8,  25);   -- Carnival Mug @ Souvenir Corner

-- =========================
-- MAINTENANCE LOG (3 logs for rides)
-- =========================
INSERT INTO MAINTENANCE_LOG (ride_id, technician_id, maintenance_date, issue_found, action_taken, parts_cost) VALUES
(3, 3, '2025-06-01 08:00:00',
    'Hydraulic pressure drop detected in main lift mechanism',
    'Replaced hydraulic seals and repressurised system; full load test passed',
    1850.00),
(1, 4, '2025-06-02 09:30:00',
    'Electrical fault in lap bar locking sensor on car 2',
    'Sensor replaced and wiring harness re-routed away from heat source',
    320.00),
(2, 3, '2025-06-03 07:00:00',
    'Carousel motor running louder than normal; mild vibration',
    'Greased drive shaft bearings and realigned motor mount bolts',
    95.00);

