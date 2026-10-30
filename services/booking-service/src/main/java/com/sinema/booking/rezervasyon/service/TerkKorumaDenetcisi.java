package com.sinema.booking.rezervasyon.service;

import com.sinema.booking.config.RezervasyonAyarlari;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import com.sinema.common.hata.CokFazlaIstekIstisnasi;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * K29 terk koruması (planlama §4.7): son 30 dk'da 3 hold ödenmeden bırakıldıysa ya da süresi dolduysa, son terkten
 * 10 dk sonrasına kadar yeni hold açılamaz (429 + Retry-After). Koltukları tutup bırakarak sabotajı zorlaştırır.
 *
 * Ek tablo veya sayaç yok: durum her istekte rezervasyon geçmişinden türetilir. Böylece süre dolumu job'ı
 * gecikse bile (süresi geçmiş PENDING_PAYMENT de sayılır) ve servis yeniden başlasa bile doğru çalışır.
 * Gateway'deki IP hız sınırından ayrıdır; bu kullanıcı bazlı bir iş kuralıdır (§3.4-3).
 */
@Component
@RequiredArgsConstructor
public class TerkKorumaDenetcisi {

    public static final String BEKLEME_KODU = "HOLD_COOLDOWN";

    private final RezervasyonRepository rezervasyonRepository;
    private final RezervasyonAyarlari ayarlar;

    @Transactional(readOnly = true)
    public void denetle(Long kullaniciId, Instant simdi) {
        RezervasyonAyarlari.TerkKorumasi kural = ayarlar.terkKorumasi();
        List<Instant> terkler = rezervasyonRepository.terkZamanlari(kullaniciId, simdi.minus(kural.pencere()), simdi);
        if (terkler.size() < kural.esik()) {
            return;
        }
        Instant serbestKalma = Collections.max(terkler).plus(kural.bekleme());
        if (serbestKalma.isAfter(simdi)) {
            throw new CokFazlaIstekIstisnasi(BEKLEME_KODU,
                    "Son yarım saatte birkaç işlemi ödeme yapmadan bıraktınız; lütfen biraz bekleyip tekrar deneyin.",
                    Duration.between(simdi, serbestKalma));
        }
    }
}
