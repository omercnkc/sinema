package com.sinema.booking.goc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.booking.goc.entity.GocNedeni;
import com.sinema.booking.goc.entity.KoltukGocKaydi;
import com.sinema.booking.goc.plan.KoltukTasima;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class KoltukDegisikligiMesajiTest {

    private static KoltukGocKaydi kayit(String eski, String yeni, GocNedeni neden) {
        return KoltukGocKaydi.olustur(1L, 2L, new KoltukTasima(3, 10, eski, 20, yeni, neden), eski, Instant.now());
    }

    @Test
    void shouldListChangedSeatsSortedByOldLabel_andSayNewPlace_whenLabelUnchanged() {
        String mesaj = KoltukDegisikligiMesaji.olustur("Son Tren", "Kadıköy", "Salon 1", List.of(
                kayit("F-8", "E-8", GocNedeni.LABEL_CHANGED),
                kayit("A-1", "A-1", GocNedeni.REPLACEMENT),
                kayit("F-7", "E-7", GocNedeni.LABEL_CHANGED)));

        assertThat(mesaj).isEqualTo("Salon düzeni değişti: Son Tren (Kadıköy, Salon 1) biletinizin koltuğu güncellendi: "
                + "A-1 (yeni yer), F-7 → E-7, F-8 → E-8.");
    }
}
