package com.sinema.auth.oturum.service;

import java.time.Duration;

/** İmzalanmış access token ve geçerlilik süresi. */
public record ErisimTokeni(String deger, Duration gecerlilik) {

    // Token bir parola gibidir; nesne loglanırsa değeri görünmesin
    @Override
    public String toString() {
        return "ErisimTokeni[deger=***, gecerlilik=" + gecerlilik + "]";
    }
}
