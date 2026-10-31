import { Injectable } from '@angular/core';
import {
  HttpContextToken,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BildirimService } from '../bildirim/bildirim.service';
import { hataMesaji } from './hata-mesajlari';
import { ProblemDetayi, UygulamaHatasi } from './uygulama-hatasi';

// problem+json'un her hatada bulunan alanları; geri kalanlar hataya özel ek alanlardır (ör. unavailableCellIds)
const ORTAK_ALANLAR = new Set(['type', 'title', 'status', 'detail', 'instance', 'code', 'correlationId', 'timestamp',
  'errors', 'retryAfterSeconds']);

/** İsteği yapan kendi gösterecekse (ör. ödeme sonucu yoklaması) genel bildirim çıkmasın. */
export const SESSIZ_HATA = new HttpContextToken<boolean>(() => false);

/**
 * Her HTTP hatasını UygulamaHatasi'na çevirir (tek yerde, rules §6.6). Kullanıcının sayfada karşılığı olmayan
 * hatalar (ağ, 5xx, 429) genel bildirim olarak gösterilir; iş hataları (400, 404, 409, 422) gösterimi bileşene
 * bırakılır (form alanı, harita yenileme...). 401 oturum yenilemesinin işidir (AuthInterceptor), burada gösterilmez.
 */
@Injectable()
export class HataInterceptor implements HttpInterceptor {
  constructor(private readonly bildirimService: BildirimService) {}

  intercept(istek: HttpRequest<unknown>, sonraki: HttpHandler): Observable<HttpEvent<unknown>> {
    return sonraki.handle(istek).pipe(
      catchError((hata: unknown) => {
        if (!(hata instanceof HttpErrorResponse)) {
          return throwError(() => hata);
        }
        const uygulamaHatasi = HataInterceptor.donustur(hata);
        if (HataInterceptor.genelBildirimMi(uygulamaHatasi.durum) && !istek.context.get(SESSIZ_HATA)) {
          this.bildirimService.hata(uygulamaHatasi.mesaj);
        }
        return throwError(() => uygulamaHatasi);
      }),
    );
  }

  private static donustur(hata: HttpErrorResponse): UygulamaHatasi {
    const problem = HataInterceptor.problemOku(hata.error);
    const kod = problem.code ?? `HTTP_${hata.status}`;
    const bekleme = HataInterceptor.beklemeSaniyesi(hata, problem);
    const ekAlanlar = Object.fromEntries(Object.entries(problem).filter(([alan]) => !ORTAK_ALANLAR.has(alan)));
    return new UygulamaHatasi(hata.status, kod, hataMesaji(hata.status, kod, problem.detail, bekleme),
      problem.errors ?? [], bekleme, problem.correlationId ?? null, ekAlanlar);
  }

  /** Gövde problem+json değilse (ağ hatası, ara katmandan gelen HTML) boş kabul edilir. */
  private static problemOku(govde: unknown): ProblemDetayi {
    return govde !== null && typeof govde === 'object' ? (govde as ProblemDetayi) : {};
  }

  /** Önce gövdedeki retryAfterSeconds, yoksa Retry-After başlığı (gateway rate limit). */
  private static beklemeSaniyesi(hata: HttpErrorResponse, problem: ProblemDetayi): number | null {
    if (typeof problem.retryAfterSeconds === 'number') {
      return problem.retryAfterSeconds;
    }
    const baslik = Number(hata.headers?.get('Retry-After'));
    return Number.isFinite(baslik) && baslik > 0 ? baslik : null;
  }

  private static genelBildirimMi(durum: number): boolean {
    return durum === 0 || durum === 429 || durum >= 500;
  }
}
