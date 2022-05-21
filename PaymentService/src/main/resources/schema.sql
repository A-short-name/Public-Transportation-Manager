create schema if not exists public;

CREATE TABLE IF NOT EXISTS transactions (
    id serial PRIMARY KEY,
    username VARCHAR(255) NOT NULL
);