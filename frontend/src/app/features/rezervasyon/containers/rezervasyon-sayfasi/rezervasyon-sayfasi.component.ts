import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, EMPTY, Observable, Subject, throwError } from 'rxjs';
import { catchError, distinctUntilChanged, exhaustMap, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { SunucuSaati } from '../../../../core/zaman/sunucu-saati.service';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { GorunenDurum, gorunenDurum, Rezervasyon } from '../../models/rezervasyon.models';
import { RezervasyonService } from '../../services/rezervasyon.service';

const REZERVASYON_YOK = new UygulamaHatasi(HttpStatusCode.NotFound, 'RESERVATION_NOT_FOUND', 'Rezervasyon bulunamadı.');
const BEKLENMEYEN_HATA = 'İşlem tamamlanamadı. Lütfen tekrar deneyin.';
/** Bu kodlarda rezervasyonun durumu değişmiştir (onaylandı / süresi doldu); sayfa yeniden yüklenir. */
const DURUM_DEGISTI_KODLARI = new Set(['RESERVATION_ALREADY_CONFIRMED', 'RESERVATION_NOT_ACTIVE']);

interface IslemDurumu {
  readonly birakiliyor: boolean;
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
 * (bilet, alt görev 6). Ödeme alt görev 4'te.
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
  private readonly islem$ = new BehaviorSubject<IslemDurumu>({ birakiliyor: false, hata: null });
  private readonly vazgec$ = new Subject<Rezervasyon>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    private readonly rezervasyonService: RezervasyonService,
    private readonly sunucuSaati: SunucuSaati,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
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

    this.vazgec$.pipe(
      exhaustMap((rezervasyon) => this.birak(rezervasyon)),
      takeUntil(this.yikim$),
    ).subscribe((rezervasyon) => {
      this.bildirimService.basari('Koltuklar bırakıldı.');
      this.router.navigate(['/seans', rezervasyon.showtimeId]);
    });
  }

  sureDoldu(): void {
    this.sureBitti$.next(true);
  }

  vazgec(rezervasyon: Rezervasyon): void {
    this.vazgec$.next(rezervasyon);
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

  /** Başarıda rezervasyonu (seansa dönmek için) verir; hatada mesaj gösterir, akış sürer. */
  private birak(rezervasyon: Rezervasyon): Observable<Rezervasyon> {
    this.islem$.next({ birakiliyor: true, hata: null });
    return this.rezervasyonService.birak(rezervasyon.reservationId).pipe(
      map(() => rezervasyon),
      catchError((hata: unknown) => {
        const uygulamaHatasi = hata instanceof UygulamaHatasi ? hata : null;
        this.islem$.next({ birakiliyor: false, hata: uygulamaHatasi?.mesaj ?? BEKLENMEYEN_HATA });
        if (uygulamaHatasi && DURUM_DEGISTI_KODLARI.has(uygulamaHatasi.kod)) {
          this.yenile$.next();
        }
        return EMPTY;
      }),
      finalize(() => this.islem$.next({ ...this.islem$.value, birakiliyor: false })),
    );
  }
}
