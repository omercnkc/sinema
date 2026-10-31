import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { DolulukCevabi, SeansDuzeni } from '../models/koltuk-haritasi.models';

const API = environment.apiTabanAdresi;

/**
 * Koltuk haritasının iki kaynağı (§3.4-2): düzen catalog'dan (değişmez, ETag ile tarayıcı önbelleği), doluluk
 * booking'den (misafire açık, önbelleksiz). İkisini sayfa birleştirir. Hatalar sayfada gösterilir; istekler sessiz.
 */
@Injectable({ providedIn: 'root' })
export class KoltukHaritasiService {
  constructor(private readonly http: HttpClient) {}

  duzen(seansId: number): Observable<SeansDuzeni> {
    return this.http.get<SeansDuzeni>(`${API}/catalog/showtimes/${seansId}/layout`, { context: sessizBaglam() });
  }

  doluluk(seansId: number): Observable<DolulukCevabi> {
    return this.http.get<DolulukCevabi>(`${API}/bookings/showtimes/${seansId}/seat-status`, { context: sessizBaglam() });
  }
}
