-- Kişi bilgileri (K48): herkese açık kayıtta zorunlu, iyzico alıcı bilgisi olarak kullanılır. Bilette, JWT'de ve
-- loglarda yer almaz. Eski hesaplarda (ilk admin, mevcut kullanıcılar) ve admin ekleme ucuyla açılan hesaplarda boştur.
ALTER TABLE users
    ADD COLUMN first_name VARCHAR(50) NULL AFTER email,
    ADD COLUMN last_name  VARCHAR(50) NULL AFTER first_name,
    -- Her zaman +905XXXXXXXXX biçiminde saklanır (TR cep telefonu)
    ADD COLUMN phone      VARCHAR(13) NULL AFTER last_name;
