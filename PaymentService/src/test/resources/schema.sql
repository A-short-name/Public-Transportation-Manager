create schema if not exists public;

CREATE TABLE IF NOT EXISTS transactions (
    id serial PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    total_cost DOUBLE PRECISION,
    credit_card_number VARCHAR(255) NOT NULL,
    card_holder VARCHAR(255) NOT NULL,
    order_id BIGINT
);
