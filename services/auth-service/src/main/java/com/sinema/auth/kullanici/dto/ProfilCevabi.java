package com.sinema.auth.kullanici.dto;

/**
 * GET /api/v1/auth/me: Angular oturum durumunu bundan kurar (planlama §5.3). Kişi bilgileri (K48) yalnızca
 * kullanıcının kendisine döner; admin ve eski hesaplarda boş olabilir.
 */
public record ProfilCevabi(Long id, String email, String role, String firstName, String lastName, String phone) {
}
