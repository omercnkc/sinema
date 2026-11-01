import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { SeansGuncellemeIstegi, SeansOlusturmaIstegi, YonetimSeansOzeti } from '../models/seans-yonetimi.models';
import { ADMIN_KATALOG_ADRESI } from './admin-api';

const SEANS_ADRESI = `${ADMIN_KATALOG_ADRESI}/showtimes`;

/** Seans çizelgesi, ekleme ve fiyat/ofset güncelleme. Silme ve iptal yok (K34). */
@Injectable({ providedIn: 'root' })
export class SeansYonetimiService {
  constructor(private readonly http: HttpClient) {}

  /** `gun` İstanbul günü (`YYYY-MM-DD`). */
  salonunSeanslari(salonId: number, gun: string): Observable<YonetimSeansOzeti[]> {
    const parametreler = new HttpParams().set('hallId', salonId).set('date', gun);
    return this.http.get<YonetimSeansOzeti[]>(SEANS_ADRESI, { params: parametreler, context: sessizBaglam() });
  }

  olustur(istek: SeansOlusturmaIstegi): Observable<unknown> {
    return this.http.post(SEANS_ADRESI, istek, { context: sessizBaglam() });
  }

  guncelle(seansId: number, istek: SeansGuncellemeIstegi): Observable<unknown> {
    return this.http.put(`${SEANS_ADRESI}/${seansId}`, istek, { context: sessizBaglam() });
  }
}
