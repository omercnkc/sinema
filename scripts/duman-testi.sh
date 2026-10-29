#!/usr/bin/env bash
# Çalışan yığına gateway üzerinden, tarayıcının yapacağı gibi uçtan uca duman testi.
# Her milestone sonunda ve büyük değişikliklerden sonra çalıştırılır; bir adım bile beklenmedik sonuç verirse çıkış kodu 1.
#
# Kullanım (repo kökünden, Windows'ta Git Bash):
#   ./scripts/duman-testi.sh                          # http://localhost:8080
#   ./scripts/duman-testi.sh http://localhost:8090    # başka gateway adresi
#   ORIGIN=http://localhost:4200 ./scripts/duman-testi.sh
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

if [[ $HATA_SAYISI -gt 0 ]]; then
    echo "SONUÇ: $HATA_SAYISI adım başarısız"
    exit 1
fi
echo "SONUÇ: tüm adımlar başarılı"
