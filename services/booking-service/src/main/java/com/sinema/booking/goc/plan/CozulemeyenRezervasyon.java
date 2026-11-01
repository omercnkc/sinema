package com.sinema.booking.goc.plan;

import java.util.List;

/** Yerinden çıkan koltukları için bitişik ve uygun boş yer bulunamayan rezervasyon (K35: göç reddedilir). */
public record CozulemeyenRezervasyon(long rezervasyonId, List<String> eskiEtiketler) {

    public CozulemeyenRezervasyon {
        eskiEtiketler = List.copyOf(eskiEtiketler);
    }
}
