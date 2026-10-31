package com.sinema.auth.kullanici.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GecerliTelefonDogrulayicisi implements ConstraintValidator<GecerliTelefon, String> {

    @Override
    public boolean isValid(String telefon, ConstraintValidatorContext baglam) {
        return TelefonNumarasi.normalizeEt(telefon).isPresent();
    }
}
