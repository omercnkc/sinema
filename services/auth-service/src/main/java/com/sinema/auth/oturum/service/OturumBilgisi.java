package com.sinema.auth.oturum.service;

/**
 * Giriş/yenileme sonucu. Access token gövdeye, refresh token cookie'ye gider.
 *
 * @param yenilemeTokeni yeni refresh token; tolerans penceresindeki yenilemede null'dır (cookie değişmez)
 */
public record OturumBilgisi(ErisimTokeni erisimTokeni, String yenilemeTokeni) {

    @Override
    public String toString() {
        return "OturumBilgisi[erisimTokeni=" + erisimTokeni + ", yenilemeTokeni=***]";
    }
}
