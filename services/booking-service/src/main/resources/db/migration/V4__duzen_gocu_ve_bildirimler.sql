-- K35/K51: salon düzeni değişince başlamamış seansların biletleri yeni sürüme taşınır. hall_id ve layout_id'ler
-- catalog'a, created_by ve user_id auth'a yumuşak referanstır (başka servisin tablosuna FK yok, db-planlama §5).

-- Göç işi. Önizleme kaydedilmez (uygulama planı yeniden hesaplar); kayıt uygulama başlayınca açılır.
CREATE TABLE layout_migrations (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    hall_id         BIGINT       NOT NULL,
    to_layout_id    BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_by      BIGINT       NOT NULL,
    failure_code    VARCHAR(50)  NULL,
    created_at      DATETIME(3)  NOT NULL,
    completed_at    DATETIME(3)  NULL,
    CONSTRAINT pk_layout_migrations PRIMARY KEY (id),
    CONSTRAINT ck_layout_migrations_status CHECK (status IN ('RUNNING', 'COMPLETED', 'ROLLED_BACK', 'FAILED'))
);

CREATE INDEX ix_layout_migrations_hall ON layout_migrations (hall_id, created_at);

-- Göçte yeri değişen her koltuğun denetim kaydı: SOLD satırın değişmezliğinin tek istisnası (db-planlama §6-5).
-- Telafi (catalog activate başarısızsa) eski hücre/etiketi buradan geri yazar.
CREATE TABLE seat_migration_audit (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    migration_id        BIGINT       NOT NULL,
    reservation_id      BIGINT       NOT NULL,
    showtime_id         BIGINT       NOT NULL,
    old_layout_cell_id  BIGINT       NOT NULL,
    new_layout_cell_id  BIGINT       NOT NULL,
    old_seat_label      VARCHAR(10)  NOT NULL,
    new_seat_label      VARCHAR(10)  NOT NULL,
    reason              VARCHAR(20)  NOT NULL,
    created_at          DATETIME(3)  NOT NULL,
    CONSTRAINT pk_seat_migration_audit PRIMARY KEY (id),
    CONSTRAINT fk_seat_migration_audit_migration FOREIGN KEY (migration_id) REFERENCES layout_migrations (id),
    CONSTRAINT fk_seat_migration_audit_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT ck_seat_migration_audit_reason CHECK (reason IN ('SAME_POSITION', 'LABEL_CHANGED', 'REPLACEMENT'))
);

CREATE INDEX ix_seat_migration_audit_migration ON seat_migration_audit (migration_id);
CREATE INDEX ix_seat_migration_audit_reservation ON seat_migration_audit (reservation_id);

-- Satır varken salonun seanslarında yeni koltuk tutulamaz (göç boyunca); PRIMARY KEY salon başına tek göç sağlar.
CREATE TABLE hall_maintenance_locks (
    hall_id       BIGINT       NOT NULL,
    migration_id  BIGINT       NOT NULL,
    locked_at     DATETIME(3)  NOT NULL,
    CONSTRAINT pk_hall_maintenance_locks PRIMARY KEY (hall_id),
    CONSTRAINT fk_hall_maintenance_locks_migration FOREIGN KEY (migration_id) REFERENCES layout_migrations (id)
);

-- Uygulama içi bildirim (K35: e-posta yok). Okunmamışlar kullanıcı bazında önce listelenir.
CREATE TABLE notifications (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    reservation_id  BIGINT        NULL,
    message         VARCHAR(500)  NOT NULL,
    read_at         DATETIME(3)   NULL,
    created_at      DATETIME(3)   NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT ck_notifications_type CHECK (type IN ('SEAT_CHANGED'))
);

CREATE INDEX ix_notifications_user_read ON notifications (user_id, read_at, created_at);
