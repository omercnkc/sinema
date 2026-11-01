import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { GocRaporu, GocSonucu } from '../models/duzen-gocu.models';
import { ADMIN_REZERVASYON_ADRESI } from './admin-api';

const GOC_ADRESI = `${ADMIN_REZERVASYON_ADRESI}/layout-migrations`;

/**
 * Düzen yayımlama = bilet göçü (K35, K51; booking yönetir, catalog'da yayımlatır). Önizleme hiçbir şey yazmaz;
 * uygulama salonu kilitler, biletleri taşır ve taslağı yayımlar.
 */
@Injectable({ providedIn: 'root' })
export class DuzenGocuService {
  constructor(private readonly http: HttpClient) {}

  onizle(salonId: number, duzenId: number): Observable<GocRaporu> {
    return this.http.post<GocRaporu>(`${GOC_ADRESI}/preview`, { hallId: salonId, toLayoutId: duzenId }, { context: sessizBaglam() });
  }

  uygula(salonId: number, duzenId: number): Observable<GocSonucu> {
    return this.http.post<GocSonucu>(GOC_ADRESI, { hallId: salonId, toLayoutId: duzenId }, { context: sessizBaglam() });
  }
}
