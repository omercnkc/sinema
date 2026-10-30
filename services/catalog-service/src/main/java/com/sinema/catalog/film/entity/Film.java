package com.sinema.catalog.film.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Film (movies). TMDB'den kopyalanır ya da elle girilir; admin her alanı düzenleyebilir (K18).
 * Liste alanları ayrı tablolardadır (K43). @BatchSize: bir sayfa film için listeler film başına
 * ayrı sorguyla değil, toplu (IN) sorguyla yüklenir (N+1 önlemi).
 */
@Entity
@Table(name = "movies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Film {

    private static final int LISTE_YUKLEME_PAKETI = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true, updatable = false)
    private Long tmdbId;

    @Column(name = "title", nullable = false)
    private String baslik;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String ozet;

    @Column(name = "poster_path")
    private String afisYolu;

    @Column(name = "runtime_minutes", nullable = false)
    private int sureDakika;

    @Column(name = "release_date")
    private LocalDate vizyonTarihi;

    @Column(name = "certification", length = 30)
    private String yasSiniri;

    @Column(name = "vote_average", precision = 3, scale = 1)
    private BigDecimal tmdbPuani;

    @Column(name = "director")
    private String yonetmen;

    @Column(name = "visible", nullable = false)
    private boolean gorunur;

    @Column(name = "tmdb_synced_at")
    private Instant tmdbSenkronZamani;

    @ElementCollection
    @CollectionTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "genre", nullable = false, length = 50)
    @OrderBy
    @BatchSize(size = LISTE_YUKLEME_PAKETI)
    private Set<String> turler = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_cast_members", joinColumns = @JoinColumn(name = "movie_id"))
    @OrderColumn(name = "position")
    @Column(name = "name", nullable = false)
    @BatchSize(size = LISTE_YUKLEME_PAKETI)
    private List<String> oyuncular = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "movie_content_descriptors", joinColumns = @JoinColumn(name = "movie_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "descriptor", nullable = false, length = 30)
    @BatchSize(size = LISTE_YUKLEME_PAKETI)
    private Set<AkilliIsaret> akilliIsaretler = new LinkedHashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    /** Elle girilen film: TMDB bağı yok, tazelik takibi yok. */
    public static Film elleOlustur(FilmBilgileri bilgiler) {
        Film film = new Film();
        film.gorunur = true;
        film.bilgileriGuncelle(bilgiler);
        return film;
    }

    /** TMDB'den doldurulmuş film: çekim anı tutulur, K45'e göre süresi gelince yenilenir. */
    public static Film tmdbdenOlustur(long tmdbId, FilmBilgileri bilgiler, Instant cekimZamani) {
        Film film = elleOlustur(bilgiler);
        film.tmdbId = tmdbId;
        film.tmdbSenkronZamani = cekimZamani;
        return film;
    }

    /** Koleksiyonlar yerinde güncellenir: Hibernate yeni koleksiyon nesnesini "tamamen değişti" sayardı. */
    public void bilgileriGuncelle(FilmBilgileri bilgiler) {
        this.baslik = bilgiler.baslik().trim();
        this.ozet = bilgiler.ozet();
        this.afisYolu = bilgiler.afisYolu();
        this.sureDakika = bilgiler.sureDakika();
        this.vizyonTarihi = bilgiler.vizyonTarihi();
        this.yasSiniri = bilgiler.yasSiniri();
        this.tmdbPuani = bilgiler.tmdbPuani();
        this.yonetmen = bilgiler.yonetmen();
        koleksiyonuDegistir(turler, bilgiler.turler());
        koleksiyonuDegistir(oyuncular, bilgiler.oyuncular());
        koleksiyonuDegistir(akilliIsaretler, bilgiler.akilliIsaretler());
    }

    /** K45 yenilemesi: TMDB'den boş gelen alanlar mevcut değeri korur; çekim anı güncellenir. */
    public void tmdbIleYenile(FilmBilgileri tmdbBilgileri, Instant cekimZamani) {
        if (tmdbId == null) {
            throw new IllegalStateException("Elle girilmiş film TMDB'den yenilenemez: " + id);
        }
        bilgileriGuncelle(FilmBilgileri.bosOlmayanlarlaBirlestir(mevcutBilgiler(), tmdbBilgileri));
        this.tmdbSenkronZamani = cekimZamani;
    }

    public FilmBilgileri mevcutBilgiler() {
        return new FilmBilgileri(baslik, ozet, afisYolu, sureDakika, vizyonTarihi, yasSiniri, tmdbPuani, yonetmen,
                new LinkedHashSet<>(turler), new ArrayList<>(oyuncular), new LinkedHashSet<>(akilliIsaretler));
    }

    public void gorunurlukDegistir(boolean gorunur) {
        this.gorunur = gorunur;
    }

    public Set<String> getTurler() {
        return Collections.unmodifiableSet(turler);
    }

    public List<String> getOyuncular() {
        return Collections.unmodifiableList(oyuncular);
    }

    public Set<AkilliIsaret> getAkilliIsaretler() {
        return Collections.unmodifiableSet(akilliIsaretler);
    }

    private static <T> void koleksiyonuDegistir(Collection<T> mevcut, Collection<T> yeni) {
        mevcut.clear();
        if (yeni != null) {
            mevcut.addAll(yeni);
        }
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Film film && id != null && id.equals(film.id);
    }

    @Override
    public int hashCode() {
        return Film.class.hashCode();
    }
}
