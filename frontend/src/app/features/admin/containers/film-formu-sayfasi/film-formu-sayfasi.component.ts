import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { catchError, exhaustMap, finalize, map, shareReplay, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { FilmFormuBaslangici, FilmIstegi, TmdbFilmOnizlemesi, YonetimFilmDetayi } from '../../models/film-formu.models';
import { TmdbService } from '../../services/tmdb.service';
import { YonetimFilmService } from '../../services/yonetim-film.service';

const FILM_LISTESI = '/admin/filmler';
const YENI_FILM: YuklemeDurumu<YonetimFilmDetayi | null> = { yukleniyor: false, veri: null, hata: null };
const FILM_YOK = new UygulamaHatasi(404, 'MOVIE_NOT_FOUND', 'Film bulunamadı.');

interface FilmFormuGorunumu {
  /** Düzenlemede kayıtlı filmin yüklenmesi; yeni filmde hemen boş veriyle hazırdır. */
  readonly film: YuklemeDurumu<YonetimFilmDetayi | null>;
  readonly onizleme: YuklemeDurumu<TmdbFilmOnizlemesi> | null;
  /** Formu dolduracak veri: son TMDB önizlemesi (ekli değilse) ya da kayıtlı film; değişince form sıfırlanır. */
  readonly baslangic: FilmFormuBaslangici | null;
}

/**
 * /admin/filmler/yeni ve /admin/filmler/:id (smart). Yeni filmde TMDB araması formu doldurur (K18); TMDB
 * bağlantısı sonradan eklenemez/değişmez, bu yüzden düzenlemede arama yoktur. Kayıttan sonra listeye dönülür.
 */
@Component({
  selector: 'app-film-formu-sayfasi',
  templateUrl: './film-formu-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmFormuSayfasiComponent implements OnDestroy {
  /** Düzenlenen filmin kimliği; yeni filmde null. */
  readonly filmId: number | null;
  readonly gorunum$: Observable<FilmFormuGorunumu>;
  gonderiliyor = false;
  kayitHatasi: unknown = null;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);
  private readonly tmdbSecimi$ = new Subject<number>();
  private readonly kaydet$ = new Subject<FilmIstegi>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    rota: ActivatedRoute,
    private readonly filmService: YonetimFilmService,
    tmdbService: TmdbService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    const kimlik = rota.snapshot.paramMap.get('id');
    this.filmId = kimlik === null ? null : Number(kimlik);

    const film$ = this.tekrarDene$.pipe(switchMap(() => this.filmYukle()));
    const onizleme$ = this.tmdbSecimi$.pipe(
      switchMap((tmdbId) => yuklemeDurumu(tmdbService.onizleme(tmdbId))),
      startWith(null),
    );
    this.gorunum$ = combineLatest([film$, onizleme$]).pipe(
      map(([film, onizleme]) => ({ film, onizleme, baslangic: formBaslangici(film, onizleme) })),
      shareReplay({ bufferSize: 1, refCount: true }),
    );

    this.kaydet$.pipe(
      tap(() => this.gonderimBasladi()),
      exhaustMap((istek) => this.kaydetIstegi(istek).pipe(
        tap((film) => this.kaydedildi(film)),
        catchError((hata: unknown) => {
          this.kayitHatasi = hata;
          return EMPTY;
        }),
        finalize(() => this.gonderimBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  tmdbSecildi(tmdbId: number): void {
    this.tmdbSecimi$.next(tmdbId);
  }

  kaydet(istek: FilmIstegi): void {
    this.kaydet$.next(istek);
  }

  vazgec(): void {
    this.router.navigateByUrl(FILM_LISTESI);
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private filmYukle(): Observable<YuklemeDurumu<YonetimFilmDetayi | null>> {
    if (this.filmId === null) {
      return of(YENI_FILM);
    }
    if (!Number.isInteger(this.filmId) || this.filmId < 1) {
      return of({ yukleniyor: false, veri: null, hata: FILM_YOK });
    }
    return yuklemeDurumu<YonetimFilmDetayi | null>(this.filmService.getir(this.filmId));
  }

  private kaydetIstegi(istek: FilmIstegi): Observable<YonetimFilmDetayi> {
    return this.filmId === null ? this.filmService.olustur(istek) : this.filmService.guncelle(this.filmId, istek);
  }

  private gonderimBasladi(): void {
    this.gonderiliyor = true;
    this.kayitHatasi = null;
  }

  private kaydedildi(film: YonetimFilmDetayi): void {
    this.bildirimService.basari(this.filmId === null ? `"${film.title}" eklendi.` : `"${film.title}" güncellendi.`);
    this.router.navigateByUrl(FILM_LISTESI);
  }

  private gonderimBitti(): void {
    this.gonderiliyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }
}

/** Zaten ekli bir TMDB filminin önizlemesi formu doldurmaz (şablon düzenleme bağlantısı gösterir). */
function formBaslangici(
  film: YuklemeDurumu<YonetimFilmDetayi | null>,
  onizleme: YuklemeDurumu<TmdbFilmOnizlemesi> | null,
): FilmFormuBaslangici | null {
  const tmdbVerisi = onizleme?.veri;
  if (tmdbVerisi && tmdbVerisi.movieId === null) {
    return tmdbVerisi;
  }
  return film.veri;
}
