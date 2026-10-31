/** `POST /auth/register` gövdesi (K48). Telefon kullanıcının yazdığı gibi gider; sunucu +905XXXXXXXXX'e çevirir. */
export interface KayitIstegi {
  readonly email: string;
  readonly password: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly phone: string;
}

export interface KayitCevabi {
  readonly id: number;
  readonly email: string;
}
