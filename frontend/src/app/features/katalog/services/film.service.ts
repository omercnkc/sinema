import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { Sayfa } from '../../../shared/models/sayfa.models';
import { FilmDetayi, FilmFiltresi, FilmOzeti } from '../models/film.models';

const FILM_ADRESI = `${environment.apiTabanAdresi}/catalog/movies`;
/** Izgarada 2/3/4 sütuna tam bölünür. */
export const FILM_SAYFA_BOYUTU = 12;

/**
 * Halka açık film uçları (misafir de görür). Liste ve detay hataları sayfada "tekrar dene" ile gösterildiği için
 * istekler sessizdir (genel bildirimle ikilenmez).
 */
@Injectable({ providedIn: 'root' })
export class FilmService {
  constructor(private readonly http: HttpClient) {}

  ara(filtre: FilmFiltresi): Observable<Sayfa<FilmOzeti>> {
    let parametreler = new HttpParams().set('page', filtre.sayfa).set('size', FILM_SAYFA_BOYUTU);
    if (filtre.arama) {
      parametreler = parametreler.set('q', filtre.arama);
    }
    if (filtre.tur) {
      parametreler = parametreler.set('genre', filtre.tur);
    }
    return this.http.get<Sayfa<FilmOzeti>>(FILM_ADRESI, { params: parametreler, context: sessizBaglam() });
  }

  getir(filmId: number): Observable<FilmDetayi> {
    return this.http.get<FilmDetayi>(`${FILM_ADRESI}/${filmId}`, { context: sessizBaglam() });
  }

  /** Tür filtresi seçenekleri; alınamazsa filtre boş kalır, liste yine çalışır. */
  turler(): Observable<readonly string[]> {
    return this.http.get<string[]>(`${FILM_ADRESI}/genres`, { context: sessizBaglam() }).pipe(catchError(() => of([])));
  }
}
