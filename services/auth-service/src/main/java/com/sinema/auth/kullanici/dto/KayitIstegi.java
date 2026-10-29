package com.sinema.auth.kullanici.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/register gövdesi. Rol alanı bilerek yok: istemci rol seçemez,
 * gövdede "role" gelse bile yok sayılır (planlama §5.8-8).
 */
public record KayitIstegi(
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 255, message = "E-posta en çok 255 karakter olabilir.")
        String email,

        @GecerliParola
        String password) {

    // Record'un varsayılan toString'i parolayı da yazar; yanlışlıkla loglanırsa sızmasın
    @Override
    public String toString() {
        return "KayitIstegi[email=" + email + ", password=***]";
    }
}
