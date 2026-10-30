#!/usr/bin/env bash
# Çalışan yığına gateway üzerinden, tarayıcının yapacağı gibi uçtan uca duman testi.
# Her milestone sonunda ve büyük değişikliklerden sonra çalıştırılır; bir adım bile beklenmedik sonuç verirse çıkış kodu 1.
#
# Kullanım (repo kökünden, Windows'ta Git Bash):
#   ./scripts/duman-testi.sh                          # http://localhost:8080
#   ./scripts/duman-testi.sh http://localhost:8090    # başka gateway adresi
#   ORIGIN=http://localhost:4200 ./scripts/duman-testi.sh
#   ADMIN_EMAIL=... ADMIN_PASSWORD=... ./scripts/duman-testi.sh   # admin katalog adımları da (veri oluşturmaz)
# Koltuk tutma adımları yeni kullanıcıyla tutup bırakır; kalıcı veri bırakmaz.
set -uo pipefail

GATEWAY="${1:-http://localhost:8080}"
ORIGIN="${ORIGIN:-http://localhost:4200}"
EMAIL="duman-$(date +%s)-$RANDOM@ornek.com"
PAROLA="DumanTesti123"
CEREZ_DOSYASI="$(mktemp)"
HATA_SAYISI=0
trap 'rm -f "$CEREZ_DOSYASI"' EXIT

# adim <açıklama> <beklenen HTTP kodu> <gerçek HTTP kodu>
adim() {
    if [[ "$3" == "$2" ]]; then
        printf '  ✔ %-55s %s\n' "$1" "$3"
    else
        printf '  ✘ %-55s beklenen %s, gelen %s\n' "$1" "$2" "$3"
        HATA_SAYISI=$((HATA_SAYISI + 1))
    fi
}

# kod <curl argümanları...> → yalnızca HTTP durum kodu
kod() {
    curl -s -o /dev/null -w '%{http_code}' "$@"
}

json_alan() {
    sed -E "s/.*\"$1\":\"([^\"]+)\".*/\1/"
}

echo "Duman testi: $GATEWAY (origin: $ORIGIN)"

echo "1. Altyapı"
adim "gateway canlı (liveness)" 200 "$(kod "$GATEWAY/actuator/health/liveness")"
adim "iç uçlar dışarıya kapalı (/internal)" 401 "$(kod -X POST "$GATEWAY/internal/v1/auth/service-token")"
adim "korumalı uç token'sız reddedilir" 401 "$(kod "$GATEWAY/api/v1/auth/me")"

echo "2. Kayıt ve giriş"
adim "kayıt" 201 "$(kod -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PAROLA\"}" "$GATEWAY/api/v1/auth/register")"
adim "aynı e-postayla tekrar kayıt" 409 "$(kod -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PAROLA\"}" "$GATEWAY/api/v1/auth/register")"
adim "yanlış parola" 401 "$(kod -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"Yanlis12345\"}" "$GATEWAY/api/v1/auth/login")"

GIRIS=$(curl -s -c "$CEREZ_DOSYASI" -H 'Content-Type: application/json' -w '\n%{http_code}' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PAROLA\"}" "$GATEWAY/api/v1/auth/login")
adim "giriş" 200 "$(tail -n1 <<<"$GIRIS")"
TOKEN=$(head -n1 <<<"$GIRIS" | json_alan accessToken)

echo "3. Oturum"
adim "/me (access token ile)" 200 "$(kod -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/v1/auth/me")"
adim "USER admin ucuna giremez" 403 "$(kod -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{}' "$GATEWAY/api/v1/admin/auth/admins")"
adim "refresh Origin olmadan reddedilir (CSRF)" 403 "$(kod -b "$CEREZ_DOSYASI" -X POST "$GATEWAY/api/v1/auth/refresh")"
adim "refresh (cookie + izinli Origin)" 200 "$(kod -b "$CEREZ_DOSYASI" -c "$CEREZ_DOSYASI" -H "Origin: $ORIGIN" \
    -X POST "$GATEWAY/api/v1/auth/refresh")"
