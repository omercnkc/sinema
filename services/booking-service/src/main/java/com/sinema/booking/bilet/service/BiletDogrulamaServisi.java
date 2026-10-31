package com.sinema.booking.bilet.service;

import com.sinema.booking.bilet.dto.BiletDogrulamaCevabi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import com.sinema.booking.rezervasyon.entity.RezervasyonKoltugu;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR bilet doğrulama (K30). Bilet görüntü verisi rezervasyondaki kopyadan gelir (§3.4-1): catalog'a gidilmez, film
 * adı sonradan değişse de bilet değişmez.
 *
 * Geçersiz biçim, bilinmeyen kod ve onaylanmamış rezervasyon aynı 404'ü alır: hangisi olduğu ayırt edilemez (K40),
 * kodlar deneme yoluyla aranamaz (ayrıca gateway'de bu uca özel hız sınırı var).
 */
@Service
@RequiredArgsConstructor
public class BiletDogrulamaServisi {

    public static final String GECERSIZ_BILET_KODU = "TICKET_NOT_FOUND";
    // Crockford Base32, 16 karakter (BiletKoduUretici); elle yazımda küçük harf de kabul edilir
    private static final Pattern GECERLI_BICIM = Pattern.compile("[0-9A-HJKMNP-TV-Z]{16}");

    private final RezervasyonRepository rezervasyonRepository;

    @Transactional(readOnly = true)
    public BiletDogrulamaCevabi dogrula(String girilenKod) {
        String kod = girilenKod.trim().toUpperCase(Locale.ROOT);
        if (!GECERLI_BICIM.matcher(kod).matches()) {
            throw gecersiz();
        }
        return rezervasyonRepository.findByBiletKodu(kod)
                .filter(rezervasyon -> rezervasyon.getDurum() == RezervasyonDurumu.CONFIRMED)
                .map(BiletDogrulamaServisi::cevap)
                .orElseThrow(BiletDogrulamaServisi::gecersiz);
    }

    private static BiletDogrulamaCevabi cevap(Rezervasyon rezervasyon) {
        return new BiletDogrulamaCevabi(true, rezervasyon.getFilmAdi(), rezervasyon.getSubeAdi(),
                rezervasyon.getSalonAdi(), rezervasyon.getSeansBaslangici(),
                rezervasyon.getKoltuklar().stream().map(RezervasyonKoltugu::getEtiket).toList());
    }

    private static KaynakBulunamadiIstisnasi gecersiz() {
        return new KaynakBulunamadiIstisnasi(GECERSIZ_BILET_KODU, "Geçersiz bilet.");
    }
}
