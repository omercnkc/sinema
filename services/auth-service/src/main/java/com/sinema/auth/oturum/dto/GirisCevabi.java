package com.sinema.auth.oturum.dto;

/**
 * Giriş cevabı (planlama §7.3). Access token gövdede döner ve istemcide yalnızca bellekte tutulur;
 * refresh token alt görev 4'te HttpOnly cookie olarak eklenir.
 */
public record GirisCevabi(String accessToken, long expiresInSeconds) {

    @Override
    public String toString() {
        return "GirisCevabi[accessToken=***, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
