/** `GET /catalog/movies/{id}/showtimes` satırı. Zamanlar UTC (ekranda İstanbul), fiyat string (`"160.00"`, TRY). */
export interface SeansOzeti {
  readonly id: number;
  readonly startsAt: string;
  readonly endsAt: string;
  readonly cinemaId: number;
  readonly cinemaName: string;
  readonly hallId: number;
  readonly hallName: string;
  readonly ticketPrice: string;
  readonly salesCloseAt: string;
  /** Sunucu saatine göre (K10: başlangıçtan 10 dk sonrasına kadar açık) */
  readonly salesOpen: boolean;
}

/** `GET /catalog/cinemas` (şube filtresi). Salon listesi burada kullanılmıyor. */
export interface Sube {
  readonly id: number;
  readonly name: string;
  readonly city: string;
  readonly address: string;
}

/** Ekranda şube başına gruplanmış seanslar (saat sırasıyla). */
export interface SubeSeanslari {
  readonly subeId: number;
  readonly subeAdi: string;
  readonly seanslar: readonly SeansOzeti[];
}

/** Sunucu saat sırasıyla döner; gruplama şubelerin ilk seans sırasını korur. */
export function subeyeGoreGrupla(seanslar: readonly SeansOzeti[]): SubeSeanslari[] {
  const gruplar = new Map<number, SubeSeanslari>();
  for (const seans of seanslar) {
    const grup = gruplar.get(seans.cinemaId) ?? { subeId: seans.cinemaId, subeAdi: seans.cinemaName, seanslar: [] };
    gruplar.set(seans.cinemaId, { ...grup, seanslar: [...grup.seanslar, seans] });
  }
  return Array.from(gruplar.values());
}
