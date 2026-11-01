import { AkilliIsaret } from '../../../shared/models/film.models';

/** TMDB'nin TR sertifika listesi; sunucu (FilmIstegi.TR_YAS_SINIRLARI) yalnızca bunları kabul eder. */
export const TR_YAS_SINIRLARI: readonly string[] = ['Genel İzleyici Kitlesi', '6A', '6+', '10A', '10+', '13A', '13+', '16+', '18+'];

/**
 * `POST/PUT /admin/catalog/movies` gövdesi. `tmdbId` boşsa film elle girilmiştir; doluysa oluşturulduktan sonra
 * değişmez. Tarih `YYYY-MM-DD`, puan 0–10 tek ondalık.
 */
export interface FilmIstegi {
  readonly tmdbId: number | null;
  readonly title: string;
  readonly overview: string | null;
  readonly posterPath: string | null;
  readonly runtimeMinutes: number;
  readonly releaseDate: string | null;
  readonly certification: string | null;
  readonly voteAverage: number | null;
  readonly director: string | null;
  readonly genres: readonly string[];
  readonly cast: readonly string[];
  readonly contentDescriptors: readonly AkilliIsaret[];
}

/**
 * Formu dolduran veri: kayıtlı film (`GET /admin/catalog/movies/{id}`) ya da TMDB önizlemesi. Alan adları
 * FilmIstegi ile aynı; önizlemede TMDB'nin bilmediği alanlar (ör. süre, TR yaş sınırı) boş gelebilir.
 */
export interface FilmFormuBaslangici extends Omit<FilmIstegi, 'runtimeMinutes'> {
  readonly runtimeMinutes: number | null;
}

/** Kayıtlı film (admin): FilmIstegi alanları + kimlik ve görünürlük. */
export interface YonetimFilmDetayi extends FilmIstegi {
  readonly id: number;
  readonly visible: boolean;
}

/** `GET /admin/catalog/tmdb/search` adayı; `movieId` doluysa bu TMDB filmi zaten ekli. */
export interface TmdbAdayi {
  readonly tmdbId: number;
  readonly title: string;
  readonly originalTitle: string | null;
  readonly releaseDate: string | null;
  readonly posterPath: string | null;
  readonly movieId: number | null;
}

export interface TmdbAramaSonucu {
  readonly page: number;
  readonly totalPages: number;
  readonly totalResults: number;
  readonly results: readonly TmdbAdayi[];
}

/** `GET /admin/catalog/tmdb/movies/{tmdbId}`: formu ön doldurur, kaydetmez (K18). */
export interface TmdbFilmOnizlemesi extends FilmFormuBaslangici {
  readonly tmdbId: number;
  readonly movieId: number | null;
}
