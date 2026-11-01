/** Sunucu sınırı (SeansOlusturmaIstegi): satış en erken başlangıçtan 2 saat önce, en geç 2 saat sonra kapanır. */
export const SATIS_OFSETI_EN_AZ = -120;
export const SATIS_OFSETI_EN_FAZLA = 120;

/**
 * `GET /admin/catalog/showtimes?hallId&date` satırı. Gizli filmlerin seansları da gelir (`movieVisible`).
 * `layoutId` seansın mühürlü düzenidir; salonun aktif düzeninden farklıysa seans eski sürümdedir (K17).
 */
export interface YonetimSeansOzeti {
  readonly id: number;
  readonly movieId: number;
  readonly movieTitle: string;
  readonly movieVisible: boolean;
  readonly startsAt: string;
  readonly endsAt: string;
  readonly ticketPrice: string;
  readonly salesCloseOffsetMinutes: number;
  readonly salesCloseAt: string;
  readonly salesOpen: boolean;
  readonly layoutId: number;
}

/** `POST /admin/catalog/showtimes`. Fiyat boşsa salonun varsayılanı, ofset boşsa sunucu varsayılanı (K10, K23). */
export interface SeansOlusturmaIstegi {
  readonly movieId: number;
  readonly hallId: number;
  readonly startsAt: string;
  readonly ticketPrice: string | null;
  readonly salesCloseOffsetMinutes: number | null;
}

/** `PUT /admin/catalog/showtimes/{id}`: yalnızca fiyat ve ofset; film, salon ve saat değişmez, seans silinmez (K34). */
export interface SeansGuncellemeIstegi {
  readonly ticketPrice: string;
  readonly salesCloseOffsetMinutes: number;
}
