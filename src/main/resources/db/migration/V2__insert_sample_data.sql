-- 초기 테스트용 샘플 고객 데이터
INSERT INTO customer (customer_id, name, email, created_at, updated_at) VALUES 
('CUST-001', '홍길동', 'hong@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CUST-002', '이순신', 'lee@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 초기 테스트용 샘플 지갑 데이터
INSERT INTO wallet (customer_id, balance, created_at, updated_at) VALUES 
('CUST-001', 100000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CUST-002', 50000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
