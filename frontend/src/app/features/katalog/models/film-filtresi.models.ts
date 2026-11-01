/** Liste filtresi (adres çubuğundaki sorgu parametrelerinden okunur). `sayfa` sıfırdan başlar. */
export interface FilmFiltresi {
  readonly arama: string;
  readonly tur: string;
  readonly sayfa: number;
}
