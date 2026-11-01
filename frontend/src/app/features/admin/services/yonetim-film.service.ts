import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { Sayfa } from '../../../shared/models/sayfa.models';
import { YonetimFilmFiltresi, YonetimFilmOzeti } from '../models/yonetim-film.models';
import { ADMIN_KATALOG_ADRESI } from './admin-api';

const FILM_ADRESI = `${ADMIN_KATALOG_ADRESI}/movies`;
export const YONETIM_FILM_SAYFA_BOYUTU = 20;

/** Film yönetimi uçları (gizliler dahil). Film oluşturma/güncelleme film formuyla (M7 alt görev 3) eklenir. */
@Injectable({ providedIn: 'root' })
export class YonetimFilmService {
  constructor(private readonly http: HttpClient) {}

  ara(filtre: YonetimFilmFiltresi): Observable<Sayfa<YonetimFilmOzeti>> {
    let parametreler = new HttpParams().set('page', filtre.sayfa).set('size', YONETIM_FILM_SAYFA_BOYUTU);
    if (filtre.arama) {
      parametreler = parametreler.set('q', filtre.arama);
    }
    return this.http.get<Sayfa<YonetimFilmOzeti>>(FILM_ADRESI, { params: parametreler, context: sessizBaglam() });
  }

  /** Gizli film halka açık listelerde görünmez ve yeni seans eklenemez; mevcut seanslar ve biletler etkilenmez. */
  gorunurlukDegistir(filmId: number, gorunur: boolean): Observable<unknown> {
    return this.http.patch(`${FILM_ADRESI}/${filmId}/visibility`, { visible: gorunur }, { context: sessizBaglam() });
  }
}
