/** Rezervasyon durumları (booking RezervasyonDurumu). */
export type RezervasyonDurumu = 'PENDING_PAYMENT' | 'CONFIRMED' | 'EXPIRED' | 'FAILED' | 'RELEASED';

/**
 * Rezervasyon cevabı (tutma, tekil sorgu ve liste aynı biçim). Film/şube/salon/saat oluşturma anının kopyasıdır.
 * Geri sayım `expiresAt` ile `serverTime` farkından hesaplanır (istemci saatine güvenilmez, §4.3).
 */
export interface Rezervasyon {
  readonly reservationId: number;
  readonly showtimeId: number;
  readonly status: RezervasyonDurumu;
  readonly movieTitle: string;
  readonly cinemaName: string;
  readonly hallName: string;
  readonly startsAt: string;
  readonly expiresAt: string;
  readonly serverTime: string;
  readonly seats: readonly { readonly cellId: number; readonly label: string }[];
  readonly totalAmount: string;
  /** Yalnızca CONFIRMED'da dolu */
  readonly ticketCode: string | null;
}

/** `POST /bookings/reservations` (K24: en çok 5 koltuk). */
export interface TutmaIstegi {
  readonly showtimeId: number;
  readonly seatCellIds: readonly number[];
}

/** K24: bir işlemde en çok 5 koltuk (sunucu da denetler). */
export const EN_FAZLA_KOLTUK = 5;

/** Ekranda gösterilen durum: süresi geçmiş ama sunucu job'ı henüz EXPIRED yapmamış tutma da "süresi doldu" sayılır. */
export type GorunenDurum = 'ODEME_BEKLIYOR' | 'ONAYLANDI' | 'SURESI_DOLDU' | 'BIRAKILDI' | 'BASARISIZ';

export function gorunenDurum(rezervasyon: Rezervasyon, sunucuSimdi: number): GorunenDurum {
  switch (rezervasyon.status) {
    case 'CONFIRMED':
      return 'ONAYLANDI';
    case 'RELEASED':
      return 'BIRAKILDI';
    case 'FAILED':
      return 'BASARISIZ';
    case 'EXPIRED':
      return 'SURESI_DOLDU';
    default:
      return Date.parse(rezervasyon.expiresAt) <= sunucuSimdi ? 'SURESI_DOLDU' : 'ODEME_BEKLIYOR';
  }
}
