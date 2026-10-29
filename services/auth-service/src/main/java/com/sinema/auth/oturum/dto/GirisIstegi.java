package com.sinema.auth.oturum.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/login gövdesi. Parola politikası (@GecerliParola) burada uygulanmaz:
 * girişte yalnızca boş olmaması aranır, kural kayıtta denetlenir.
 */
public record GirisIstegi(
        @NotBlank(message = "E-posta zorunludur.") String email,
        @NotBlank(message = "Parola zorunludur.") String password) {

    @Override
    public String toString() {
        return "GirisIstegi[email=" + email + ", password=***]";
    }
}
