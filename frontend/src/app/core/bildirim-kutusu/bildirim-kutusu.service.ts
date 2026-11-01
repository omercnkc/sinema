import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NavigationEnd, Router } from '@angular/router';
import { EMPTY, merge, Observable, of, Subject, timer } from 'rxjs';
import { catchError, distinctUntilChanged, exhaustMap, filter, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Sayfa } from '../../shared/models/sayfa.models';
import { sessizBaglam } from '../hata/hata.interceptor';
import { OturumService } from '../oturum/oturum.service';
import { SekmeGorunurlugu } from '../zaman/sekme-gorunurlugu';
import { UygulamaBildirimi } from './bildirim-kutusu.models';

const BILDIRIM_ADRESI = `${environment.apiTabanAdresi}/bookings/notifications`;
/** Zilin açılır listesinde gösterilen son bildirim sayısı. */
export const ZIL_LISTE_BOYUTU = 10;

/**
 * Kullanıcının uygulama içi bildirimleri (K35: koltuk değişikliği). Toast bildirimlerinden (BildirimService) ayrı:
 * bunlar sunucuda saklanır. Okunmamış sayısı giriş yapmış kullanıcı için açılışta, sayfa değişiminde ve belirli
 * aralıkla yoklanır; sekme gizliyken durur. Hata zili bozmaz (son sayı kalır, genel uyarı çıkmaz).
 */
@Injectable({ providedIn: 'root' })
export class BildirimKutusuService {
  readonly okunmamisSayisi$: Observable<number>;

  private readonly yenile$ = new Subject<void>();

  constructor(
    private readonly http: HttpClient,
    oturumService: OturumService,
    sekmeGorunurlugu: SekmeGorunurlugu,
    router: Router,
  ) {
    const tetikleyiciler$ = merge(
      sekmeGorunurlugu.gorunur$.pipe(switchMap((gorunur) => (gorunur ? timer(0, environment.bildirimYenilemeMs) : EMPTY))),
      router.events.pipe(filter((olay) => olay instanceof NavigationEnd)),
      this.yenile$,
    );
    this.okunmamisSayisi$ = oturumService.kullanici$.pipe(
      switchMap((kullanici) => (kullanici ? tetikleyiciler$.pipe(exhaustMap(() => this.sayiyiGetir())) : of(0))),
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  /** En yeni bildirimler, okunmamışlar önce. */
  sonBildirimler(): Observable<Sayfa<UygulamaBildirimi>> {
    const parametreler = new HttpParams().set('size', ZIL_LISTE_BOYUTU);
    return this.http.get<Sayfa<UygulamaBildirimi>>(BILDIRIM_ADRESI, { params: parametreler, context: sessizBaglam() });
  }

  /** Okundu işaretler ve okunmamış sayısını hemen yeniler. */
  okundu(bildirimId: number): Observable<void> {
    return this.http.post<void>(`${BILDIRIM_ADRESI}/${bildirimId}/read`, null, { context: sessizBaglam() })
      .pipe(tap(() => this.yenile$.next()));
  }

  private sayiyiGetir(): Observable<number> {
    return this.http.get<{ count: number }>(`${BILDIRIM_ADRESI}/unread-count`, { context: sessizBaglam() }).pipe(
      map((cevap) => cevap.count),
      catchError(() => EMPTY),
    );
  }
}
