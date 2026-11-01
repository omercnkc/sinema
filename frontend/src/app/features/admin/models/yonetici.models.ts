/** `POST /admin/auth/admins` gövdesi. Yöneticiler personel hesabıdır: kişi bilgisi (K48) istenmez. */
export interface YoneticiIstegi {
  readonly email: string;
  readonly password: string;
}

export interface YoneticiCevabi {
  readonly id: number;
  readonly email: string;
}
