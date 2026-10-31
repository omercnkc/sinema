import { Injectable } from '@angular/core';
import {
  HttpContextToken,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpStatusCode,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { OTURUM_UCLARI, OturumService } from './oturum.service';

/** Yenilemeden sonra tekrarlanan istek: ikinci 401'de döngüye girilmez. */
const YENILEME_SONRASI = new HttpContextToken<boolean>(() => false);

/**
 * Kendi API'mize giden isteklere access token ekler (TMDB gibi dış adreslere asla). Token'lı istek 401 alırsa
 * (access token 15 dk'da dolar) bir kez yenileyip isteği tekrarlar; aynı anda gelen 401'ler tek yenilemeyi bekler
 * (OturumService.yenile paylaşımlı). Yenileme de başarısızsa oturum kapanır ve kullanıcı giriş sayfasına gider.
 */
@Injectable()
export class OturumInterceptor implements HttpInterceptor {
  constructor(private readonly oturumService: OturumService) {}

  intercept(istek: HttpRequest<unknown>, sonraki: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.oturumService.erisimTokeni;
    if (!token || !OturumInterceptor.tokenEklenmeli(istek)) {
      return sonraki.handle(istek);
    }
    return sonraki.handle(OturumInterceptor.tokenli(istek, token)).pipe(
      catchError((hata: unknown) => {
        if (!OturumInterceptor.yenilenmeli(istek, hata)) {
          return throwError(() => hata);
        }
        return this.oturumService.yenile().pipe(
          catchError(() => {
            this.oturumService.oturumSonaErdi();
            return throwError(() => hata);
          }),
          switchMap((yeniToken) =>
            sonraki.handle(OturumInterceptor.tokenli(istek, yeniToken).clone({
              context: istek.context.set(YENILEME_SONRASI, true),
            })),
          ),
        );
      }),
    );
  }

  private static tokenEklenmeli(istek: HttpRequest<unknown>): boolean {
    return istek.url.startsWith(environment.apiTabanAdresi) && !OTURUM_UCLARI.includes(istek.url);
  }

  private static yenilenmeli(istek: HttpRequest<unknown>, hata: unknown): boolean {
    return hata instanceof HttpErrorResponse
      && hata.status === HttpStatusCode.Unauthorized
      && !istek.context.get(YENILEME_SONRASI);
  }

  private static tokenli(istek: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    return istek.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
}
