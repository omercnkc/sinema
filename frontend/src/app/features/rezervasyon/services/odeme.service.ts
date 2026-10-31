import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { Odeme } from '../models/odeme.models';

const ODEME_ADRESI = `${environment.apiTabanAdresi}/payments`;

/** Ödeme (K39: iyzico sandbox ya da yerelde MOCK). Hatalar sayfada gösterilir; istekler sessiz. */
@Injectable({ providedIn: 'root' })
export class OdemeService {
  constructor(private readonly http: HttpClient) {}

  /**
   * Ödemeyi başlatır: sunucu tutarı booking'den alır, tutma süresini bir kez uzatır (K32) ve ödeme sayfası adresini
   * döner. Aynı rezervasyon için tekrar çağrılırsa geçerli sayfa yeniden verilir.
   */
  baslat(rezervasyonId: number): Observable<Odeme> {
    return this.http.post<Odeme>(ODEME_ADRESI, { reservationId: rezervasyonId }, { context: sessizBaglam() });
  }

  /** Rezervasyonun ödemesi (iade bilgisi için). */
  rezervasyonunOdemesi(rezervasyonId: number): Observable<Odeme> {
    return this.http.get<Odeme>(`${ODEME_ADRESI}/by-reservation/${rezervasyonId}`, { context: sessizBaglam() });
  }
}
