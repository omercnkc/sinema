import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, EMPTY, merge, Observable, of, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, exhaustMap, finalize, map, shareReplay, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { gunEkle, istanbulGunu } from '../../../../core/zaman/is-gunu';
import { sessizYukleme, YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { SeansGuncellemeIstegi, YonetimSeansOzeti } from '../../models/seans-yonetimi.models';
import { YonetimSalon, YonetimSube } from '../../models/sube-yonetimi.models';
import { SeansYonetimiService } from '../../services/seans-yonetimi.service';
import { SubeYonetimiService } from '../../services/sube-yonetimi.service';

const PARAMETRE = { salon: 'salon', gun: 'gun' } as const;
const TARIH_DESENI = /^\d{4}-\d{2}-\d{2}$/;

interface Secim {
  readonly sube: YonetimSube | null;
  readonly salon: YonetimSalon | null;
  readonly gun: string;
}

interface SeansYonetimiGorunumu {
  readonly subeler: readonly YonetimSube[];
  readonly secim: Secim;
  /** Salon seçilmemişse (hiç salon yoksa) null. */
  readonly seanslar: YuklemeDurumu<YonetimSeansOzeti[]> | null;
}

/** Adres çubuğundaki salon ve gün; geçersizse ilk salon ve bugün (İstanbul). */
function secimKur(subeler: readonly YonetimSube[], parametreler: ParamMap, bugun: string): Secim {
  const salonId = Number(parametreler.get(PARAMETRE.salon));
  const gunParametresi = parametreler.get(PARAMETRE.gun) ?? '';
  const sube = subeler.find((s) => s.halls.some((h) => h.id === salonId)) ?? subeler.find((s) => s.halls.length > 0) ?? null;
  const salon = sube?.halls.find((h) => h.id === salonId) ?? sube?.halls[0] ?? null;
  return { sube, salon, gun: TARIH_DESENI.test(gunParametresi) ? gunParametresi : bugun };
}

function ayniSecim(onceki: Secim, simdiki: Secim): boolean {
  return onceki.salon?.id === simdiki.salon?.id && onceki.gun === simdiki.gun;
}

/**
 * /admin/seanslar (smart): şube → salon → gün çizelgesi, seans ekleme ve fiyat/ofset düzenleme. Seçim adres
 * çubuğundadır (`?salon=&gun=`). Düzeni yayımlanmamış salona seans eklenemez (sunucu kuralı); bu durumda düzen
 * sayfasına yönlendirilir. Kayıttan sonra çizelge arkada sessizce yenilenir.
 */
@Component({
  selector: 'app-seans-yonetimi-sayfasi',
  templateUrl: './seans-yonetimi-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeansYonetimiSayfasiComponent implements OnDestroy {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly subeDurumu$: Observable<YuklemeDurumu<YonetimSube[]>>;
  readonly gorunum$: Observable<SeansYonetimiGorunumu>;
  eklemeAcik = false;
  duzenlenenId: number | null = null;
  gonderiliyor = false;
  kayitHatasi: unknown = null;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);
  private readonly sessizYenile$ = new Subject<void>();
  private readonly guncelle$ = new Subject<{ readonly seans: YonetimSeansOzeti; readonly istek: SeansGuncellemeIstegi }>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    subeService: SubeYonetimiService,
    private readonly seansService: SeansYonetimiService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    private readonly rota: ActivatedRoute,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    this.subeDurumu$ = this.tekrarDene$.pipe(
      switchMap(() => yuklemeDurumu(subeService.listele())),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const subeler$ = this.subeDurumu$.pipe(map((durum) => durum.veri), switchMap((veri) => (veri ? of(veri) : EMPTY)));
    const secim$ = combineLatest([subeler$, rota.queryParamMap]).pipe(
      map(([subeler, parametreler]) => secimKur(subeler, parametreler, istanbulGunu(new Date()))),
      distinctUntilChanged(ayniSecim),
      tap(() => this.duzenlemeyiKapat()),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const seanslar$ = secim$.pipe(
      switchMap((secim) => {
        const salon = secim.salon;
        if (!salon) {
          return of(null);
        }
        const istek = (): Observable<YonetimSeansOzeti[]> => seansService.salonunSeanslari(salon.id, secim.gun);
        return merge(yuklemeDurumu(istek()), this.sessizYenile$.pipe(switchMap(() => sessizYukleme(istek()))));
      }),
    );
    this.gorunum$ = combineLatest([subeler$, secim$, seanslar$]).pipe(
      map(([subeler, secim, seanslar]) => ({ subeler, secim, seanslar })),
    );

    this.guncelle$.pipe(
      tap(() => this.gonderimBasladi()),
      exhaustMap(({ seans, istek }) => seansService.guncelle(seans.id, istek).pipe(
        tap(() => this.kaydedildi(`${seans.movieTitle} seansı güncellendi.`)),
        catchError((hata: unknown) => {
          this.kayitHatasi = hata;
          return EMPTY;
        }),
        finalize(() => this.gonderimBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  subeSecildi(subeler: readonly YonetimSube[], subeId: string): void {
    const ilkSalon = subeler.find((s) => s.id === Number(subeId))?.halls[0];
    this.secimiGuncelle({ [PARAMETRE.salon]: ilkSalon?.id ?? null });
  }

  salonSecildi(salonId: string): void {
    this.secimiGuncelle({ [PARAMETRE.salon]: salonId });
  }

  gunSecildi(gun: string): void {
    if (TARIH_DESENI.test(gun)) {
      this.secimiGuncelle({ [PARAMETRE.gun]: gun });
    }
  }

  gunKaydir(gun: string, fark: number): void {
    this.gunSecildi(gunEkle(gun, fark));
  }

  eklemeyiAc(): void {
    this.eklemeAcik = true;
    this.duzenlenenId = null;
  }

  eklemeyiKapat(): void {
    this.eklemeAcik = false;
  }

  seansEklendi(): void {
    this.bildirimService.basari('Seans eklendi.');
    this.sessizYenile$.next();
  }

  duzenle(seans: YonetimSeansOzeti): void {
    if (!this.gonderiliyor) {
      this.duzenlenenId = seans.id;
      this.kayitHatasi = null;
      this.eklemeAcik = false;
    }
  }

  duzenlemeyiKapat(): void {
    this.duzenlenenId = null;
    this.kayitHatasi = null;
  }

  guncelle(seans: YonetimSeansOzeti, istek: SeansGuncellemeIstegi): void {
    this.guncelle$.next({ seans, istek });
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  subeIzle(_: number, sube: YonetimSube): number {
    return sube.id;
  }

  salonIzle(_: number, salon: YonetimSalon): number {
    return salon.id;
  }

  seansIzle(_: number, seans: YonetimSeansOzeti): number {
    return seans.id;
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private secimiGuncelle(parametreler: Params): void {
    this.router.navigate([], { relativeTo: this.rota, queryParams: parametreler, queryParamsHandling: 'merge' });
  }

  private gonderimBasladi(): void {
    this.gonderiliyor = true;
    this.kayitHatasi = null;
  }

  private kaydedildi(mesaj: string): void {
    this.duzenlenenId = null;
    this.bildirimService.basari(mesaj);
    this.sessizYenile$.next();
  }

  private gonderimBitti(): void {
    this.gonderiliyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }
}
