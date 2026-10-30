-- Rezervasyon ve koltuk durumu (db-planlama.md §3.3, K9, K24, K30, K32, K40, K43).
-- showtime_id, hall_id, layout_cell_id (catalog) ve user_id (auth) yumuşak referanstır: başka servisin tablosuna FK yok;
-- o kayıtlar hiç silinmez (db-planlama §5-1).

CREATE TABLE reservations (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT        NOT NULL,
    showtime_id             BIGINT        NOT NULL,
    hall_id                 BIGINT        NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    total_amount            DECIMAL(10,2) NOT NULL,
    -- Hold bitişi = min(oluşturma + hold süresi, satış kapanışı); ödeme başlayınca bir kez uzar (K32)
    expires_at              DATETIME(3)   NOT NULL,
    payment_window_extended BOOLEAN       NOT NULL DEFAULT FALSE,
    released_at             DATETIME(3)   NULL,
    -- Yalnızca CONFIRMED rezervasyonda; rastgele ve tahmin edilemez (K30)
    ticket_code             VARCHAR(32)   NULL,
    -- Bilet snapshot'ı (planlama §3.4-1): catalog'daki ad sonradan değişse de bilet değişmez
    movie_title             VARCHAR(255)  NOT NULL,
    cinema_name             VARCHAR(150)  NOT NULL,
    hall_name               VARCHAR(100)  NOT NULL,
    starts_at               DATETIME(3)   NOT NULL,
    -- Kullanıcı başına seansta tek aktif işlem (K24) DB garantisiyle (K43): yalnızca PENDING_PAYMENT satırda dolu,
    -- diğerlerinde NULL. UNIQUE indeks birden fazla NULL'a izin verir; MySQL'de kısmi indeks olmadığı için bu yol.
    active_pending_key      VARCHAR(80) GENERATED ALWAYS AS (
                                CASE WHEN status = 'PENDING_PAYMENT' THEN CONCAT(user_id, '-', showtime_id) END) STORED,
    created_at              DATETIME(3)   NOT NULL,
    confirmed_at            DATETIME(3)   NULL,
    updated_at              DATETIME(3)   NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT uk_reservations_active_pending UNIQUE (active_pending_key),
    CONSTRAINT uk_reservations_ticket_code UNIQUE (ticket_code),
    CONSTRAINT ck_reservations_status
        CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'EXPIRED', 'FAILED', 'RELEASED')),
    CONSTRAINT ck_reservations_amount CHECK (total_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- "Rezervasyonlarım" listesi
CREATE INDEX ix_reservations_user_created ON reservations (user_id, created_at);
-- K29 terk sayacı (son 30 dk'daki EXPIRED/RELEASED)
CREATE INDEX ix_reservations_user_status_expires ON reservations (user_id, status, expires_at);
-- Süre dolumu job'ı (PENDING_PAYMENT ve süresi geçmiş)
CREATE INDEX ix_reservations_status_expires ON reservations (status, expires_at);

-- Rezervasyondaki koltuklar: etiket ve fiyat o anki değerin kopyası (bilet sonradan değişmez).
-- seat_bookings'e FK yok: süresi dolan HELD satırlar silinir (K43), bu satırlar ise geçmiş olarak kalır.
CREATE TABLE reservation_seats (
    reservation_id BIGINT        NOT NULL,
    layout_cell_id BIGINT        NOT NULL,
    seat_label     VARCHAR(10)   NOT NULL,
    unit_price     DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_reservation_seats PRIMARY KEY (reservation_id, layout_cell_id),
    CONSTRAINT fk_reservation_seats_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT ck_reservation_seats_price CHECK (unit_price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Koltuk durumunun tek doğruluk kaynağı: satır yoksa koltuk boştur.
CREATE TABLE seat_bookings (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    showtime_id     BIGINT      NOT NULL,
    layout_cell_id  BIGINT      NOT NULL,
    status          VARCHAR(10) NOT NULL,
    -- HELD'de dolu, SOLD'da NULL; bu andan sonra HELD satır "boş" sayılır ve devralınabilir (§4.3)
    hold_expires_at DATETIME(3) NULL,
    user_id         BIGINT      NOT NULL,
    reservation_id  BIGINT      NOT NULL,
    version         BIGINT      NOT NULL,
    created_at      DATETIME(3) NOT NULL,
    updated_at      DATETIME(3) NOT NULL,
    CONSTRAINT pk_seat_bookings PRIMARY KEY (id),
    -- K9: aynı seansta aynı koltuk için tek satır. Çift satışı kod değil bu kısıt engeller (§4.1).
    CONSTRAINT uk_seat_bookings_showtime_cell UNIQUE (showtime_id, layout_cell_id),
    CONSTRAINT fk_seat_bookings_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT ck_seat_bookings_status CHECK (status IN ('HELD', 'SOLD')),
    CONSTRAINT ck_seat_bookings_hold_expiry CHECK ((status = 'HELD') = (hold_expires_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Doluluk sorgusu (seat-status): seansın SOLD ve süresi dolmamış HELD satırları
CREATE INDEX ix_seat_bookings_showtime_status ON seat_bookings (showtime_id, status, hold_expires_at);
