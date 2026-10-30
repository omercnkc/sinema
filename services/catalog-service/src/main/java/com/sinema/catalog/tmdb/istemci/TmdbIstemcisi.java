package com.sinema.catalog.tmdb.istemci;

import com.sinema.catalog.config.TmdbAyarlari;
import com.sinema.common.hata.BagimlilikKullanilamazIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * TMDB API adaptörü (Adapter pattern): HTTP ayrıntıları ve TMDB hataları burada kalır, dışarıya
 * yalnızca bizim istisnalarımız çıkar. 404 → TMDB_MOVIE_NOT_FOUND; diğer her hata (zaman aşımı,
 * 401 token, 429 hız sınırı, 5xx) → 503 TMDB_UNAVAILABLE. Ayrıntı loglanır, istemciye sızmaz.
 */
@Slf4j
@Component
public class TmdbIstemcisi {

    public static final String TMDB_KULLANILAMAZ_KODU = "TMDB_UNAVAILABLE";
    public static final String TMDB_FILM_YOK_KODU = "TMDB_MOVIE_NOT_FOUND";
    private static final String FILM_EKLERI = "release_dates,credits";

    private final RestClient restClient;
    private final TmdbAyarlari ayarlar;

    public TmdbIstemcisi(@Qualifier("tmdbRestClient") RestClient restClient, TmdbAyarlari ayarlar) {
        this.restClient = restClient;
        this.ayarlar = ayarlar;
    }

    public TmdbAramaCevabi ara(String sorgu, int sayfa) {
        return cagir(() -> restClient.get()
                .uri(uri -> uri.path("/search/movie")
                        .queryParam("query", sorgu)
                        .queryParam("page", sayfa)
                        .queryParam("language", ayarlar.dil())
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(TmdbAramaCevabi.class));
    }

    /** Film detayı, yaş sınırları ve kadro tek istekte (append_to_response, K22). */
    public TmdbFilmCevabi filmGetir(long tmdbId) {
        return cagir(() -> restClient.get()
                .uri(uri -> uri.path("/movie/{id}")
                        .queryParam("language", ayarlar.dil())
                        .queryParam("append_to_response", FILM_EKLERI)
                        .build(tmdbId))
                .retrieve()
                .body(TmdbFilmCevabi.class));
    }

    private <T> T cagir(Supplier<T> istek) {
        if (!ayarlar.tokenVar()) {
            throw kullanilamaz("TMDB yapılandırılmamış; filmi elle girebilirsiniz.");
        }
        try {
            T cevap = istek.get();
            if (cevap == null) {
                throw kullanilamaz("TMDB boş cevap döndü.");
            }
            return cevap;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new KaynakBulunamadiIstisnasi(TMDB_FILM_YOK_KODU, "TMDB'de bu film bulunamadı.");
            }
            log.warn("TMDB istemci hatası: {}", e.getStatusCode());
            throw kullanilamaz("TMDB isteği reddetti; filmi elle girebilirsiniz.");
        } catch (RestClientException e) {
            log.warn("TMDB'ye ulaşılamadı: {}", e.getMessage());
            throw kullanilamaz("TMDB'ye şu an ulaşılamıyor; filmi elle girebilirsiniz.");
        }
    }

    private static BagimlilikKullanilamazIstisnasi kullanilamaz(String mesaj) {
        return new BagimlilikKullanilamazIstisnasi(TMDB_KULLANILAMAZ_KODU, mesaj);
    }
}
