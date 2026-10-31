package com.sinema.auth.kullanici.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** TR cep telefonu (K48); kural TelefonNumarasi'nda. Boş değer geçersizdir. */
@Documented
@Constraint(validatedBy = GecerliTelefonDogrulayicisi.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface GecerliTelefon {

    String message() default "Geçerli bir cep telefonu numarası girin (ör. 0532 123 45 67).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
