package com.sinema.catalog.tmdb.service;

import com.sinema.catalog.config.TmdbAyarlari;
import com.sinema.catalog.film.dto.FilmIstegi;
import com.sinema.catalog.film.entity.AkilliIsaret;
import com.sinema.catalog.tmdb.dto.TmdbFilmOnizlemesi;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.UlkeVizyonu;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.Vizyon;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * TMDB cevabını bizim film modelimize çevirir (K19-K22). Çıktı, FilmIstegi doğrulamasından geçecek
 * biçimdedir: geçersiz veya bilinmeyen değer kaydı engellemesin diye atlanır (null), uzun metin kesilir.
 * DB'ye ve ağa dokunmaz; birim testle tek başına sınanır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbDonusturucu {

    private static final Pattern GECERLI_YAS_SINIRI = Pattern.compile(FilmIstegi.TR_YAS_SINIRLARI);
    private static final int SINEMA_VIZYONU = 3;
    private static final int SINIRLI_SINEMA_VIZYONU = 2;
    private static final String YONETMEN_GOREVI = "Director";
    private static final int EN_COK_TUR = 10;
    private static final int TUR_UZUNLUGU = 50;
    private static final int METIN_UZUNLUGU = 255;
    private static final int OZET_UZUNLUGU = 5000;
    private static final int PUAN_BASAMAGI = 1;
    private static final BigDecimal EN_YUKSEK_PUAN = BigDecimal.TEN;

    private final TmdbAyarlari ayarlar;

    public TmdbFilmOnizlemesi onizleme(TmdbFilmCevabi film) {
        List<Vizyon> bolgeVizyonlari = bolgeVizyonlari(film);
        return new TmdbFilmOnizlemesi(
                film.id(),
                kes(film.title(), METIN_UZUNLUGU),
                kes(film.overview(), OZET_UZUNLUGU),
                film.posterPath(),
                film.runtime() == null || film.runtime() <= 0 ? null : film.runtime(),
                tarih(film.releaseDate()),
                yasSiniri(bolgeVizyonlari),
                puan(film.voteAverage()),
                yonetmen(film),
                turler(film),
                oyuncular(film),
                akilliIsaretler(film.id(), bolgeVizyonlari),
                null);
    }

    public static LocalDate tarih(String deger) {
        if (!StringUtils.hasText(deger)) {
            return null;
        }
        try {
            return LocalDate.parse(deger);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<Vizyon> bolgeVizyonlari(TmdbFilmCevabi film) {
        if (film.releaseDates() == null || film.releaseDates().results() == null) {
            return List.of();
        }
        return film.releaseDates().results().stream()
                .filter(ulke -> ayarlar.bolge().equalsIgnoreCase(ulke.ulkeKodu()) && ulke.vizyonlar() != null)
                .map(UlkeVizyonu::vizyonlar)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Aynı ülkede birden çok kayıt olabilir (ör. biri boş sertifikalı, gerçek API'de görüldü).
     * Önce sinema vizyonu, sonra sınırlı sinema, sonra diğerleri; geçerli TR değeri olan ilki alınır.
     */
    private static String yasSiniri(List<Vizyon> vizyonlar) {
        return vizyonlar.stream()
                .sorted(Comparator.comparingInt(TmdbDonusturucu::vizyonOnceligi))
                .map(Vizyon::certification)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(sertifika -> GECERLI_YAS_SINIRI.matcher(sertifika).matches())
                .findFirst()
                .orElse(null);
    }

    private static int vizyonOnceligi(Vizyon vizyon) {
        Integer tur = vizyon.type();
        if (Objects.equals(tur, SINEMA_VIZYONU)) {
            return 0;
        }
        return Objects.equals(tur, SINIRLI_SINEMA_VIZYONU) ? 1 : 2;
    }

    private static Set<AkilliIsaret> akilliIsaretler(long tmdbId, List<Vizyon> vizyonlar) {
        Set<AkilliIsaret> isaretler = new LinkedHashSet<>();
        vizyonlar.stream()
                .filter(vizyon -> vizyon.descriptors() != null)
                .flatMap(vizyon -> vizyon.descriptors().stream())
                .forEach(deger -> AkilliIsaret.tmdbDegerinden(deger).ifPresentOrElse(isaretler::add,
                        () -> log.info("TMDB {} filmi için bilinmeyen içerik işareti atlandı: {}", tmdbId, deger)));
        return isaretler;
    }

    private static BigDecimal puan(Double tmdbPuani) {
        if (tmdbPuani == null || tmdbPuani < 0) {
            return null;
        }
        return BigDecimal.valueOf(tmdbPuani).setScale(PUAN_BASAMAGI, RoundingMode.HALF_UP).min(EN_YUKSEK_PUAN);
    }

    private static String yonetmen(TmdbFilmCevabi film) {
        if (film.credits() == null || film.credits().crew() == null) {
            return null;
        }
        String yonetmenler = film.credits().crew().stream()
                .filter(ekip -> YONETMEN_GOREVI.equals(ekip.job()) && StringUtils.hasText(ekip.name()))
                .map(ekip -> ekip.name().trim())
                .distinct()
                .collect(Collectors.joining(", "));
        return yonetmenler.isEmpty() ? null : kes(yonetmenler, METIN_UZUNLUGU);
    }

    private static Set<String> turler(TmdbFilmCevabi film) {
        if (film.genres() == null) {
            return Set.of();
        }
        return film.genres().stream()
                .map(TmdbFilmCevabi.Ad::name)
                .filter(StringUtils::hasText)
                .map(ad -> kes(ad, TUR_UZUNLUGU))
                .limit(EN_COK_TUR)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** TMDB "order" alanına göre: başrol önce. */
    private List<String> oyuncular(TmdbFilmCevabi film) {
        if (film.credits() == null || film.credits().cast() == null) {
            return List.of();
        }
        return film.credits().cast().stream()
                .filter(oyuncu -> StringUtils.hasText(oyuncu.name()))
                .sorted(Comparator.comparing(oyuncu -> Optional.ofNullable(oyuncu.order()).orElse(Integer.MAX_VALUE)))
                .map(oyuncu -> kes(oyuncu.name(), METIN_UZUNLUGU))
                .distinct()
                .limit(ayarlar.oyuncuSiniri())
                .toList();
    }

    private static String kes(String metin, int enCok) {
        if (!StringUtils.hasText(metin)) {
            return null;
        }
        String temiz = metin.trim();
        return temiz.length() <= enCok ? temiz : temiz.substring(0, enCok);
    }
}
