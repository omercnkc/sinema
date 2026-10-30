package com.sinema.booking.doluluk;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * K47: değeri kısa bir süre (ör. 2 sn) bellekte tutan önbellek. Koltuk doluluğu sorgusunun her polling isteğinde
 * MySQL'e gitmesini önler; doğruluk kaynağı yine veritabanıdır (K9), burası yalnızca gösterimi ucuzlatır.
 *
 * <ul>
 *   <li><b>Zamanlayıcı yok:</b> kayıt yoksa ya da eskiyse o anki istek yükler; kimsenin istemediği anahtar için
 *       hiç yükleme yapılmaz.</li>
 *   <li><b>Aynı anda gelenler tek yüklemeyi bekler:</b> kayıt eskidiği anda 1.000 istek gelse bile veritabanına
 *       tek sorgu gider (yükleyen ilk istek, diğerleri onun sonucunu bekler).</li>
 *   <li><b>Hata önbelleğe alınmaz:</b> yükleme başarısız olursa bekleyenler aynı hatayı alır, kayıt silinir,
 *       sonraki istek yeniden dener.</li>
 *   <li><b>Bellek sızıntısı yok:</b> eskiyen kayıtlar her yeni yüklemede temizlenir.</li>
 * </ul>
 * Yükleme sürerken {@link #sil} çağrılırsa o yükleme bekleyenlerine bir kez eski veri verebilir; en fazla bir
 * polling turu sürer, doğruluğu etkilemez (koltuk tutma kararı veritabanındadır).
 *
 * @param <K> anahtar (ör. seans id'si)
 * @param <V> değer; değişmez (immutable) olmalı, çünkü aynı nesne tüm isteklere paylaştırılır
 */
public class KisaOmurluOnbellek<K, V> {

    private final Duration omur;
    private final Clock saat;
    private final ConcurrentHashMap<K, Kayit<V>> kayitlar = new ConcurrentHashMap<>();

    private record Kayit<V>(CompletableFuture<V> deger, Instant olusturulma) {
    }

    public KisaOmurluOnbellek(Duration omur, Clock saat) {
        if (omur.isNegative() || omur.isZero()) {
            throw new IllegalArgumentException("Önbellek ömrü pozitif olmalı: " + omur);
        }
        this.omur = omur;
        this.saat = saat;
    }

    public V getir(K anahtar, Supplier<V> yukleyici) {
        Instant simdi = saat.instant();
        CompletableFuture<V> yeniYukleme = new CompletableFuture<>();
        // compute anahtar bazında atomiktir: aynı anda gelenlerden yalnızca biri yeni kaydı yerleştirir
        Kayit<V> kayit = kayitlar.compute(anahtar, (k, mevcut) ->
                mevcut != null && tazeMi(mevcut, simdi) ? mevcut : new Kayit<>(yeniYukleme, simdi));
        if (kayit.deger() == yeniYukleme) {
            yukle(anahtar, kayit, yukleyici);
            eskileriTemizle(simdi);
        }
        return sonucuBekle(kayit.deger());
    }

    /** Veri değişti (hold, onay, bırakma): bir sonraki istek veritabanından taze okur. */
    public void sil(K anahtar) {
        kayitlar.remove(anahtar);
    }

    /** Test ve izleme için: şu an bellekte duran kayıt sayısı. */
    int boyut() {
        return kayitlar.size();
    }

    private void yukle(K anahtar, Kayit<V> kayit, Supplier<V> yukleyici) {
        try {
            kayit.deger().complete(yukleyici.get());
        } catch (RuntimeException e) {
            kayit.deger().completeExceptionally(e);
            kayitlar.remove(anahtar, kayit); // yalnızca bu kaydı sil; arada başkası yenisini koyduysa dokunma
        }
    }

    private void eskileriTemizle(Instant simdi) {
        kayitlar.values().removeIf(kayit -> !tazeMi(kayit, simdi));
    }

    private boolean tazeMi(Kayit<V> kayit, Instant simdi) {
        return simdi.isBefore(kayit.olusturulma().plus(omur));
    }

    /** join() hatayı CompletionException içine sarar; çağıran asıl hatayı (ör. veritabanı hatası) görsün. */
    private static <V> V sonucuBekle(CompletableFuture<V> deger) {
        try {
            return deger.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException asil) {
                throw asil;
            }
            throw e;
        }
    }
}
