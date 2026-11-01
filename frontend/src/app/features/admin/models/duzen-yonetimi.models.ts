import { Hucre } from '../../../shared/components/koltuk-haritasi/koltuk-haritasi.models';

export type DuzenDurumu = 'DRAFT' | 'PUBLISHED';

/** `GET /admin/catalog/halls/{id}/layouts` satırı (en yeni önce). Taslakta `version` ve `publishedAt` boş. */
export interface DuzenOzeti {
  readonly id: number;
  readonly version: number | null;
  readonly status: DuzenDurumu;
  readonly active: boolean;
  readonly gridRows: number;
  readonly gridCols: number;
  readonly seatCount: number;
  readonly createdAt: string;
  readonly publishedAt: string | null;
}

/** Düzenin tamamı (taslak ya da yayımlı sürüm); hücre alanları halka açık düzen ucuyla aynı. */
export interface DuzenCevabi {
  readonly id: number;
  readonly hallId: number;
  readonly version: number | null;
  readonly status: DuzenDurumu;
  readonly active: boolean;
  readonly gridRows: number;
  readonly gridCols: number;
  readonly cells: readonly Hucre[];
}
