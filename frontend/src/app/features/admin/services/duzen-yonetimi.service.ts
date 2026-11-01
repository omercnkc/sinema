import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { DuzenIstegi } from '../models/duzen-editoru.models';
import { DuzenCevabi, DuzenOzeti } from '../models/duzen-yonetimi.models';
import { ADMIN_KATALOG_ADRESI } from './admin-api';

/**
 * Salon düzeni sürümleri: salon başına tek taslak. Yayımlama bilet göçüyle yapılır (DuzenGocuService, K51):
 * başlamamış seanslar yeni sürüme geçer, başlamış seanslar eski sürümde kalır.
 */
@Injectable({ providedIn: 'root' })
export class DuzenYonetimiService {
  constructor(private readonly http: HttpClient) {}

  listele(salonId: number): Observable<DuzenOzeti[]> {
    return this.http.get<DuzenOzeti[]>(this.adres(salonId), { context: sessizBaglam() });
  }

  getir(salonId: number, duzenId: number): Observable<DuzenCevabi> {
    return this.http.get<DuzenCevabi>(`${this.adres(salonId)}/${duzenId}`, { context: sessizBaglam() });
  }

  taslakOlustur(salonId: number, istek: DuzenIstegi): Observable<DuzenCevabi> {
    return this.http.post<DuzenCevabi>(this.adres(salonId), istek, { context: sessizBaglam() });
  }

  taslakGuncelle(salonId: number, duzenId: number, istek: DuzenIstegi): Observable<DuzenCevabi> {
    return this.http.put<DuzenCevabi>(`${this.adres(salonId)}/${duzenId}`, istek, { context: sessizBaglam() });
  }

  private adres(salonId: number): string {
    return `${ADMIN_KATALOG_ADRESI}/halls/${salonId}/layouts`;
  }
}
