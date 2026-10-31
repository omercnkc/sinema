/** `GET /catalog/showtimes/{id}`: koltuk seçimi sayfasının başlığı. Zamanlar UTC, fiyat string (TRY). */
export interface SeansDetayi {
  readonly id: number;
  readonly movieId: number;
  readonly movieTitle: string;
  readonly posterPath: string | null;
  readonly certification: string | null;
  readonly cinemaId: number;
  readonly cinemaName: string;
  readonly hallId: number;
  readonly hallName: string;
  readonly layoutId: number;
  readonly startsAt: string;
  readonly endsAt: string;
  readonly ticketPrice: string;
  readonly salesCloseOffsetMinutes: number;
  readonly salesCloseAt: string;
  readonly salesOpen: boolean;
  /** İstemci saatine güvenilmez; geri sayımlar sunucu saatine hizalanır (M6, §4.3) */
  readonly serverTime: string;
}
