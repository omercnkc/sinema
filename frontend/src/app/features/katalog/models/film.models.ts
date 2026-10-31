/** Akıllı işaretler (K46); sunucu enum adlarıyla gelir. */
export type AkilliIsaret = 'SIDDET_KORKU' | 'CINSELLIK' | 'OLUMSUZ_ORNEK';

/** Ekranda gösterilen adlar (RTÜK akıllı işaret adları). */
export const AKILLI_ISARET_ADLARI: Readonly<Record<AkilliIsaret, string>> = {
  SIDDET_KORKU: 'Şiddet / Korku',
  CINSELLIK: 'Cinsellik',
  OLUMSUZ_ORNEK: 'Olumsuz örnek oluşturabilecek davranışlar',
};

/** `GET /catalog/movies` liste kartı. Tarih `YYYY-MM-DD`, puan 0–10 (TMDB). */
export interface FilmOzeti {
  readonly id: number;
  readonly title: string;
  readonly posterPath: string | null;
  readonly runtimeMinutes: number;
  readonly releaseDate: string | null;
  readonly certification: string | null;
  readonly voteAverage: number | null;
  readonly genres: readonly string[];
}

/** `GET /catalog/movies/{id}`. `tmdbId` doluysa puan "TMDB puanı" olarak gösterilir. Oyuncular başrol önce. */
export interface FilmDetayi extends FilmOzeti {
  readonly tmdbId: number | null;
  readonly overview: string | null;
  readonly director: string | null;
  readonly cast: readonly string[];
  readonly contentDescriptors: readonly AkilliIsaret[];
}

/** Liste filtresi (adres çubuğundaki sorgu parametrelerinden okunur). `sayfa` sıfırdan başlar. */
export interface FilmFiltresi {
  readonly arama: string;
  readonly tur: string;
  readonly sayfa: number;
}
