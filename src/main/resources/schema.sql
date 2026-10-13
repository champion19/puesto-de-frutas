CREATE TABLE IF NOT EXISTS fruit (
    id        BIGINT         NOT NULL AUTO_INCREMENT,
    name      VARCHAR(100)   NOT NULL,
    price     DECIMAL(10, 2) NOT NULL,
    quantity  DECIMAL(10, 3) NOT NULL DEFAULT 0,
    unit      VARCHAR(20)    NOT NULL,
    active    BOOLEAN        NOT NULL DEFAULT TRUE,
    version   BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_fruit PRIMARY KEY (id),
    CONSTRAINT uk_fruit_name UNIQUE (name),
    CONSTRAINT ck_fruit_price CHECK (price >= 0),
    CONSTRAINT ck_fruit_quantity CHECK (quantity >= 0)
);
