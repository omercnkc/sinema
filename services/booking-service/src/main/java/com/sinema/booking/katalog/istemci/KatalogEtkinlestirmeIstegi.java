package com.sinema.booking.katalog.istemci;

import java.util.List;

/** Göçün son adımı: booking'in biletlerini taşıdığı seanslar (catalog hâlâ başlamamış olduklarını doğrular). */
public record KatalogEtkinlestirmeIstegi(List<Long> showtimeIds) {
}
