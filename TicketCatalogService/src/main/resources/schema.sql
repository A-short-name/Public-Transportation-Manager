create schema if not exists public

CREATE TABLE IF NOT EXISTS ticket_items (
    id serial PRIMARY KEY,
    type VARCHAR ( 255 ) NOT NULL,
    price DOUBLE PRECISION
);