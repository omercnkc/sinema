package com.sinema.auth.kullanici.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class GecerliParolaDogrulayicisi implements ConstraintValidator<GecerliParola, String> {

    static final int EN_AZ_KARAKTER = 8;
    static final int EN_FAZLA_BAYT = 72;

    @Override
    public boolean isValid(String parola, ConstraintValidatorContext baglam) {
        if (parola == null) {
            return false;
        }
        return parola.length() >= EN_AZ_KARAKTER
                && parola.getBytes(StandardCharsets.UTF_8).length <= EN_FAZLA_BAYT;
    }
}
