/** Sunucunun RFC 7807 problem+json cevabı (planlama §7.2, sinema-common GlobalHataYoneticisi). */
export interface ProblemDetayi {
  status?: number;
  title?: string;
  detail?: string;
  code?: string;
  correlationId?: string;
  errors?: AlanHatasi[];
  retryAfterSeconds?: number;
  /** İlgili hatalarda ek alanlar (unavailableCellIds, reservationId, invalidCellIds...) */
  [ekAlan: string]: unknown;
}

/** Doğrulama hatasında bir form alanının mesajı (alan adı istek gövdesindeki adla aynı). */
export interface AlanHatasi {
  field: string;
  message: string;
}

/**
 * Uygulamanın her yerde gördüğü tek hata tipi (ErrorInterceptor üretir). Bileşenler HttpErrorResponse ile
 * uğraşmaz; kullanıcıya gösterilecek mesaj, makine okunur kod ve form alanı hataları hazır gelir.
 *
 * durum 0: sunucuya ulaşılamadı (ağ hatası, CORS, kapalı servis).
 */
export class UygulamaHatasi extends Error {
  constructor(
    readonly durum: number,
    readonly kod: string,
    readonly mesaj: string,
    readonly alanHatalari: readonly AlanHatasi[] = [],
    readonly beklemeSaniyesi: number | null = null,
    readonly korelasyonKimligi: string | null = null,
    readonly ekAlanlar: Readonly<Record<string, unknown>> = {},
  ) {
    super(mesaj);
    this.name = 'UygulamaHatasi';
  }
}
