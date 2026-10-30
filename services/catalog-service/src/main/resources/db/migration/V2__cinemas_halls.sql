-- Şube ve salon (db-planlama.md §3.2). Kayıtlar silinmez: salon id'si seans ve bilet snapshot'larında yaşar.
CREATE TABLE cinemas (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NOT NULL,
    created_at DATETIME(3)  NOT NULL,
    updated_at DATETIME(3)  NOT NULL,
    CONSTRAINT pk_cinemas PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- active_layout_id'nin hall_layouts'a FK'si, o tablo V3'te oluşunca eklenir
CREATE TABLE halls (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    cinema_id            BIGINT        NOT NULL,
    name                 VARCHAR(100)  NOT NULL,
    active_layout_id     BIGINT        NULL,
    default_ticket_price DECIMAL(10,2) NOT NULL,
    created_at           DATETIME(3)   NOT NULL,
    updated_at           DATETIME(3)   NOT NULL,
    CONSTRAINT pk_halls PRIMARY KEY (id),
    CONSTRAINT fk_halls_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id),
    CONSTRAINT uq_halls_cinema_name UNIQUE (cinema_id, name),
    CONSTRAINT ck_halls_price CHECK (default_ticket_price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
