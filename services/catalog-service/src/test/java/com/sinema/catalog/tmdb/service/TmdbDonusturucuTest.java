package com.sinema.catalog.tmdb.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sinema.catalog.config.TmdbAyarlari;
import com.sinema.catalog.film.entity.AkilliIsaret;
import com.sinema.catalog.tmdb.dto.TmdbFilmOnizlemesi;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.Ad;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.Ekip;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.Kadro;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.Oyuncu;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.UlkeVizyonu;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.VizyonKayitlari;
import com.sinema.catalog.tmdb.istemci.TmdbFilmCevabi.Vizyon;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TmdbDonusturucuTest {

    private static final int OYUNCU_SINIRI = 2;

    private final TmdbDonusturucu donusturucu = new TmdbDonusturucu(new TmdbAyarlari("http://tmdb", "token", "tr-TR",
            "TR", Duration.ofSeconds(1), Duration.ofSeconds(1), OYUNCU_SINIRI, Duration.ofDays(90), "-", 10));

    @Test
    void shouldMapAllFields_whenTmdbResponseComplete() {
        TmdbFilmOnizlemesi onizleme = donusturucu.onizleme(film(List.of(
                ulke("TR", new Vizyon("18+", List.of("Violence / Horror", "Sexuality"), 3)))));

        assertThat(onizleme.tmdbId()).isEqualTo(550);
        assertThat(onizleme.title()).isEqualTo("Dövüş Kulübü");
        assertThat(onizleme.runtimeMinutes()).isEqualTo(139);
        assertThat(onizleme.releaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        assertThat(onizleme.certification()).isEqualTo("18+");
        assertThat(onizleme.voteAverage()).isEqualByComparingTo("8.4");
        assertThat(onizleme.director()).isEqualTo("David Fincher");
        assertThat(onizleme.genres()).containsExactly("Dram", "Gerilim");
        assertThat(onizleme.contentDescriptors()).containsExactly(AkilliIsaret.SIDDET_KORKU, AkilliIsaret.CINSELLIK);
        assertThat(onizleme.movieId()).isNull();
    }

    @Test
    void shouldPreferTheatricalNonEmptyCertification_whenCountryHasSeveralReleases() {
        // Gerçek API'de görüldü: TR'de biri boş sertifikalı iki sinema kaydı (Başlangıç)
        TmdbFilmOnizlemesi onizleme = donusturucu.onizleme(film(List.of(ulke("TR",
                new Vizyon("16+", List.of(), 4), new Vizyon("", List.of(), 3), new Vizyon("13+", List.of(), 3)))));

        assertThat(onizleme.certification()).isEqualTo("13+");
    }

    @Test
    void shouldIgnoreOtherCountriesAndUnknownValues_whenMappingCertificationAndDescriptors() {
        TmdbFilmOnizlemesi onizleme = donusturucu.onizleme(film(List.of(
                ulke("US", new Vizyon("R", List.of("Violence / Horror"), 3)),
                ulke("TR", new Vizyon("R", List.of("Bilinmeyen İşaret", "Negative examples"), 3)))));

        assertThat(onizleme.certification()).as("TR listesinde olmayan değer atlanır").isNull();
        assertThat(onizleme.contentDescriptors()).containsExactly(AkilliIsaret.OLUMSUZ_ORNEK);
    }

    @Test
    void shouldOrderCastByBillingAndApplyLimit_whenManyActors() {
        TmdbFilmOnizlemesi onizleme = donusturucu.onizleme(film(List.of()));

        assertThat(onizleme.cast()).containsExactly("Brad Pitt", "Edward Norton");
    }

    @Test
    void shouldReturnNulls_whenOptionalFieldsMissingOrEmpty() {
        TmdbFilmCevabi eksik = new TmdbFilmCevabi(1, "Film", "  ", null, 0, "", null, null, null, null);

        TmdbFilmOnizlemesi onizleme = donusturucu.onizleme(eksik);

        assertThat(onizleme.overview()).isNull();
        assertThat(onizleme.runtimeMinutes()).isNull();
        assertThat(onizleme.releaseDate()).isNull();
        assertThat(onizleme.voteAverage()).isNull();
        assertThat(onizleme.director()).isNull();
        assertThat(onizleme.genres()).isEmpty();
        assertThat(onizleme.cast()).isEmpty();
        assertThat(onizleme.contentDescriptors()).isEmpty();
    }

    @Test
    void shouldRoundVoteToOneDecimal_whenTmdbReturnsMoreDigits() {
        TmdbFilmCevabi film = new TmdbFilmCevabi(1, "Film", null, null, 90, "2024-01-01", 7.849, null, null, null);

        assertThat(donusturucu.onizleme(film).voteAverage()).isEqualTo(new BigDecimal("7.8"));
    }

    private static TmdbFilmCevabi film(List<UlkeVizyonu> ulkeler) {
        Kadro kadro = new Kadro(
                List.of(new Oyuncu("Helena Bonham Carter", 2), new Oyuncu("Brad Pitt", 0), new Oyuncu("Edward Norton", 1)),
                List.of(new Ekip("David Fincher", "Director"), new Ekip("Jim Uhls", "Screenplay")));
        return new TmdbFilmCevabi(550, "Dövüş Kulübü", "Bir uykusuzluk hikayesi.", "/abc.jpg", 139, "1999-10-15", 8.438,
                List.of(new Ad("Dram"), new Ad("Gerilim")), kadro, new VizyonKayitlari(ulkeler));
    }

    private static UlkeVizyonu ulke(String kod, Vizyon... vizyonlar) {
        return new UlkeVizyonu(kod, List.of(vizyonlar));
    }
}
