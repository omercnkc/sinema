import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, merge, Observable, of, Subject } from 'rxjs';
import {
  catchError, debounceTime, distinctUntilChanged, filter, finalize, map, mergeMap, shareReplay, switchMap, takeUntil, tap,
} from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Sayfa } from '../../../../shared/models/sayfa.models';
import { sessizYukleme, YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { YonetimFilmFiltresi, YonetimFilmOzeti } from '../../models/yonetim-film.models';
import { YONETIM_FILM_SAYFA_BOYUTU, YonetimFilmService } from '../../services/yonetim-film.service';

const ARAMA_BEKLEMESI_MS = 300;
const PARAMETRE = { arama: 'ara', sayfa: 'sayfa' } as const;
const BEKLENMEYEN_HATA = 'Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.';

function filtreOku(parametreler: ParamMap): YonetimFilmFiltresi {
  const sayfaNo = Number(parametreler.get(PARAMETRE.sayfa));
  return {
    arama: parametreler.get(PARAMETRE.arama) ?? '',
    sayfa: Number.isInteger(sayfaNo) && sayfaNo > 1 ? sayfaNo - 1 : 0,
  };
}

function ayniFiltre(onceki: YonetimFilmFiltresi, simdiki: YonetimFilmFiltresi): boolean {
  return onceki.arama === simdiki.arama && onceki.sayfa === simdiki.sayfa;
}

interface FilmYonetimiGorunumu {
  readonly durum: YuklemeDurumu<Sayfa<YonetimFilmOzeti>>;
  /** Görünürlük isteği süren filmler: düğmeleri kapalıdır (aynı filme çift istek gitmez). */
  readonly islemdekiler: ReadonlySet<number>;
}

/**
 * /admin/filmler (smart): gizliler dahil film listesi, arama ve gizle/göster (K40). Filtre adres çubuğundadır
 * (katalog film listesiyle aynı kalıp). Görünürlük değişince liste arkada sessizce yenilenir, ekran boşalmaz.
 */
@Component({
  selector: 'app-film-yonetimi-sayfasi',
  templateUrl: './film-yonetimi-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmYonetimiSayfasiComponent implements OnDestroy {
  readonly sayfaBoyutu = YONETIM_FILM_SAYFA_BOYUTU;
  readonly aramaKontrolu = new FormControl('');
  readonly gorunum$: Observable<FilmYonetimiGorunumu>;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);
  private readonly sessizYenile$ = new Subject<void>();
  private readonly gorunurluk$ = new Subject<YonetimFilmOzeti>();
  private readonly islemdekiler$ = new BehaviorSubject<ReadonlySet<number>>(new Set());
  private readonly yikim$ = new Subject<void>();

  constructor(
    private readonly filmService: YonetimFilmService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    private readonly rota: ActivatedRoute,
  ) {
    const filtre$ = rota.queryParamMap.pipe(
      map(filtreOku),
      distinctUntilChanged(ayniFiltre),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const durum$ = combineLatest([filtre$, this.tekrarDene$]).pipe(
      switchMap(([filtre]) => merge(
        yuklemeDurumu(filmService.ara(filtre)),
        this.sessizYenile$.pipe(switchMap(() => sessizYukleme(filmService.ara(filtre)))),
      )),
    );
    this.gorunum$ = combineLatest([durum$, this.islemdekiler$]).pipe(
      map(([durum, islemdekiler]) => ({ durum, islemdekiler })),
    );

    filtre$.pipe(takeUntil(this.yikim$))
      .subscribe((filtre) => this.aramaKontrolu.setValue(filtre.arama, { emitEvent: false }));
    this.aramaKontrolu.valueChanges.pipe(
      map((deger: unknown) => String(deger ?? '').trim()),
      debounceTime(ARAMA_BEKLEMESI_MS),
      distinctUntilChanged(),
      takeUntil(this.yikim$),
    ).subscribe((arama) => this.filtreyiGuncelle({ arama, sayfa: 0 }));

    // Farklı filmlerin istekleri birbirini beklemez (mergeMap); aynı film işlemdeyken yeni istek süzülür
    this.gorunurluk$.pipe(
      filter((film) => !this.islemdekiler$.value.has(film.id)),
      mergeMap((film) => this.gorunurlukDegistir(film)),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  gorunurlukDegistirIstendi(film: YonetimFilmOzeti): void {
    this.gorunurluk$.next(film);
  }

  /** ng-bootstrap sayfalama 1'den sayar. */
  sayfaDegisti(sayfaNo: number): void {
    this.filtreyiGuncelle({ sayfa: sayfaNo - 1 });
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  filmIzle(_: number, film: YonetimFilmOzeti): number {
    return film.id;
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private gorunurlukDegistir(film: YonetimFilmOzeti): Observable<unknown> {
    const gorunur = !film.visible;
    this.islemIsaretle(film.id, true);
    return this.filmService.gorunurlukDegistir(film.id, gorunur).pipe(
      tap(() => {
        this.bildirimService.basari(gorunur
          ? `"${film.title}" artık müşterilere görünüyor.`
          : `"${film.title}" gizlendi. Mevcut seanslar ve satılmış biletler etkilenmez.`);
        this.sessizYenile$.next();
      }),
      catchError((hata: unknown) => {
        this.bildirimService.hata(hata instanceof UygulamaHatasi ? hata.mesaj : BEKLENMEYEN_HATA);
        return of(null);
      }),
      finalize(() => this.islemIsaretle(film.id, false)),
    );
  }

  private islemIsaretle(filmId: number, islemde: boolean): void {
    const kume = new Set(this.islemdekiler$.value);
    if (islemde) {
      kume.add(filmId);
    } else {
      kume.delete(filmId);
    }
    this.islemdekiler$.next(kume);
  }

  /** Yalnızca değişen parametreler yazılır; varsayılan değerler (boş, ilk sayfa) adresten silinir. */
  private filtreyiGuncelle(degisiklik: Partial<YonetimFilmFiltresi>): void {
    const parametreler: Params = {};
    if (degisiklik.arama !== undefined) {
      parametreler[PARAMETRE.arama] = degisiklik.arama || null;
    }
    if (degisiklik.sayfa !== undefined) {
      parametreler[PARAMETRE.sayfa] = degisiklik.sayfa > 0 ? degisiklik.sayfa + 1 : null;
    }
    this.router.navigate([], { relativeTo: this.rota, queryParams: parametreler, queryParamsHandling: 'merge' });
  }
}
