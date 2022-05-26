create schema if not exists public;

CREATE TABLE IF NOT EXISTS ticket_items (
    id serial PRIMARY KEY,
    ticket_type VARCHAR ( 255 ) NOT NULL,
    price DOUBLE PRECISION,
    min_age INT,
    max_age INT,
    duration BIGINT
);
CREATE TABLE IF NOT EXISTS ticket_orders (
    order_id serial PRIMARY KEY,
    order_state VARCHAR ( 255 ) NOT NULL,
    total_price DOUBLE PRECISION,
    quantity DOUBLE PRECISION,
    ticket_id DOUBLE PRECISION,
    username VARCHAR ( 255 ) NOT NULL,
    valid_from TIMESTAMPTZ,
    zid VARCHAR ( 255 ) NOT NULL
);
