-- Sürümlü salon düzeni ve hücreleri (db-planlama.md §3.2, K5, K17).
-- Taslak (DRAFT) düzenlenebilir; yayımlanınca (PUBLISHED) sürüm numarası alır ve bir daha değişmez.
CREATE TABLE hall_layouts (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    hall_id      BIGINT      NOT NULL,
    version      INT         NULL,
    status       VARCHAR(20) NOT NULL,
    grid_rows    INT         NOT NULL,
    grid_cols    INT         NOT NULL,
    created_at   DATETIME(3) NOT NULL,
    updated_at   DATETIME(3) NOT NULL,
    published_at DATETIME(3) NULL,
    -- Salon başına en çok bir taslak: taslakken hall_id, yayımlanınca NULL (NULL'lar UNIQUE'i bozmaz).
    -- Uygulamadaki kontrol kullanıcıya anlaşılır hata verir; bu kolon yarışta bile ikinci taslağı engeller.
    draft_hall_key BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'DRAFT' THEN hall_id END) VIRTUAL,
    CONSTRAINT pk_hall_layouts PRIMARY KEY (id),
    CONSTRAINT fk_hall_layouts_hall FOREIGN KEY (hall_id) REFERENCES halls (id),
    CONSTRAINT uq_hall_layouts_hall_version UNIQUE (hall_id, version),
    CONSTRAINT uq_hall_layouts_draft UNIQUE (draft_hall_key),
    CONSTRAINT ck_hall_layouts_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_hall_layouts_grid CHECK (grid_rows > 0 AND grid_cols > 0),
    -- Sürüm ve yayım anı yalnızca yayımlanmış düzende olur
    CONSTRAINT ck_hall_layouts_publish CHECK (
        (status = 'DRAFT' AND version IS NULL AND published_at IS NULL)
        OR (status = 'PUBLISHED' AND version IS NOT NULL AND published_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Yalnızca boş olmayan hücreler saklanır; satır yoksa o konum boşluk/koridordur.
-- Koltuk kimliği = layout_cells.id (booking bunu layout_cell_id olarak kullanır).
CREATE TABLE layout_cells (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    layout_id      BIGINT      NOT NULL,
    row_index      INT         NOT NULL,
    col_index      INT         NOT NULL,
    cell_type      VARCHAR(20) NOT NULL,
    seat_row_label VARCHAR(5)  NULL,
    seat_number    INT         NULL,
    seat_type      VARCHAR(20) NULL,
    CONSTRAINT pk_layout_cells PRIMARY KEY (id),
    CONSTRAINT fk_layout_cells_layout FOREIGN KEY (layout_id) REFERENCES hall_layouts (id),
    CONSTRAINT uq_layout_cells_position UNIQUE (layout_id, row_index, col_index),
    -- Aynı düzende iki koltuk aynı etiketi (ör. F-7) taşıyamaz; koltuk olmayan hücrelerde etiket NULL
    CONSTRAINT uq_layout_cells_seat_label UNIQUE (layout_id, seat_row_label, seat_number),
    CONSTRAINT ck_layout_cells_type CHECK (cell_type IN ('SEAT', 'ENTRANCE', 'EXIT', 'SCREEN', 'WALL')),
    CONSTRAINT ck_layout_cells_seat_type CHECK (seat_type IS NULL OR seat_type IN ('STANDARD', 'ACCESSIBLE')),
    -- Koltuk bilgileri yalnızca koltukta ve koltukta eksiksiz olur
    CONSTRAINT ck_layout_cells_seat_fields CHECK (
        (cell_type = 'SEAT' AND seat_row_label IS NOT NULL AND seat_number IS NOT NULL AND seat_type IS NOT NULL)
        OR (cell_type <> 'SEAT' AND seat_row_label IS NULL AND seat_number IS NULL AND seat_type IS NULL)),
    CONSTRAINT ck_layout_cells_position CHECK (row_index >= 0 AND col_index >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- V2'de ertelenen FK: salon yalnızca var olan bir düzene işaret edebilir
ALTER TABLE halls
    ADD CONSTRAINT fk_halls_active_layout FOREIGN KEY (active_layout_id) REFERENCES hall_layouts (id);
