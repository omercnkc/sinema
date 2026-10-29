#!/usr/bin/env bash
# JWT (RS256) için RSA anahtar çifti üretir (planlama §5.1, K36).
#   keys/jwt-private.pem : PKCS#8 private key -> YALNIZCA auth-service'e mount edilir (token imzalar)
#   keys/jwt-public.pem  : X.509 public key   -> gateway ve diğer servisler (token doğrular)
# keys/ klasörü .gitignore'dadır; anahtarlar repoya girmez.
#
# Kullanım (repo kökünden, Linux/macOS veya Windows'ta Git Bash):
#   ./scripts/jwt-anahtar-uret.sh           # anahtar yoksa üretir
#   ./scripts/jwt-anahtar-uret.sh --zorla   # var olanın üzerine yazar (tüm oturumlar geçersiz olur!)
set -euo pipefail

ANAHTAR_BITI=3072
KLASOR="$(cd "$(dirname "$0")/.." && pwd)/keys"
OZEL_ANAHTAR="$KLASOR/jwt-private.pem"
ACIK_ANAHTAR="$KLASOR/jwt-public.pem"

if ! command -v openssl >/dev/null 2>&1; then
    echo "HATA: openssl bulunamadı. Windows'ta Git Bash ile çalıştırın." >&2
    exit 1
fi

if [[ -f "$OZEL_ANAHTAR" && "${1:-}" != "--zorla" ]]; then
    echo "Anahtar zaten var: $OZEL_ANAHTAR"
    echo "Yeniden üretmek için --zorla kullanın (verilmiş tüm token'lar geçersiz olur)."
    exit 0
fi

mkdir -p "$KLASOR"
# Private key önce kısıtlı izinle oluşsun; kısa bir an bile herkesçe okunabilir kalmasın
umask 077
openssl genpkey -algorithm RSA -pkeyopt "rsa_keygen_bits:$ANAHTAR_BITI" -out "$OZEL_ANAHTAR" 2>/dev/null
openssl pkey -in "$OZEL_ANAHTAR" -pubout -out "$ACIK_ANAHTAR"
# Container'daki root olmayan kullanıcı okuyabilsin diye public key herkese okunur olabilir
chmod 644 "$ACIK_ANAHTAR"

echo "Üretildi ($ANAHTAR_BITI bit RSA):"
echo "  $OZEL_ANAHTAR  (gizli, yalnızca auth-service)"
echo "  $ACIK_ANAHTAR   (doğrulama, diğer servisler)"
