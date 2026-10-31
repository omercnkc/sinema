import { ChangeDetectionStrategy, Component, Inject, Input, LOCALE_ID } from '@angular/core';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, ReplaySubject } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, switchMap } from 'rxjs/operators';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { IsGunu, istanbulGunleri } from '../../../../core/zaman/is-gunu';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { SeansOzeti, Sube, SubeSeanslari, subeyeGoreGrupla } from '../../models/seans.models';
import { SeansService } from '../../services/seans.service';

/** Örnek veri 7 günlük seans üretir (M2); gün seçici de bir haftayı gösterir. */
const GOSTERILEN_GUN_SAYISI = 7;
const PARAMETRE = { gun: 'gun', sube: 'sube' } as const;

interface SeansSecimi {
  readonly gun: string;
  readonly subeId: number | null;
}

interface SeansGorunumu {
  readonly secim: SeansSecimi;
  readonly durum: YuklemeDurumu<SubeSeanslari[]>;
}

/**
 * Film detayındaki seans seçimi (smart): gün (İstanbul günü) ve şube filtresi adres çubuğunda (`?gun=&sube=`), seanslar
 * şube başına gruplu. Satışı kapanmış seans gösterilir ama seçilemez. Seansa tıklayınca koltuk seçimi (`/seans/:id`).
 */
@Component({
  selector: 'app-film-seanslari',
  templateUrl: './film-seanslari.component.html',
  styleUrls: ['./film-seanslari.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmSeanslariComponent {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly gunler: readonly IsGunu[];
  readonly subeler$: Observable<readonly Sube[]>;
  readonly gorunum$: Observable<SeansGorunumu>;

  private readonly filmId$ = new ReplaySubject<number>(1);
  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);

  @Input() set filmId(deger: number) {
    this.filmId$.next(deger);
  }

  constructor(
    seansService: SeansService,
    private readonly router: Router,
    private readonly rota: ActivatedRoute,
    @Inject(LOCALE_ID) yerel: string,
  ) {
    this.gunler = istanbulGunleri(new Date(), GOSTERILEN_GUN_SAYISI, yerel);
    this.subeler$ = seansService.subeler();
    const secim$ = rota.queryParamMap.pipe(
      map((parametreler) => this.secimOku(parametreler)),
      distinctUntilChanged((onceki, simdiki) => onceki.gun === simdiki.gun && onceki.subeId === simdiki.subeId),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const durum$ = combineLatest([this.filmId$.pipe(distinctUntilChanged()), secim$, this.tekrarDene$]).pipe(
      switchMap(([filmId, secim]) =>
        yuklemeDurumu(seansService.filminSeanslari(filmId, secim.gun, secim.subeId).pipe(map(subeyeGoreGrupla)))),
    );
    this.gorunum$ = combineLatest([secim$, durum$]).pipe(map(([secim, durum]) => ({ secim, durum })));
  }

  gunSecildi(gun: string): void {
    // Bugün varsayılandır; adreste yazılmaz
    this.secimiGuncelle({ [PARAMETRE.gun]: gun === this.gunler[0].tarih ? null : gun });
  }

  subeSecildi(subeId: string): void {
    this.secimiGuncelle({ [PARAMETRE.sube]: subeId || null });
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  gunIzle(_: number, gun: IsGunu): string {
    return gun.tarih;
  }

  subeIzle(_: number, sube: Sube): number {
    return sube.id;
  }

  grupIzle(_: number, grup: SubeSeanslari): number {
    return grup.subeId;
  }

  seansIzle(_: number, seans: SeansOzeti): number {
    return seans.id;
  }

  /** Adresteki gün gösterilen hafta dışındaysa (eski bağlantı) bugüne düşülür. */
  private secimOku(parametreler: ParamMap): SeansSecimi {
    const gun = parametreler.get(PARAMETRE.gun);
    const subeId = Number(parametreler.get(PARAMETRE.sube));
    return {
      gun: this.gunler.some((secenek) => secenek.tarih === gun) ? (gun as string) : this.gunler[0].tarih,
      subeId: Number.isInteger(subeId) && subeId > 0 ? subeId : null,
    };
  }

  private secimiGuncelle(parametreler: Params): void {
    // Gün/şube değişimi geçmişe yeni kayıt eklemez (geri tuşu film listesine döner)
    this.router.navigate([], { relativeTo: this.rota, queryParams: parametreler, queryParamsHandling: 'merge',
      replaceUrl: true });
  }
}
