-- Film ve liste alanları (db-planlama.md §3.2, K16, K18-K22, K43, K45, K46).
-- Film silinmez, gizlenir (K40): id'si seanslarda ve bilet snapshot'larında yaşar.
CREATE TABLE movies (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    tmdb_id         BIGINT        NULL,
    title           VARCHAR(255)  NOT NULL,
    overview        TEXT          NULL,
    poster_path     VARCHAR(255)  NULL,
    runtime_minutes INT           NOT NULL,
    release_date    DATE          NULL,
    -- TR yaş sınırı; en uzunu "Genel İzleyici Kitlesi" (db-planlama'daki 10 karakter yetmiyordu)
    certification   VARCHAR(30)   NULL,
    vote_average    DECIMAL(3,1)  NULL,
    director        VARCHAR(255)  NULL,
    visible         BOOLEAN       NOT NULL DEFAULT TRUE,
    -- TMDB'den en son ne zaman çekildi (K45: 3 aydan eskiyse yenilenir); elle girilen filmde NULL
    tmdb_synced_at  DATETIME(3)   NULL,
    created_at      DATETIME(3)   NOT NULL,
    updated_at      DATETIME(3)   NOT NULL,
    CONSTRAINT pk_movies PRIMARY KEY (id),
    -- NULL = elle girilmiş; birden çok NULL UNIQUE'i bozmaz
    CONSTRAINT uq_movies_tmdb_id UNIQUE (tmdb_id),
    CONSTRAINT ck_movies_runtime CHECK (runtime_minutes > 0),
    CONSTRAINT ck_movies_vote CHECK (vote_average IS NULL OR (vote_average >= 0 AND vote_average <= 10))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Halka açık liste: görünür filmler, yeniden eskiye
CREATE INDEX ix_movies_visible_release ON movies (visible, release_date, id);

CREATE TABLE movie_genres (
    movie_id BIGINT      NOT NULL,
    genre    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_movie_genres PRIMARY KEY (movie_id, genre),
    CONSTRAINT fk_movie_genres_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    -- Türe göre filtre için (genre, movie_id) sırası
    INDEX ix_movie_genres_genre (genre, movie_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- position: oyuncunun TMDB'deki sırası (başrol önce)
CREATE TABLE movie_cast_members (
    movie_id BIGINT       NOT NULL,
    position INT          NOT NULL,
    name     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_movie_cast_members PRIMARY KEY (movie_id, position),
    CONSTRAINT fk_movie_cast_members_movie FOREIGN KEY (movie_id) REFERENCES movies (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE movie_content_descriptors (
    movie_id   BIGINT      NOT NULL,
    descriptor VARCHAR(30) NOT NULL,
    CONSTRAINT pk_movie_content_descriptors PRIMARY KEY (movie_id, descriptor),
    CONSTRAINT fk_movie_content_descriptors_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT ck_movie_content_descriptors CHECK (descriptor IN ('SIDDET_KORKU', 'CINSELLIK', 'OLUMSUZ_ORNEK'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
