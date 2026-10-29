package com.sinema.auth.kullanici.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parola politikası tek yerde (planlama §5.6): en az 8 karakter, en çok 72 BAYT.
 * Sınır karakter değil bayttır, çünkü BCrypt yalnızca ilk 72 baytı kullanır; Türkçe harfler UTF-8'de 2 bayttır.
 * Kayıt ve admin ekleme aynı kuralı kullanır.
 */
@Documented
@Constraint(validatedBy = GecerliParolaDogrulayicisi.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface GecerliParola {

    String message() default "Parola en az 8 karakter olmalı ve 72 baytı geçmemeli.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
