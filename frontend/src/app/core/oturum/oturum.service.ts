import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, distinctUntilChanged, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { BildirimService } from '../bildirim/bildirim.service';
import { SESSIZ_HATA } from '../hata/hata.interceptor';
import { GirisBilgileri, Kullanici, TokenCevabi } from './oturum.models';

const AUTH_ADRESI = `${environment.apiTabanAdresi}/auth`;

/** Refresh cookie'si yalnızca bu uçlara gider (Path=/api/v1/auth); Bearer token eklenmez, 401'de yenileme denenmez. */
export const OTURUM_UCLARI: readonly string[] = ['login', 'register', 'refresh', 'logout'].map((uc) => `${AUTH_ADRESI}/${uc}`);

interface OturumDurumu {
  readonly erisimTokeni: string | null;
  readonly kullanici: Kullanici | null;
}

const BOS_OTURUM: OturumDurumu = { erisimTokeni: null, kullanici: null };

/**
 * Oturumun tek sahibi (planlama §8.4, K36). Access token yalnızca bellekte tutulur (localStorage yok: XSS ile
 * çalınamaz); sayfa yenilenince kaybolur ve açılışta HttpOnly refresh cookie'si ile sessizce geri alınır.
 * Durum private BehaviorSubject'te, dışarıya yalnızca Observable açılır (rules §6.4).
 */
@Injectable({ providedIn: 'root' })
export class OturumService {
  private readonly durum = new BehaviorSubject<OturumDurumu>(BOS_OTURUM);
  /** Süren yenileme isteği: aynı anda 401 alan istekler tek refresh'i bekler (refresh token rotasyonlu, §5.2). */
  private yenileme$: Observable<string> | null = null;

  readonly kullanici$: Observable<Kullanici | null> = this.durum.pipe(
    map((durum) => durum.kullanici),
    distinctUntilChanged(),
  );

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly bildirimService: BildirimService,
  ) {}

  /** Interceptor'ın her istekte okuduğu anlık değer. */
  get erisimTokeni(): string | null {
    return this.durum.value.erisimTokeni;
  }

  get kullanici(): Kullanici | null {
    return this.durum.value.kullanici;
  }

  girisYap(bilgiler: GirisBilgileri): Observable<Kullanici> {
    return this.http.post<TokenCevabi>(`${AUTH_ADRESI}/login`, bilgiler, { withCredentials: true }).pipe(
      switchMap((cevap) => this.kullaniciyiYukle(cevap.accessToken)),
    );
  }

  /**
   * Refresh cookie'si ile yeni access token alır. Eşzamanlı çağrılar aynı isteği paylaşır; başarısızsa oturum
   * temizlenir ve hata çağırana iletilir.
   */
  yenile(): Observable<string> {
    if (!this.yenileme$) {
      this.yenileme$ = this.http
        .post<TokenCevabi>(`${AUTH_ADRESI}/refresh`, null, { withCredentials: true, context: OturumService.sessiz() })
        .pipe(
          map((cevap) => cevap.accessToken),
          tap({
            next: (token) => this.durum.next({ ...this.durum.value, erisimTokeni: token }),
            error: () => this.durum.next(BOS_OTURUM),
          }),
          finalize(() => (this.yenileme$ = null)),
          shareReplay({ bufferSize: 1, refCount: true }),
        );
    }
    return this.yenileme$;
  }

  /** APP_INITIALIZER: önceki oturum varsa geri kurar; yoksa (ya da sunucuya ulaşılamazsa) misafir olarak devam edilir. */
  oturumuGeriYukle(): Observable<Kullanici | null> {
    return this.yenile().pipe(
      switchMap((token) => this.kullaniciyiYukle(token)),
      catchError(() => {
        this.durum.next(BOS_OTURUM);
        return of(null);
      }),
    );
  }

  /** Sunucu hatası olsa bile yerel oturum kapanır (access token en geç 15 dk'da kendiliğinden geçersizleşir). */
  cikisYap(): Observable<void> {
    return this.http.post<void>(`${AUTH_ADRESI}/logout`, null, { withCredentials: true, context: OturumService.sessiz() })
      .pipe(
        catchError(() => of(undefined)),
        map(() => undefined),
        finalize(() => this.durum.next(BOS_OTURUM)),
      );
  }

  /** Yenileme de başarısız oldu (refresh süresi doldu ya da iptal edildi): kullanıcı yeniden giriş yapar. */
  oturumSonaErdi(): void {
    this.durum.next(BOS_OTURUM);
    this.bildirimService.bilgi('Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.');
    this.router.navigate(['/giris'], { queryParams: { donus: this.router.url } });
  }

  private kullaniciyiYukle(token: string): Observable<Kullanici> {
    this.durum.next({ ...this.durum.value, erisimTokeni: token });
    return this.http.get<Kullanici>(`${AUTH_ADRESI}/me`).pipe(
      tap((kullanici) => this.durum.next({ erisimTokeni: token, kullanici })),
      catchError((hata: unknown) => {
        this.durum.next(BOS_OTURUM);
        return throwError(() => hata);
      }),
    );
  }

  private static sessiz(): HttpContext {
    return new HttpContext().set(SESSIZ_HATA, true);
  }
}
