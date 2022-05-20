CREATE TABLE IF NOT EXISTS ticket_items (
                                            id SERIAL PRIMARY KEY,
                                            type VARCHAR(255) NOT NULL,
                                            price DOUBLE
);