package com.sinema.catalog.ornekveri;

import com.sinema.catalog.config.KatalogAyarlari;
import com.sinema.catalog.duzen.service.DuzenServisi;
import com.sinema.catalog.film.service.FilmServisi;
import com.sinema.catalog.ornekveri.OrnekVeriler.OrnekSalon;
import com.sinema.catalog.ornekveri.OrnekVeriler.OrnekSube;
import com.sinema.catalog.seans.dto.SeansOlusturmaIstegi;
import com.sinema.catalog.seans.service.SeansServisi;
import com.sinema.catalog.sube.dto.SalonOlusturmaIstegi;
import com.sinema.catalog.sube.dto.SubeIstegi;
import com.sinema.catalog.sube.repository.SubeRepository;
import com.sinema.catalog.sube.service.SubeServisi;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Yalnızca "dev" profilinde ve katalog boşken örnek veri yükler (K42: migration değil, seed). Veriler admin
 * API'sinin kullandığı servislerden geçer: örnek veri de aynı kurallara (çakışma, düzen doğrulaması) tabidir.
 * Hepsi tek transaction'da: yarıda kalırsa hiçbiri yazılmaz, bir sonraki açılışta baştan denenir.
 * Seanslar bugünden itibaren GUN_SAYISI gün için; süre dolunca `docker compose down -v` ile sıfırlanır.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class OrnekVeriYukleyici implements ApplicationRunner {

    private static final int GUN_SAYISI = 7;
    private static final LocalTime ILK_SEANS = LocalTime.of(11, 0);
    private static final LocalTime SON_SEANS_EN_GEC = LocalTime.of(23, 0);
    private static final int SEANS_YUVARLAMA_DAKIKASI = 15;

    private final SubeRepository subeRepository;
    private final SubeServisi subeServisi;
    private final DuzenServisi duzenServisi;
    private final FilmServisi filmServisi;
    private final SeansServisi seansServisi;
    private final KatalogAyarlari ayarlar;
    private final TransactionTemplate transactionTemplate;
    private final Clock saat;

    @Override
    public void run(ApplicationArguments argumanlar) {
        if (subeRepository.count() > 0) {
            log.info("Katalogda veri var; örnek veri yüklenmedi");
            return;
        }
        transactionTemplate.executeWithoutResult(durum -> yukle());
    }

    private void yukle() {
        List<Long> salonIdleri = new ArrayList<>();
        for (OrnekSube sube : OrnekVeriler.SUBELER) {
            Long subeId = subeServisi.subeOlustur(new SubeIstegi(sube.ad(), OrnekVeriler.SEHIR, sube.adres())).id();
            for (OrnekSalon salon : sube.salonlar()) {
                salonIdleri.add(salonKur(subeId, salon));
            }
        }
        List<EklenenFilm> filmler = OrnekVeriler.FILMLER.stream()
                .map(film -> new EklenenFilm(filmServisi.olustur(film).id(), film.runtimeMinutes()))
                .toList();
        int seansSayisi = seanslariPlanla(salonIdleri, filmler);
        log.info("Örnek veri yüklendi: {} şube, {} salon, {} film, {} seans", OrnekVeriler.SUBELER.size(),
                salonIdleri.size(), filmler.size(), seansSayisi);
    }

    private record EklenenFilm(Long id, int sureDakika) {
    }

    private Long salonKur(Long subeId, OrnekSalon salon) {
        Long salonId = subeServisi.salonOlustur(
                new SalonOlusturmaIstegi(subeId, salon.ad(), new BigDecimal(salon.fiyat()))).id();
        Long taslakId = duzenServisi.taslakOlustur(salonId, OrnekDuzenUretici.uret(salon.duzen())).id();
        duzenServisi.yayimla(salonId, taslakId);
        return salonId;
    }

    /**
     * Her salonda gün 11:00'de başlar; sonraki seans öncekinin bitişinin (film + ara payı) yuvarlandığı çeyrek
     * saatte başlar. Filmler salon ve güne göre kaydırılır: her salonda gün içinde farklı filmler oynar.
     * Bugünün geçmişte kalan seansları eklenmez ama takvim onlarla aynı ilerler.
     */
    private int seanslariPlanla(List<Long> salonIdleri, List<EklenenFilm> filmler) {
        Instant simdi = saat.instant();
        LocalDate bugun = LocalDate.ofInstant(simdi, ayarlar.isSaatDilimi());
        int seansSayisi = 0;
        for (int gun = 0; gun < GUN_SAYISI; gun++) {
            for (int salon = 0; salon < salonIdleri.size(); salon++) {
                ZonedDateTime baslangic = bugun.plusDays(gun).atTime(ILK_SEANS).atZone(ayarlar.isSaatDilimi());
                ZonedDateTime enGecBaslangic = baslangic.with(SON_SEANS_EN_GEC);
                for (int sira = 0; !baslangic.isAfter(enGecBaslangic); sira++) {
                    EklenenFilm film = filmler.get((salon + gun + sira) % filmler.size());
                    if (baslangic.toInstant().isAfter(simdi)) {
                        seansServisi.olustur(new SeansOlusturmaIstegi(film.id(), salonIdleri.get(salon),
                                baslangic.toInstant(), null, null));
                        seansSayisi++;
                    }
                    ZonedDateTime bitis = baslangic.plusMinutes(film.sureDakika()).plus(ayarlar.seansArasiPayi());
                    baslangic = ceyrekSaateYuvarla(bitis);
                }
            }
        }
        return seansSayisi;
    }

    /** Başlangıçlar hep tam dakikadır (11:00 + dakika cinsinden süreler); ör. 13:07 → 13:15, 13:15 → 13:15. */
    private static ZonedDateTime ceyrekSaateYuvarla(ZonedDateTime an) {
        int artan = an.getMinute() % SEANS_YUVARLAMA_DAKIKASI;
        ZonedDateTime tamDakika = an.truncatedTo(ChronoUnit.MINUTES);
        return artan == 0 ? tamDakika : tamDakika.plusMinutes(SEANS_YUVARLAMA_DAKIKASI - artan);
    }

}
