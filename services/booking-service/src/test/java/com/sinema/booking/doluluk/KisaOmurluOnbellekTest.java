package com.sinema.booking.doluluk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class KisaOmurluOnbellekTest {

    private static final Duration OMUR = Duration.ofSeconds(2);

    private final ElleSaat saat = new ElleSaat();
    private final KisaOmurluOnbellek<Long, String> onbellek = new KisaOmurluOnbellek<>(OMUR, saat);
    private final AtomicInteger yuklemeSayisi = new AtomicInteger();

    @Test
    void shouldServeFromMemory_whenEntryYoungerThanLifetime() {
        assertThat(onbellek.getir(42L, this::yukle)).isEqualTo("veri-1");
        saat.ilerlet(Duration.ofMillis(1_999));

        assertThat(onbellek.getir(42L, this::yukle)).isEqualTo("veri-1");
        assertThat(yuklemeSayisi).hasValue(1);
    }

    @Test
    void shouldReload_whenLifetimeExpired() {
        onbellek.getir(42L, this::yukle);
        saat.ilerlet(OMUR);

        assertThat(onbellek.getir(42L, this::yukle)).isEqualTo("veri-2");
    }

    @Test
    void shouldReloadImmediately_whenEntryDeletedAfterDataChange() {
        onbellek.getir(42L, this::yukle);

        onbellek.sil(42L);

        assertThat(onbellek.getir(42L, this::yukle)).isEqualTo("veri-2");
    }

    @Test
    void shouldKeepKeysIndependent_whenDifferentShowtimesRequested() {
        onbellek.getir(1L, this::yukle);
        onbellek.getir(2L, this::yukle);
        onbellek.sil(1L);

        assertThat(onbellek.getir(2L, this::yukle)).isEqualTo("veri-2");
        assertThat(yuklemeSayisi).hasValue(2);
    }

    @Test
    void shouldNotCacheFailure_andRetryOnNextRequest() {
        assertThatThrownBy(() -> onbellek.getir(42L, () -> {
            throw new IllegalStateException("veritabanı yok");
        })).isInstanceOf(IllegalStateException.class).hasMessage("veritabanı yok");

        assertThat(onbellek.getir(42L, this::yukle)).isEqualTo("veri-1");
    }

    @Test
    void shouldRemoveStaleEntries_whenNewLoadHappens() {
        onbellek.getir(1L, this::yukle);
        onbellek.getir(2L, this::yukle);
        saat.ilerlet(OMUR);

        onbellek.getir(3L, this::yukle);

        assertThat(onbellek.boyut()).isEqualTo(1);
    }

    @Test
    void shouldRunSingleLoad_whenManyRequestsArriveTogether() throws Exception {
        int istekSayisi = 50;
        CountDownLatch yuklemeBasladi = new CountDownLatch(1);
        CountDownLatch yuklemeyiBitir = new CountDownLatch(1);
        ExecutorService havuz = Executors.newFixedThreadPool(istekSayisi);
        try {
            List<Future<String>> sonuclar = IntStream.range(0, istekSayisi)
                    .mapToObj(i -> havuz.submit(() -> onbellek.getir(42L, () -> yavasYukle(yuklemeBasladi,
                            yuklemeyiBitir))))
                    .toList();
            assertThat(yuklemeBasladi.await(5, TimeUnit.SECONDS)).isTrue();
            yuklemeyiBitir.countDown(); // diğer 49 istek bu sırada bekliyor

            for (Future<String> sonuc : sonuclar) {
                assertThat(sonuc.get(5, TimeUnit.SECONDS)).isEqualTo("veri-1");
            }
            assertThat(yuklemeSayisi).hasValue(1);
        } finally {
            havuz.shutdownNow();
        }
    }

    private String yukle() {
        return "veri-" + yuklemeSayisi.incrementAndGet();
    }

    private String yavasYukle(CountDownLatch basladi, CountDownLatch bitir) {
        basladi.countDown();
        try {
            assertThat(bitir.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return yukle();
    }

    /** Gerçek zamandan bağımsız: süre yalnızca test ilerletince akar (sonuçlar her makinede aynı). */
    private static final class ElleSaat extends Clock {

        private Instant an = Instant.parse("2026-11-01T17:00:00Z");

        void ilerlet(Duration sure) {
            an = an.plus(sure);
        }

        @Override
        public Instant instant() {
            return an;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
