import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, shareReplay, switchMap, takeUntil } from 'rxjs/operators';
import { Sayfa } from '../../../../shared/models/sayfa.models';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { FilmFiltresi, FilmOzeti } from '../../models/film.models';
import { FILM_SAYFA_BOYUTU, FilmService } from '../../services/film.service';

/** Aramada her tuşta değil, yazma durunca istek gider (rules §6.4 typeahead). */
const ARAMA_BEKLEMESI_MS = 300;

/** Adres çubuğu sorgu parametreleri; sayfa numarası kullanıcıya 1'den başlar. */
const PARAMETRE = { arama: 'ara', tur: 'tur', sayfa: 'sayfa' } as const;

function filtreOku(parametreler: ParamMap): FilmFiltresi {
  const sayfaNo = Number(parametreler.get(PARAMETRE.sayfa));
  return {
    arama: parametreler.get(PARAMETRE.arama) ?? '',
    tur: parametreler.get(PARAMETRE.tur) ?? '',
    sayfa: Number.isInteger(sayfaNo) && sayfaNo > 1 ? sayfaNo - 1 : 0,
  };
}

function ayniFiltre(onceki: FilmFiltresi, simdiki: FilmFiltresi): boolean {
  return onceki.arama === simdiki.arama && onceki.tur === simdiki.tur && onceki.sayfa === simdiki.sayfa;
}

interface FilmListesiGorunumu {
  readonly filtre: FilmFiltresi;
  readonly durum: YuklemeDurumu<Sayfa<FilmOzeti>>;
}

/**
 * /filmler (smart). Filtre (arama, tür, sayfa) adres çubuğundadır: geri tuşu ve paylaşılan bağlantı aynı listeyi
 * açar. Akış: sorgu parametreleri → filtre → `switchMap` ile istek (yeni filtre gelince eski istek iptal edilir).
 */
@Component({
  selector: 'app-film-listesi-sayfasi',
  templateUrl: './film-listesi-sayfasi.component.html',
  styleUrls: ['./film-listesi-sayfasi.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmListesiSayfasiComponent implements OnDestroy {
  readonly sayfaBoyutu = FILM_SAYFA_BOYUTU;
  readonly aramaKontrolu = new FormControl('');
  readonly turler$: Observable<readonly string[]>;
  readonly gorunum$: Observable<FilmListesiGorunumu>;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);
  private readonly yikim$ = new Subject<void>();

  constructor(
    private readonly filmService: FilmService,
    private readonly router: Router,
    private readonly rota: ActivatedRoute,
  ) {
    const filtre$ = rota.queryParamMap.pipe(
      map(filtreOku),
      distinctUntilChanged(ayniFiltre),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const durum$ = combineLatest([filtre$, this.tekrarDene$]).pipe(
      switchMap(([filtre]) => yuklemeDurumu(filmService.ara(filtre))),
    );
    this.gorunum$ = combineLatest([filtre$, durum$]).pipe(map(([filtre, durum]) => ({ filtre, durum })));
    this.turler$ = filmService.turler();

    // Adres → arama kutusu (geri tuşu); emitEvent kapalı, yoksa adres tekrar yazılırdı
    filtre$.pipe(takeUntil(this.yikim$))
      .subscribe((filtre) => this.aramaKontrolu.setValue(filtre.arama, { emitEvent: false }));
    // Arama kutusu → adres
    this.aramaKontrolu.valueChanges.pipe(
      map((deger: unknown) => String(deger ?? '').trim()),
      debounceTime(ARAMA_BEKLEMESI_MS),
      distinctUntilChanged(),
      takeUntil(this.yikim$),
    ).subscribe((arama) => this.filtreyiGuncelle({ arama, sayfa: 0 }));
  }

  turSecildi(tur: string): void {
    this.filtreyiGuncelle({ tur, sayfa: 0 });
  }

  /** ng-bootstrap sayfalama 1'den sayar. */
  sayfaDegisti(sayfaNo: number): void {
    this.filtreyiGuncelle({ sayfa: sayfaNo - 1 });
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  filmIzle(_: number, film: FilmOzeti): number {
    return film.id;
  }

  turIzle(_: number, tur: string): string {
    return tur;
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  /** Yalnızca değişen parametreler yazılır; varsayılan değerler (boş, ilk sayfa) adresten silinir. */
  private filtreyiGuncelle(degisiklik: Partial<FilmFiltresi>): void {
    const parametreler: Params = {};
    if (degisiklik.arama !== undefined) {
      parametreler[PARAMETRE.arama] = degisiklik.arama || null;
    }
    if (degisiklik.tur !== undefined) {
      parametreler[PARAMETRE.tur] = degisiklik.tur || null;
    }
    if (degisiklik.sayfa !== undefined) {
      parametreler[PARAMETRE.sayfa] = degisiklik.sayfa > 0 ? degisiklik.sayfa + 1 : null;
    }
    this.router.navigate([], { relativeTo: this.rota, queryParams: parametreler, queryParamsHandling: 'merge' });
  }
}
