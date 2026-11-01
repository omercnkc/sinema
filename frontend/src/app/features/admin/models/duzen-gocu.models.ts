/** Göçte bir koltuğun nasıl eşlendiği (booking GocNedeni). */
export type GocNedeni = 'SAME_POSITION' | 'LABEL_CHANGED' | 'REPLACEMENT';

export interface GocDegisikligi {
  readonly reservationId: number;
  readonly oldLabel: string;
  readonly newLabel: string;
  readonly reason: GocNedeni;
}

export interface CozulemeyenRezervasyon {
  readonly reservationId: number;
  readonly oldLabels: readonly string[];
}

export interface SeansGocRaporu {
  readonly showtimeId: number;
  readonly startsAt: string;
  readonly unchanged: number;
  readonly changes: readonly GocDegisikligi[];
  readonly unresolved: readonly CozulemeyenRezervasyon[];
}

/**
 * `POST /admin/bookings/layout-migrations/preview` (K35, K51): taslak yayımlanırsa biletlere ne olacağı. Aynı yerde
 * ve etiketinde kalanlar yalnızca sayılır; etiketi değişen ve yedek atananlar listelenir (sahiplerine bildirim gider).
 * `showtimes` yalnızca bileti olan başlamamış seanslardır.
 */
export interface GocRaporu {
  readonly hallId: number;
  readonly toLayoutId: number;
  readonly applicable: boolean;
  readonly summary: {
    readonly migratedShowtimes: number;
    readonly seats: number;
    readonly unchanged: number;
    readonly labelChanged: number;
    readonly replaced: number;
    readonly unresolvedReservations: number;
  };
  readonly showtimes: readonly SeansGocRaporu[];
}

/** `POST /admin/bookings/layout-migrations`. Askıdaki göç sürdürüldüğünde `report` boştur. */
export interface GocSonucu {
  readonly migrationId: number;
  readonly status: 'COMPLETED';
  readonly layoutVersion: number;
  readonly migratedShowtimes: number;
  readonly report: GocRaporu | null;
}
