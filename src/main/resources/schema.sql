CREATE TABLE IF NOT EXISTS fruta (
    id        BIGINT         NOT NULL AUTO_INCREMENT,
    nombre    VARCHAR(100)   NOT NULL,
    precio    DECIMAL(10, 2) NOT NULL,
    cantidad  INT            NOT NULL DEFAULT 0,
    unidad    VARCHAR(20)    NOT NULL,
    CONSTRAINT pk_fruta PRIMARY KEY (id),
    CONSTRAINT uk_fruta_nombre UNIQUE (nombre),
    CONSTRAINT ck_fruta_precio CHECK (precio >= 0),
    CONSTRAINT ck_fruta_cantidad CHECK (cantidad >= 0)
);
