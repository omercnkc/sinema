/** Düzen hücre tipleri (catalog HucreTipi). Boşluk saklanmaz: hücre yoksa koridor/boş alandır. */
export type HucreTipi = 'SEAT' | 'ENTRANCE' | 'EXIT' | 'SCREEN' | 'WALL';
/** K28: fiyat farkı yok, yalnızca görsel/erişilebilirlik. */
export type KoltukTipi = 'STANDARD' | 'ACCESSIBLE';
