package com.sinema.common.guvenlik;

import org.springframework.http.HttpMethod;

/** "GET /api/v1/catalog/**" biçimindeki ayar satırının çözülmüş hali; metot null ise her metot. */
record AcikYol(HttpMethod metot, String desen) {

    static AcikYol coz(String satir) {
        String[] parcalar = satir.trim().split("\\s+");
        return switch (parcalar.length) {
            case 1 -> new AcikYol(null, parcalar[0]);
            case 2 -> new AcikYol(HttpMethod.valueOf(parcalar[0].toUpperCase()), parcalar[1]);
            default -> throw new IllegalArgumentException(
                    "Geçersiz açık yol: '" + satir + "' (beklenen: 'METOT /desen' veya '/desen')");
        };
    }
}
