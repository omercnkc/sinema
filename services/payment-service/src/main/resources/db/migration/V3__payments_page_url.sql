-- Ödeme sayfası adresi: aynı rezervasyon için tekrar gelen "öde" isteği yeni ödeme açmadan aynı sayfaya döner (§7.6).
ALTER TABLE payments ADD COLUMN payment_page_url VARCHAR(500) NULL AFTER provider_token_expires_at;
