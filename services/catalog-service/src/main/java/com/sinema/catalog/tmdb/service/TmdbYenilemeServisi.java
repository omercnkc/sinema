package com.sinema.catalog.tmdb.service;

import com.sinema.catalog.config.TmdbAyarlari;
import com.sinema.catalog.film.entity.FilmBilgileri;
import com.sinema.catalog.film.repository.FilmRepository;
import com.sinema.catalog.tmdb.istemci.TmdbIstemcisi;
import com.sinema.catalog.tmdb.mapper.TmdbMapper;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * K45: TMDB şartları verinin 6 aydan uzun saklanmasını yasaklar; biz 3 ayda bir yenileriz.
 * Her film kendi kısa transaction'ında güncellenir: TMDB çağrısı transaction dışında, bir filmin hatası
 * diğerlerini geri almaz. TMDB'ye hiç ulaşılamıyorsa çalışma durur, bir sonraki zamanlamada tekrar denenir
 * (doğruluk tek bir çalışmaya bağlı değil: süresi geçen film her çalışmada yeniden seçilir).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbYenilemeServisi {

    private final TmdbIstemcisi tmdbIstemcisi;
    private final TmdbDonusturucu donusturucu;
    private final TmdbMapper tmdbMapper;
    private final FilmRepository filmRepository;
    private final TmdbAyarlari ayarlar;
    private final TransactionTemplate transactionTemplate;
    private final Clock saat;

    @Scheduled(cron = "${sinema.tmdb.yenileme-zamanlamasi}")
    public void zamaniGelinceYenile() {
        int yenilenen = suresiGecenleriYenile();
        log.info("TMDB tazelik yenilemesi bitti: {} film yenilendi", yenilenen);
    }

    /** @return yenilenen film sayısı */
    public int suresiGecenleriYenile() {
        if (!ayarlar.tokenVar()) {
            log.warn("TMDB token'ı yok; tazelik yenilemesi atlandı (K45 için token gerekli)");
            return 0;
        }
        Instant esik = saat.instant().minus(ayarlar.tazelikSuresi());
        List<Long> filmIdleri = filmRepository.yenilenecekFilmIdleri(esik, PageRequest.of(0, ayarlar.yenilemePaketi()));
        int yenilenen = 0;
        for (Long filmId : filmIdleri) {
            try {
                filmiYenile(filmId);
                yenilenen++;
            } catch (BagimlilikKullanilamazIstisnasi e) {
                log.warn("TMDB'ye ulaşılamıyor; yenileme sonraki çalışmaya bırakıldı ({} film kaldı)",
                        filmIdleri.size() - yenilenen);
                break;
            } catch (KaynakBulunamadiIstisnasi e) {
                // TMDB filmi kaldırmış; mevcut veri kalır, admin kararını verir. Her çalışmada tekrar uyarılır.
                log.warn("Film {} TMDB'de artık yok; yenilenemedi", filmId);
            }
        }
        return yenilenen;
    }

    private void filmiYenile(Long filmId) {
        Long tmdbId = filmRepository.findById(filmId).orElseThrow().getTmdbId();
        FilmBilgileri tmdbBilgileri = tmdbMapper.filmBilgileri(donusturucu.onizleme(tmdbIstemcisi.filmGetir(tmdbId)));
        Instant cekimZamani = saat.instant();
        transactionTemplate.executeWithoutResult(durum -> filmRepository.findById(filmId)
                .ifPresent(film -> film.tmdbIleYenile(tmdbBilgileri, cekimZamani)));
    }
}
