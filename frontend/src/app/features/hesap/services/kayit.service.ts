import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { KayitCevabi, KayitIstegi } from '../models/kayit.models';

/** Üye kaydı. Kayıt oturum açmaz (planlama §5.3); kullanıcı ardından giriş yapar. Hataları sayfa formda gösterir. */
@Injectable({ providedIn: 'root' })
export class KayitService {
  constructor(private readonly http: HttpClient) {}

  kayitOl(istek: KayitIstegi): Observable<KayitCevabi> {
    return this.http.post<KayitCevabi>(`${environment.apiTabanAdresi}/auth/register`, istek, { context: sessizBaglam() });
  }
}
