-- Rotasyonlu refresh token'lar (db-planlama.md §3.1, planlama §5.2). Token'ın kendisi değil yalnızca SHA-256 özeti saklanır:
-- veritabanı sızsa bile içindeki değerlerle oturum ele geçirilemez.
CREATE TABLE refresh_tokens (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    token_hash     CHAR(64)    NOT NULL,
    family_id      CHAR(36)    NOT NULL,
    expires_at     DATETIME(3) NOT NULL,
    revoked_at     DATETIME(3) NULL,
    replaced_by_id BIGINT      NULL,
    created_at     DATETIME(3) NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX ix_refresh_tokens_family (family_id),
    INDEX ix_refresh_tokens_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
