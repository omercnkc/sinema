import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { SeansDetayi } from '../models/seans-detayi.models';

/** Seans başlık bilgisi (halka açık). Hata sayfada gösterilir; istek sessiz. */
@Injectable({ providedIn: 'root' })
export class SeansDetayService {
  constructor(private readonly http: HttpClient) {}

  getir(seansId: number): Observable<SeansDetayi> {
    return this.http.get<SeansDetayi>(`${environment.apiTabanAdresi}/catalog/showtimes/${seansId}`,
      { context: sessizBaglam() });
  }
}
