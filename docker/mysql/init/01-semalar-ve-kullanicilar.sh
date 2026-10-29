#!/bin/bash
# MySQL container'ı İLK açıldığında (veri klasörü boşken) bir kez çalışır.
# Her servis için ayrı şema ve yalnızca o şemaya yetkili ayrı kullanıcı oluşturur (planlama §6.3).
# Parolalar SQL dosyasına yazılmaz, .env'den ortam değişkeni olarak gelir.
set -euo pipefail

sema_ve_kullanici_olustur() {
    local sema="$1" kullanici="$2" parola="$3"
    mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS \`$sema\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$kullanici'@'%' IDENTIFIED BY '$parola';
GRANT ALL PRIVILEGES ON \`$sema\`.* TO '$kullanici'@'%';
SQL
    echo "[init] $sema şeması ve $kullanici kullanıcısı hazır"
}

sema_ve_kullanici_olustur auth_db    auth_user    "$AUTH_DB_PASSWORD"
sema_ve_kullanici_olustur catalog_db catalog_user "$CATALOG_DB_PASSWORD"
sema_ve_kullanici_olustur booking_db booking_user "$BOOKING_DB_PASSWORD"
sema_ve_kullanici_olustur payment_db payment_user "$PAYMENT_DB_PASSWORD"

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" -e "FLUSH PRIVILEGES;"
