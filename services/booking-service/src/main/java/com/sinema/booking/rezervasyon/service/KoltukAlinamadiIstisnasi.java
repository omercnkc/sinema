package com.sinema.booking.rezervasyon.service;

/**
 * Transaction içinde bir koltuk alınamadı: işlem geri alınsın diye fırlatılır (hepsi ya da hiçbiri, §4.3).
 * Kullanıcıya gitmez; KoltukTutmaServisi bunu 409 SEATS_UNAVAILABLE'a çevirir.
 */
class KoltukAlinamadiIstisnasi extends RuntimeException {

    private final transient Long hucreId;

    KoltukAlinamadiIstisnasi(Long hucreId) {
        super("Koltuk alınamadı: " + hucreId, null, false, false);
        this.hucreId = hucreId;
    }

    Long getHucreId() {
        return hucreId;
    }
}
