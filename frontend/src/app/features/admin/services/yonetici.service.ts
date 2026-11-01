import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { YoneticiCevabi, YoneticiIstegi } from '../models/yonetici.models';
import { ADMIN_AUTH_ADRESI } from './admin-api';

/** Yeni yönetici hesabı açar (K36: yöneticiyi yalnızca mevcut yönetici ekler; herkese açık kayıt hep USER'dır). */
@Injectable({ providedIn: 'root' })
export class YoneticiService {
  constructor(private readonly http: HttpClient) {}

  ekle(istek: YoneticiIstegi): Observable<YoneticiCevabi> {
    return this.http.post<YoneticiCevabi>(`${ADMIN_AUTH_ADRESI}/admins`, istek, { context: sessizBaglam() });
  }
}
