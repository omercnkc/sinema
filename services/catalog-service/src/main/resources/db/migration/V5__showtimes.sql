-- Seans (db-planlama.md §3.2, K10, K17, K23, K26, K34). Seans silinmez ve iptal edilmez (K34);
-- id'si booking'de showtime_id ve bilet snapshot'larında yaşar.
CREATE TABLE showtimes (
    id                         BIGINT        NOT NULL AUTO_INCREMENT,
    movie_id                   BIGINT        NOT NULL,
    hall_id                    BIGINT        NOT NULL,
    -- Mühürlü düzen sürümü: seans oluşturulduğunda salonun aktif düzeni (K17); yalnızca K35 göçü değiştirir (M8)
    layout_id                  BIGINT        NOT NULL,
    starts_at                  DATETIME(3)   NOT NULL,
    -- starts_at + film süresi + seans arası payı (K26)
    ends_at                    DATETIME(3)   NOT NULL,
    ticket_price               DECIMAL(10,2) NOT NULL,
    -- Satış kapanışı = starts_at + bu ofset; negatif = başlamadan önce (K10, varsayılan +10)
    sales_close_offset_minutes INT           NOT NULL,
    status                     VARCHAR(20)   NOT NULL,
    created_at                 DATETIME(3)   NOT NULL,
    updated_at                 DATETIME(3)   NOT NULL,
    CONSTRAINT pk_showtimes PRIMARY KEY (id),
    CONSTRAINT fk_showtimes_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT fk_showtimes_hall FOREIGN KEY (hall_id) REFERENCES halls (id),
    CONSTRAINT fk_showtimes_layout FOREIGN KEY (layout_id) REFERENCES hall_layouts (id),
    CONSTRAINT ck_showtimes_interval CHECK (ends_at > starts_at),
    CONSTRAINT ck_showtimes_price CHECK (ticket_price >= 0),
    CONSTRAINT ck_showtimes_status CHECK (status IN ('SCHEDULED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Salondaki çakışma sorgusu ve "salonda süren seans var mı" (K13) kontrolü
CREATE INDEX ix_showtimes_hall_starts ON showtimes (hall_id, starts_at);
-- Film detayındaki "bu filmin seansları" listesi
CREATE INDEX ix_showtimes_movie_starts ON showtimes (movie_id, starts_at);
