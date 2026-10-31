-- Ödemeler ve ham sağlayıcı kayıtları (db-planlama.md §3.4, planlama §4.8, §7.6, K39, K43).
-- reservation_id (booking) ve user_id (auth) yumuşak referanstır: başka servisin tablosuna FK yok.

CREATE TABLE payments (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    -- Rezervasyon başına tek ödeme (§7.2): tekrar gelen "öde" isteği aynı kaydı döner
    reservation_id            BIGINT        NOT NULL,
    user_id                   BIGINT        NOT NULL,
    provider                  VARCHAR(20)   NOT NULL,
    -- INITIATED: kayıt açıldı, sağlayıcıda başlatılmadı | PENDING: kullanıcı ödeme sayfasında
    -- SUCCEEDED / FAILED: sağlayıcı sonucu | REFUND_REQUIRED: ödeme alındı ama rezervasyon onaylanamadı (§4.8)
    -- REFUNDED: iade tamamlandı
    status                    VARCHAR(20)   NOT NULL,
    -- Tutar booking'den alınır, istemci gönderemez (§4.8)
    amount                    DECIMAL(10,2) NOT NULL,
    currency                  CHAR(3)       NOT NULL,
    -- Sağlayıcıya gönderilen, sonucu kendi kaydımızla eşlemek için kullanılan rastgele kimlik
    conversation_id           VARCHAR(64)   NOT NULL,
    -- Ödeme sayfası token'ı: callback'te ödemeyi bulur (iyzico ömrü 30 dk)
    provider_token            VARCHAR(255)  NULL,
    provider_token_expires_at DATETIME(3)   NULL,
    -- Sağlayıcının ödeme numarası; iade bununla yapılır
    provider_payment_id       VARCHAR(64)   NULL,
    -- booking confirm/fail çağrısının durumu: başarısızsa mutabakat job'ı yeniden dener (§4.8)
    booking_sync_status       VARCHAR(20)   NOT NULL,
    booking_sync_attempts     INT           NOT NULL DEFAULT 0,
    next_retry_at             DATETIME(3)   NULL,
    failure_code              VARCHAR(50)   NULL,
    created_at                DATETIME(3)   NOT NULL,
    updated_at                DATETIME(3)   NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_reservation UNIQUE (reservation_id),
    CONSTRAINT uk_payments_conversation UNIQUE (conversation_id),
    CONSTRAINT uk_payments_provider_token UNIQUE (provider_token),
    CONSTRAINT uk_payments_provider_payment UNIQUE (provider_payment_id),
    CONSTRAINT ck_payments_provider CHECK (provider IN ('IYZICO', 'MOCK')),
    CONSTRAINT ck_payments_status
        CHECK (status IN ('INITIATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'REFUND_REQUIRED', 'REFUNDED')),
    CONSTRAINT ck_payments_sync CHECK (booking_sync_status IN ('NOT_NEEDED', 'PENDING', 'DONE', 'FAILED')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- "Rezervasyonlarım" ve sahiplik kontrolü
CREATE INDEX ix_payments_user ON payments (user_id);
-- Mutabakat job'ı: sonucu bekleyen ödemeler ve yeniden denenecek booking senkronları
CREATE INDEX ix_payments_status_expires ON payments (status, provider_token_expires_at);
CREATE INDEX ix_payments_sync_retry ON payments (booking_sync_status, next_retry_at);

-- Sağlayıcıdan gelen ham kayıtlar (denetim). Kart verisi tutulmaz; ham gövde kaydedilmeden önce hassas alanlar maskelenir.
-- 90 gün sonra temizlik job'ı siler (K43).
CREATE TABLE payment_callback_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    -- Bilinmeyen token'la gelen callback da kaydedilir (saldırı izi); o zaman ödeme yoktur
    payment_id      BIGINT       NULL,
    provider_token  VARCHAR(255) NULL,
    event_source    VARCHAR(20)  NOT NULL,
    retrieve_status VARCHAR(30)  NULL,
    raw_payload     JSON         NULL,
    received_at     DATETIME(3)  NOT NULL,
    CONSTRAINT pk_payment_callback_logs PRIMARY KEY (id),
    CONSTRAINT fk_payment_callback_logs_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT ck_payment_callback_logs_source CHECK (event_source IN ('CALLBACK', 'RETRIEVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_payment_callback_logs_payment ON payment_callback_logs (payment_id);
CREATE INDEX ix_payment_callback_logs_received ON payment_callback_logs (received_at);
