import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of, timer } from 'rxjs';
import { catchError, exhaustMap, map, startWith, switchMap, take, takeWhile, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { OdemeDurumu } from '../../models/odeme.models';
import { Rezervasyon } from '../../models/rezervasyon.models';
import { OdemeService } from '../../services/odeme.service';
import { RezervasyonService } from '../../services/rezervasyon.service';

/** Yoklama: 2 sn arayla en çok 15 kez (~30 sn); 3DS/inceleme daha uzun sürerse elle yenilenir. */
const YOKLAMA_ARALIGI_MS = 2000;
const EN_FAZLA_DENEME = 15;
const IADE_DURUMLARI: ReadonlySet<OdemeDurumu> = new Set<OdemeDurumu>(['REFUND_REQUIRED', 'REFUNDED']);

export type SonucTuru = 'KONTROL' | 'GECIKTI' | 'SURESI_DOLDU' | 'BASARISIZ' | 'ODEME_YOK' | 'ONAYLANDI';

export interface OdemeSonucuGorunumu {
  readonly tur: SonucTuru;
  readonly rezervasyon: Rezervasyon | null;
  /** Ödeme alındı ama bilet verilemedi (süre doldu): iade edildi/edilecek */
  readonly iade: boolean;
}

const KONTROL: OdemeSonucuGorunumu = { tur: 'KONTROL', rezervasyon: null, iade: false };
const ODEME_YOK: OdemeSonucuGorunumu = { tur: 'ODEME_YOK', rezervasyon: null, iade: false };

/**
 * /odeme/sonuc (smart): ödeme sağlayıcısından dönüş (payment callback'i 302 ile gönderir: `?reservationId=` ya da
 * `?error=PAYMENT_NOT_FOUND`). Sonuç sunucuda belirlenir; sayfa rezervasyonu kısa aralıkla yoklar. Onaylanınca bilete
 * geçilir; süresi dolmuşsa ve ödeme alınmışsa iade bilgisi verilir.
 */
@Component({
  selector: 'app-odeme-sonucu-sayfasi',
  templateUrl: './odeme-sonucu-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OdemeSonucuSayfasiComponent {
  readonly gorunum$: Observable<OdemeSonucuGorunumu>;
  private readonly yenile$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly rezervasyonService: RezervasyonService,
    private readonly odemeService: OdemeService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    rota: ActivatedRoute,
  ) {
    this.gorunum$ = combineLatest([rota.queryParamMap, this.yenile$]).pipe(
      switchMap(([parametreler]) => {
        const id = OdemeSonucuSayfasiComponent.rezervasyonId(parametreler);
        return id === null ? of(ODEME_YOK) : this.yokla(id).pipe(startWith(KONTROL));
      }),
      tap((gorunum) => gorunum.tur === 'ONAYLANDI' && gorunum.rezervasyon && this.bileteGec(gorunum.rezervasyon)),
    );
  }

  yenile(): void {
    this.yenile$.next();
  }

  private yokla(id: number): Observable<OdemeSonucuGorunumu> {
    return timer(0, YOKLAMA_ARALIGI_MS).pipe(
      take(EN_FAZLA_DENEME),
      exhaustMap(() => this.rezervasyonService.getir(id).pipe(catchError(() => of(null)))),
      map((rezervasyon, sira) => ({ rezervasyon, sonDeneme: sira === EN_FAZLA_DENEME - 1 })),
      takeWhile(({ rezervasyon }) => !rezervasyon || rezervasyon.status === 'PENDING_PAYMENT', true),
      switchMap(({ rezervasyon, sonDeneme }) => this.sonuc(rezervasyon, sonDeneme)),
    );
  }

  private sonuc(rezervasyon: Rezervasyon | null, sonDeneme: boolean): Observable<OdemeSonucuGorunumu> {
    if (!rezervasyon || rezervasyon.status === 'PENDING_PAYMENT') {
      return of({ tur: sonDeneme ? 'GECIKTI' : 'KONTROL', rezervasyon, iade: false });
    }
    if (rezervasyon.status === 'CONFIRMED') {
      return of({ tur: 'ONAYLANDI', rezervasyon, iade: false });
    }
    const tur: SonucTuru = rezervasyon.status === 'FAILED' ? 'BASARISIZ' : 'SURESI_DOLDU';
    // Bilet verilemediyse ödemenin iadeye düşüp düşmediği sorulur (bilgi yoksa iade denmez)
    return this.odemeService.rezervasyonunOdemesi(rezervasyon.reservationId).pipe(
      map((odeme) => ({ tur, rezervasyon, iade: IADE_DURUMLARI.has(odeme.status) })),
      catchError(() => of({ tur, rezervasyon, iade: false })),
    );
  }

  private bileteGec(rezervasyon: Rezervasyon): void {
    this.bildirimService.basari('Ödemeniz alındı, biletiniz hazır.');
    this.router.navigate(['/rezervasyonlarim', rezervasyon.reservationId], { replaceUrl: true });
  }

  private static rezervasyonId(parametreler: ParamMap): number | null {
    const id = Number(parametreler.get('reservationId'));
    return !parametreler.has('error') && Number.isInteger(id) && id > 0 ? id : null;
  }
}