adim "çıkış" 204 "$(kod -b "$CEREZ_DOSYASI" -H "Origin: $ORIGIN" -X POST "$GATEWAY/api/v1/auth/logout")"
adim "çıkıştan sonra refresh reddedilir" 401 "$(kod -b "$CEREZ_DOSYASI" -H "Origin: $ORIGIN" \
    -X POST "$GATEWAY/api/v1/auth/refresh")"

echo "4. CORS"
adim "izinli origin'den preflight" 200 "$(kod -X OPTIONS -H "Origin: $ORIGIN" \
    -H 'Access-Control-Request-Method: POST' "$GATEWAY/api/v1/auth/login")"
adim "izinsiz origin'den preflight" 403 "$(kod -X OPTIONS -H 'Origin: https://kotu-site.example' \
    -H 'Access-Control-Request-Method: POST' "$GATEWAY/api/v1/auth/login")"

echo "5. Katalog (misafir)"
adim "şube listesi" 200 "$(kod "$GATEWAY/api/v1/catalog/cinemas")"
FILMLER=$(curl -s "$GATEWAY/api/v1/catalog/movies?size=1")
adim "film listesi (sayfalı)" 200 "$(kod "$GATEWAY/api/v1/catalog/movies?size=1")"
adim "sayfa boyutu sınırı (51)" 400 "$(kod "$GATEWAY/api/v1/catalog/movies?size=51")"
adim "olmayan film" 404 "$(kod "$GATEWAY/api/v1/catalog/movies/999999999")"
adim "katalog iç ucu dışarıya kapalı" 401 "$(kod "$GATEWAY/internal/v1/catalog/showtimes/1")"
adim "katalog admin ucu token'sız" 401 "$(kod "$GATEWAY/api/v1/admin/catalog/cinemas")"
# Çıkış yapılsa da access token süresi dolana kadar (≤15 dk) geçerlidir (planlama §5.3); rol kontrolü için yeterli
adim "katalog admin ucu USER ile" 403 "$(kod -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/v1/admin/catalog/cinemas")"

# Örnek veri (dev profili) yüklüyse bir filmin yarınki seansı üzerinden harita akışı denenir
FILM_ID=$(sed -nE 's/.*"content":\[\{"id":([0-9]+).*/\1/p' <<<"$FILMLER")
YARIN=$(date -d tomorrow +%F)
SEANS_ID=""
if [[ -n "$FILM_ID" ]]; then
    SEANS_ID=$(curl -s "$GATEWAY/api/v1/catalog/movies/$FILM_ID/showtimes?date=$YARIN" \
        | sed -nE 's/^\[\{"id":([0-9]+).*/\1/p')
fi
if [[ -n "$SEANS_ID" ]]; then
    adim "film detayı" 200 "$(kod "$GATEWAY/api/v1/catalog/movies/$FILM_ID")"
    adim "seans detayı" 200 "$(kod "$GATEWAY/api/v1/catalog/showtimes/$SEANS_ID")"
    ETIKET=$(curl -s -D - -o /dev/null "$GATEWAY/api/v1/catalog/showtimes/$SEANS_ID/layout" \
        | sed -nE 's/^[Ee][Tt]ag: *(.*)\r?$/\1/p' | tr -d '\r')
    adim "koltuk haritası (ETag ile)" 200 "$(kod "$GATEWAY/api/v1/catalog/showtimes/$SEANS_ID/layout")"
    adim "koltuk haritası değişmediyse 304" 304 "$(kod -H "If-None-Match: $ETIKET" \
        "$GATEWAY/api/v1/catalog/showtimes/$SEANS_ID/layout")"
else
    echo "  - yarın için seans yok (örnek veri yüklü değil): harita adımları atlandı"
fi

echo "6. Rezervasyon"
adim "koltuk tutma token'sız" 401 "$(kod -H 'Content-Type: application/json' \
    -d '{"showtimeId":1,"seatCellIds":[1]}' "$GATEWAY/api/v1/bookings/reservations")"
