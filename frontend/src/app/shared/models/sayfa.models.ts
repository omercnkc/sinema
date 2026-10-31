/** Sunucunun sayfalı liste cevabı (sinema-common SayfaCevabi, planlama §7.2). `page` sıfırdan başlar. */
export interface Sayfa<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
