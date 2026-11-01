import { FilmOzeti } from '../../../shared/models/film.models';

/** `GET /admin/catalog/movies` satırı: gizli filmler de gelir (K40: film silinmez, gizlenir). */
export interface YonetimFilmOzeti extends FilmOzeti {
  readonly visible: boolean;
}

/** Admin film listesi filtresi (adres çubuğundan okunur). `sayfa` sıfırdan başlar. */
export interface YonetimFilmFiltresi {
  readonly arama: string;
  readonly sayfa: number;
}
