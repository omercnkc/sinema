import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { SalonIstegi, SubeIstegi, YonetimSalon, YonetimSube } from '../models/sube-yonetimi.models';
import { ADMIN_KATALOG_ADRESI } from './admin-api';

/** Şube ve salon yönetimi uçları. Silme yok (seanslar ve biletler bu kayıtlara bağlı). */
@Injectable({ providedIn: 'root' })
export class SubeYonetimiService {
  constructor(private readonly http: HttpClient) {}

  listele(): Observable<YonetimSube[]> {
    return this.http.get<YonetimSube[]>(`${ADMIN_KATALOG_ADRESI}/cinemas`, { context: sessizBaglam() });
  }

  subeOlustur(istek: SubeIstegi): Observable<YonetimSube> {
    return this.http.post<YonetimSube>(`${ADMIN_KATALOG_ADRESI}/cinemas`, istek, { context: sessizBaglam() });
  }

  subeGuncelle(subeId: number, istek: SubeIstegi): Observable<YonetimSube> {
    return this.http.put<YonetimSube>(`${ADMIN_KATALOG_ADRESI}/cinemas/${subeId}`, istek, { context: sessizBaglam() });
  }

  salonOlustur(subeId: number, istek: SalonIstegi): Observable<YonetimSalon> {
    return this.http.post<YonetimSalon>(`${ADMIN_KATALOG_ADRESI}/halls`, { cinemaId: subeId, ...istek },
      { context: sessizBaglam() });
  }

  salonGuncelle(salonId: number, istek: SalonIstegi): Observable<YonetimSalon> {
    return this.http.put<YonetimSalon>(`${ADMIN_KATALOG_ADRESI}/halls/${salonId}`, istek, { context: sessizBaglam() });
  }
}
