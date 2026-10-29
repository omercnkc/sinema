package com.sinema.common.hata;

/**
 * Doğrulama hatasında tek bir alanın sorunu. Bileşen adları API sözleşmesindeki
 * JSON alan adlarıdır (errors[{field, message}], planlama §7.2), bu yüzden İngilizce.
 */
public record AlanHatasi(String field, String message) {
}
