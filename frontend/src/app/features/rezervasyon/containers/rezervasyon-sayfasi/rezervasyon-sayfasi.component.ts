import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, EMPTY, Observable, Subject, throwError } from 'rxjs';
import { catchError, distinctUntilChanged, exhaustMap, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { SayfaYonlendirici } from '../../../../core/yonlendirme/sayfa-yonlendirici.service';
import { SunucuSaati } from '../../../../core/zaman/sunucu-saati.service';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { GorunenDurum, gorunenDurum, Rezervasyon } from '../../models/rezervasyon.models';
import { OdemeService } from '../../services/odeme.service';
import { RezervasyonService } from '../../services/rezervasyon.service';

const REZERVASYON_YOK = new UygulamaHatasi(HttpStatusCode.NotFound, 'RESERVATION_NOT_FOUND', 'Rezervasyon bulunamadı.');
const BEKLENMEYEN_HATA = 'İşlem tamamlanamadı. Lütfen tekrar deneyin.';
const ODEME_SAYFASI_ACILAMADI = 'Ödeme sayfası açılamadı. Lütfen tekrar deneyin.';
/** Bu kodlarda rezervasyonun durumu değişmiştir (onaylandı / süresi doldu / ödendi); sayfa yeniden yüklenir. */
const DURUM_DEGISTI_KODLARI = new Set(['RESERVATION_ALREADY_CONFIRMED', 'RESERVATION_NOT_ACTIVE', 'RESERVATION_NOT_PAYABLE',
  'PAYMENT_ALREADY_COMPLETED', 'PAYMENT_CLOSED']);

type IslemTuru = 'ODE' | 'VAZGEC';

interface IslemDurumu {
  /** Süren işlem; biri sürerken diğeri başlamaz */
  readonly calisan: IslemTuru | null;
  readonly hata: string | null;
}

interface RezervasyonGorunumu {
  readonly yukleme: YuklemeDurumu<Rezervasyon>;
  readonly durum: GorunenDurum | null;
  readonly islem: IslemDurumu;
}

/**
 * /rezervasyonlarim/:id (smart, giriş gerekir). Durumuna göre: ödeme bekleyen tutma (koltuklar, tutar, sunucu saatine
 * göre geri sayım, vazgeç), süresi dolmuş/bırakılmış/başarısız işlem (koltuk seçimine dönüş) ya da onaylı işlem
 * (bilet, alt görev 6). "Öde" ödeme sayfasına tam sayfa yönlendirir (K41); dönüş /odeme/sonuc.
 */
@Component({
  selector: 'app-rezervasyon-sayfasi',
  templateUrl: './rezervasyon-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RezervasyonSayfasiComponent implements OnDestroy {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly gorunum$: Observable<RezervasyonGorunumu>;

  private readonly yenile$ = new BehaviorSubject<void>(undefined);
  /** Geri sayım sıfırlandı: sunucu job'ını beklemeden "süre doldu" gösterilir */
  private readonly sureBitti$ = new BehaviorSubject<boolean>(false);
  private readonly islem$ = new BehaviorSubject<IslemDurumu>({ calisan: null, hata: null });
  private readonly islemIstegi$ = new Subject<{ tur: IslemTuru; rezervasyon: Rezervasyon }>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    private readonly rezervasyonService: RezervasyonService,
    private readonly sunucuSaati: SunucuSaati,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    private readonly odemeService: OdemeService,
    private readonly yonlendirici: SayfaYonlendirici,
    rota: ActivatedRoute,
  ) {
    const id$ = rota.paramMap.pipe(map((p) => Number(p.get('id'))), distinctUntilChanged());
    const yukleme$ = combineLatest([id$, this.yenile$]).pipe(
      tap(() => this.sureBitti$.next(false)),
      switchMap(([id]) => yuklemeDurumu(this.getir(id))),
    );
    this.gorunum$ = combineLatest([yukleme$, this.sureBitti$, this.islem$]).pipe(
      map(([yukleme, sureBitti, islem]) => ({ yukleme, islem, durum: this.durum(yukleme.veri, sureBitti) })),
    );

    // Öde ve vazgeç tek kuyrukta: biri sürerken ikinci tıklama (aynı ya da diğer düğme) yok sayılır
    this.islemIstegi$.pipe(
      exhaustMap(({ tur, rezervasyon }) => (tur === 'ODE' ? this.odemeyeGit(rezervasyon) : this.birak(rezervasyon))),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  ode(rezervasyon: Rezervasyon): void {
    this.islemIstegi$.next({ tur: 'ODE', rezervasyon });
  }

  sureDoldu(): void {
    this.sureBitti$.next(true);
  }

  vazgec(rezervasyon: Rezervasyon): void {
    this.islemIstegi$.next({ tur: 'VAZGEC', rezervasyon });
  }

  tekrarDene(): void {
    this.yenile$.next();
  }

  /** 404 (yok/başkasının) tekrar denemekle düzelmez. */
  tekrarDenenebilir(hata: UygulamaHatasi): boolean {
    return hata.durum !== HttpStatusCode.NotFound && hata.durum !== HttpStatusCode.BadRequest;
  }

  koltukIzle(_: number, koltuk: { cellId: number }): number {
    return koltuk.cellId;
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private getir(id: number): Observable<Rezervasyon> {
    if (!Number.isInteger(id) || id <= 0) {
      return throwError(() => REZERVASYON_YOK);
    }
    return this.rezervasyonService.getir(id).pipe(tap((r) => this.sunucuSaati.farkiGuncelle(r.serverTime)));
  }

  private durum(rezervasyon: Rezervasyon | null, sureBitti: boolean): GorunenDurum | null {
    if (!rezervasyon) {
      return null;
    }
    const durum = gorunenDurum(rezervasyon, this.sunucuSaati.simdi());
    return durum === 'ODEME_BEKLIYOR' && sureBitti ? 'SURESI_DOLDU' : durum;
  }

  /**
   * Ödeme sayfası adresini alır ve tarayıcıyı oraya gönderir. Başarıda sayfa terk edildiği için "çalışıyor" durumu
   * bırakılmaz (düğme kapalı kalır).
   */
  private odemeyeGit(rezervasyon: Rezervasyon): Observable<unknown> {
    this.islem$.next({ calisan: 'ODE', hata: null });
    return this.odemeService.baslat(rezervasyon.reservationId).pipe(
      tap((odeme) => {
        if (!odeme.paymentPageUrl || !this.yonlendirici.disAdreseGit(odeme.paymentPageUrl)) {
          this.islem$.next({ calisan: null, hata: ODEME_SAYFASI_ACILAMADI });
        }
      }),
      catchError((hata: unknown) => this.islemHatasi(hata)),
    );
  }

  /** Başarıda bildirim ve seansa dönüş. */
  private birak(rezervasyon: Rezervasyon): Observable<unknown> {
    this.islem$.next({ calisan: 'VAZGEC', hata: null });
    return this.rezervasyonService.birak(rezervasyon.reservationId).pipe(
      tap(() => {
        this.bildirimService.basari('Koltuklar bırakıldı.');
        this.router.navigate(['/seans', rezervasyon.showtimeId]);
      }),
      catchError((hata: unknown) => this.islemHatasi(hata)),
      finalize(() => this.islem$.next({ ...this.islem$.value, calisan: null })),
    );
  }

  /** Mesajı gösterir; durum değiştiyse (ödendi, süresi doldu) sayfayı yeniler. Akış sürer. */
  private islemHatasi(hata: unknown): Observable<never> {
    const uygulamaHatasi = hata instanceof UygulamaHatasi ? hata : null;
    this.islem$.next({ calisan: null, hata: uygulamaHatasi?.mesaj ?? BEKLENMEYEN_HATA });
    if (uygulamaHatasi && DURUM_DEGISTI_KODLARI.has(uygulamaHatasi.kod)) {
      this.yenile$.next();
    }
    return EMPTY;
  }
}
