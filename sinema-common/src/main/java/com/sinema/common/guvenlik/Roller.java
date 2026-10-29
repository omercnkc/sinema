package com.sinema.common.guvenlik;

/**
 * Token'daki "role" değerleri. USER/ADMIN kullanıcı token'larında, SERVICE yalnızca servisler arası
 * token'larda bulunur (planlama §5.5). Tek yerde: imzalayan (auth) ve doğrulayanlar aynı adları kullanır.
 */
public final class Roller {

    public static final String ROL_ALANI = "role";
    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String SERVICE = "SERVICE";

    private Roller() {
    }
}
