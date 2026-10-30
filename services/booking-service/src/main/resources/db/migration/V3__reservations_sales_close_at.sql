-- Satış kapanışının kopyası (K10, K32): ödeme penceresi uzatması kapanışı aşamaz. Hold anında catalog'dan zaten
-- alınıyor; saklanınca uzatma için catalog'a tekrar gitmek gerekmez (fail fast, tek yönlü bağımlılık korunur).
ALTER TABLE reservations ADD COLUMN sales_close_at DATETIME(3) NULL AFTER starts_at;

-- Önceki satırlar: hold bitişi zaten kapanışı aşmıyordu, en güvenli alt sınır o
UPDATE reservations SET sales_close_at = expires_at WHERE sales_close_at IS NULL;

ALTER TABLE reservations MODIFY COLUMN sales_close_at DATETIME(3) NOT NULL;
