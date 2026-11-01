import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { TmdbAramaSonucu, TmdbFilmOnizlemesi } from '../models/film-formu.models';
import { ADMIN_KATALOG_ADRESI } from './admin-api';

const TMDB_ADRESI = `${ADMIN_KATALOG_ADRESI}/tmdb`;

/**
 * TMDB araması ve form ön doldurma (catalog üzerinden; tarayıcı TMDB'ye doğrudan istek atmaz, anahtar sunucuda).
 * TMDB erişilemezse 503 TMDB_UNAVAILABLE gelir; elle giriş her zaman çalışır.
 */
@Injectable({ providedIn: 'root' })
export class TmdbService {
  constructor(private readonly http: HttpClient) {}

  ara(sorgu: string): Observable<TmdbAramaSonucu> {
    const parametreler = new HttpParams().set('query', sorgu);
    return this.http.get<TmdbAramaSonucu>(`${TMDB_ADRESI}/search`, { params: parametreler, context: sessizBaglam() });
  }

  onizleme(tmdbId: number): Observable<TmdbFilmOnizlemesi> {
    return this.http.get<TmdbFilmOnizlemesi>(`${TMDB_ADRESI}/movies/${tmdbId}`, { context: sessizBaglam() });
  }
}