adim "rezervasyon iç ucu dışarıya kapalı" 401 "$(kod "$GATEWAY/internal/v1/bookings/reservations/1/payment-info")"
if [[ -n "$SEANS_ID" ]]; then
    DOLULUK="$GATEWAY/api/v1/bookings/showtimes/$SEANS_ID/seat-status"
    adim "koltuk doluluğu (misafir)" 200 "$(kod "$DOLULUK")"
    # Haritanın son koltuğu: örnek veride en arka sıra, elle yapılan denemelerle çakışma olasılığı düşük
    HUCRE=$(curl -s "$GATEWAY/api/v1/catalog/showtimes/$SEANS_ID/layout" \
        | grep -oE '"cellId":[0-9]+,"row":[0-9]+,"col":[0-9]+,"type":"SEAT"' | tail -n1 \
        | sed -E 's/"cellId":([0-9]+).*/\1/')
    TUTMA_GOVDESI="{\"showtimeId\":$SEANS_ID,\"seatCellIds\":[$HUCRE]}"
    TUTMA=$(curl -s -w '\n%{http_code}' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -d "$TUTMA_GOVDESI" "$GATEWAY/api/v1/bookings/reservations")
    adim "koltuk tutma (hold)" 201 "$(tail -n1 <<<"$TUTMA")"
    REZERVASYON_ID=$(head -n1 <<<"$TUTMA" | sed -nE 's/.*"reservationId":([0-9]+).*/\1/p')
    adim "tutulan koltuk doluluğa yansır" 1 "$(curl -s "$DOLULUK" | grep -c "\"cellId\":$HUCRE,")"
    adim "aynı seansta ikinci aktif işlem" 409 "$(kod -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' -d "$TUTMA_GOVDESI" "$GATEWAY/api/v1/bookings/reservations")"
    adim "rezervasyon detayı" 200 "$(kod -H "Authorization: Bearer $TOKEN" \
        "$GATEWAY/api/v1/bookings/reservations/$REZERVASYON_ID")"
    adim "rezervasyonlarım (sayfalı)" 200 "$(kod -H "Authorization: Bearer $TOKEN" \
        "$GATEWAY/api/v1/bookings/reservations?size=5")"
    adim "hold'u bırakma" 204 "$(kod -X DELETE -H "Authorization: Bearer $TOKEN" \
        "$GATEWAY/api/v1/bookings/reservations/$REZERVASYON_ID")"
    adim "bırakılan koltuk hemen boşalır" 0 "$(curl -s "$DOLULUK" | grep -c "\"cellId\":$HUCRE,")"
else
    echo "  - seans yok: koltuk tutma adımları atlandı"
fi

# Admin adımları isteğe bağlı ve veri oluşturmaz (her çalıştırmada örnek veriyi kirletmesin)
if [[ -n "${ADMIN_EMAIL:-}" && -n "${ADMIN_PASSWORD:-}" ]]; then
    echo "7. Katalog (admin)"
    ADMIN_TOKEN=$(curl -s -H 'Content-Type: application/json' \
        -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" "$GATEWAY/api/v1/auth/login" \
        | json_alan accessToken)
    adim "admin şube listesi" 200 "$(kod -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$GATEWAY/api/v1/admin/catalog/cinemas")"
    adim "admin film listesi (gizliler dahil)" 200 "$(kod -H "Authorization: Bearer $ADMIN_TOKEN" \
        "$GATEWAY/api/v1/admin/catalog/movies")"
    adim "geçersiz şube isteği doğrulanır" 400 "$(kod -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H 'Content-Type: application/json' -d '{}' "$GATEWAY/api/v1/admin/catalog/cinemas")"
else
    echo "  - ADMIN_EMAIL/ADMIN_PASSWORD verilmedi: admin katalog adımları atlandı"
fi

if [[ $HATA_SAYISI -gt 0 ]]; then
    echo "SONUÇ: $HATA_SAYISI adım başarısız"
    exit 1
fi
echo "SONUÇ: tüm adımlar başarılı"
