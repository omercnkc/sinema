package com.sinema.auth.kullanici.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/register gövdesi. Rol alanı bilerek yok: istemci rol seçemez,
 * gövdede "role" gelse bile yok sayılır (planlama §5.8-8).
 * Ad, soyad ve telefon (K48) iyzico alıcı bilgisi içindir; bilette gösterilmez.
 */
public record KayitIstegi(
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 255, message = "E-posta en çok 255 karakter olabilir.")
        String email,

        @GecerliParola
        String password,

        @NotBlank(message = "Ad zorunludur.")
        @Size(max = KisiAdi.EN_FAZLA_KARAKTER, message = "Ad en çok 50 karakter olabilir.")
        @Pattern(regexp = KisiAdi.DESEN, message = "Ad yalnızca harf, boşluk, kesme ve tire içerebilir.")
        String firstName,

        @NotBlank(message = "Soyad zorunludur.")
        @Size(max = KisiAdi.EN_FAZLA_KARAKTER, message = "Soyad en çok 50 karakter olabilir.")
        @Pattern(regexp = KisiAdi.DESEN, message = "Soyad yalnızca harf, boşluk, kesme ve tire içerebilir.")
        String lastName,

        @GecerliTelefon
        String phone) {

    // Record'un varsayılan toString'i parolayı ve kişisel bilgileri yazar; yanlışlıkla loglanırsa sızmasın
    @Override
    public String toString() {
        return "KayitIstegi[email=" + email + ", password=***, firstName=***, lastName=***, phone=***]";
    }

    /** Ad/soyad kuralı: harfle başlar; harf (Türkçe dahil), boşluk, kesme ve tire. Baştaki/sondaki boşluk serbest. */
    public static final class KisiAdi {

        public static final int EN_FAZLA_KARAKTER = 50;
        public static final String DESEN = "^\\s*\\p{L}[\\p{L} '\\-]*\\s*$";

        private KisiAdi() {
        }
    }
}
