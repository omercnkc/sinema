package com.sinema.auth.kullanici.dto;

/** Kayıt sonrası dönen veri: kimlik ve e-posta; parola özeti ve rol dışarı çıkmaz. */
public record KayitCevabi(Long id, String email) {
}
