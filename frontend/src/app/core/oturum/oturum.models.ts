/** Sunucudaki roller (auth-service, JWT "role"). Misafir bir rol değildir: oturum yoksa kullanıcı da yoktur (K3). */
export type Rol = 'USER' | 'ADMIN';

/** `GET /auth/me` cevabı. Kişi alanları eski hesaplarda (ilk admin) boş olabilir (K48). */
export interface Kullanici {
  readonly id: number;
  readonly email: string;
  readonly role: Rol;
  readonly firstName: string | null;
  readonly lastName: string | null;
  readonly phone: string | null;
}

/** `POST /auth/login` ve `POST /auth/refresh` cevabı; refresh token ayrıca HttpOnly cookie olarak gelir. */
export interface TokenCevabi {
  readonly accessToken: string;
  readonly expiresInSeconds: number;
}

export interface GirisBilgileri {
  readonly email: string;
  readonly password: string;
}
