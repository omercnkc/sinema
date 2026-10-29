package com.sinema.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Testte zamanı ileri sarmak için: 5 dakikalık kilidin bitişini beklemeden denemeyi sağlar. */
public class AyarlanabilirSaat extends Clock {

    private final AtomicReference<Duration> kayma = new AtomicReference<>(Duration.ZERO);

    public void ilerlet(Duration sure) {
        kayma.updateAndGet(mevcut -> mevcut.plus(sure));
    }

    public void sifirla() {
        kayma.set(Duration.ZERO);
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(kayma.get());
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Testte yalnızca UTC kullanılır");
    }
}
