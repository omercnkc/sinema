package com.sinema.auth.kullanici.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin oluşturma (POST /api/v1/admin/auth/admins ve .env'deki ilk admin): yalnızca e-posta ve parola.
 * Adminler personel hesabıdır; kişi bilgisi (K48) istenmez. Herkese açık kayıttan (KayitIstegi) bu yüzden ayrı.
 */
public record AdminIstegi(
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 255, message = "E-posta en çok 255 karakter olabilir.")
        String email,

        @GecerliParola
        String password) {

    @Override
    public String toString() {
        return "AdminIstegi[email=" + email + ", password=***]";
    }
}
