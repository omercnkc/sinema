import { Observable, of } from 'rxjs';
import { catchError, map, startWith } from 'rxjs/operators';
import { UygulamaHatasi } from '../../core/hata/uygulama-hatasi';

/** Bir sayfanın veri yükleme durumu: şablon tek akıştan yükleniyor / veri / hata dallarını çizer. */
export interface YuklemeDurumu<T> {
  readonly yukleniyor: boolean;
  readonly veri: T | null;
  readonly hata: UygulamaHatasi | null;
}

const BEKLENMEYEN = new UygulamaHatasi(0, 'UNEXPECTED', 'Beklenmeyen bir hata oluştu.');

/**
 * İsteği yükleme durumuna çevirir: önce "yükleniyor", sonra veri ya da hata. Hata akışı bitirmez (catchError), bu
 * yüzden dış akış (filtre değişimi, tekrar dene) çalışmaya devam eder. `switchMap` içinde kullanılır.
 */
export function yuklemeDurumu<T>(istek: Observable<T>): Observable<YuklemeDurumu<T>> {
  return istek.pipe(
    map((veri): YuklemeDurumu<T> => ({ yukleniyor: false, veri, hata: null })),
    startWith<YuklemeDurumu<T>>({ yukleniyor: true, veri: null, hata: null }),
    catchError((hata: unknown) =>
      of<YuklemeDurumu<T>>({ yukleniyor: false, veri: null, hata: hata instanceof UygulamaHatasi ? hata : BEKLENMEYEN })),
  );
}
