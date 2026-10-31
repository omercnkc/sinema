import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { sessizBaglam } from '../../core/hata/hata.interceptor';

/** `GET /tickets/{kod}`: yalnızca onaylı bilette döner; kişisel veri ve tutar yok (K30). */
export interface BiletDogrulamasi {
  readonly valid: boolean;
  readonly movieTitle: string;
  readonly cinemaName: string;
  readonly hallName: string;
  readonly startsAt: string;
  readonly seats: readonly string[];
}

/** Herkese açık bilet doğrulama (QR). Bilinmeyen/onaysız kod 404; gateway hız sınırı 429. */
@Injectable({ providedIn: 'root' })
export class BiletDogrulamaService {
  constructor(private readonly http: HttpClient) {}

  dogrula(kod: string): Observable<BiletDogrulamasi> {
    return this.http.get<BiletDogrulamasi>(`${environment.apiTabanAdresi}/tickets/${encodeURIComponent(kod)}`,
      { context: sessizBaglam() });
  }
}
