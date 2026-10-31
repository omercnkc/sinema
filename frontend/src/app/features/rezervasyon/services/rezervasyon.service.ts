import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { Rezervasyon, TutmaIstegi } from '../models/rezervasyon.models';

const REZERVASYON_ADRESI = `${environment.apiTabanAdresi}/bookings/reservations`;

/** Kullanıcının rezervasyon işlemleri (giriş gerekir). Hataları sayfa kendisi gösterir; istekler sessiz. */
@Injectable({ providedIn: 'root' })
export class RezervasyonService {
  constructor(private readonly http: HttpClient) {}

  /** Koltukları tutar (hepsi ya da hiçbiri, §4.3); başarıda PENDING_PAYMENT rezervasyon döner. */
  tut(istek: TutmaIstegi): Observable<Rezervasyon> {
    return this.http.post<Rezervasyon>(REZERVASYON_ADRESI, istek, { context: sessizBaglam() });
  }

  /** Yalnızca sahibine; başkasının rezervasyonu 404 (varlığı sızdırılmaz). */
  getir(rezervasyonId: number): Observable<Rezervasyon> {
    return this.http.get<Rezervasyon>(`${REZERVASYON_ADRESI}/${rezervasyonId}`, { context: sessizBaglam() });
  }

  /** Ödenmemiş tutmayı bırakır (K40); koltuklar hemen boşalır. Terk korumasına (K29) sayılır. */
  birak(rezervasyonId: number): Observable<void> {
    return this.http.delete<void>(`${REZERVASYON_ADRESI}/${rezervasyonId}`, { context: sessizBaglam() });
  }
}
