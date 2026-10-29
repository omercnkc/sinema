package com.sinema.auth.kullanici.dto;

/** GET /api/v1/auth/me: Angular oturum durumunu bundan kurar (planlama §5.3). */
public record ProfilCevabi(Long id, String email, String role) {
}
