import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { EMPTY, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, exhaustMap, filter, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { istanbulAni } from '../../../../core/zaman/is-gunu';
import { fiyatiNormalizeEt, SinemaDogrulayicilar } from '../../../../shared/form/sinema-dogrulayicilar';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import { TAM_SAYI_DESENI } from '../../components/seans-duzenleme-formu/seans-duzenleme-formu.component';
import { SATIS_OFSETI_EN_AZ, SATIS_OFSETI_EN_FAZLA, SeansOlusturmaIstegi } from '../../models/seans-yonetimi.models';
import { YonetimSalon } from '../../models/sube-yonetimi.models';
import { YonetimFilmOzeti } from '../../models/yonetim-film.models';
import { SeansYonetimiService } from '../../services/seans-yonetimi.service';
import { YonetimFilmService } from '../../services/yonetim-film.service';

const ARAMA_BEKLEMESI_MS = 300;
const EN_AZ_KARAKTER = 2;

/** Alanla ilgili iş hataları ilgili alanın altında gösterilir. */
const KOD_ALANLARI: Readonly<Record<string, string>> = {
  SHOWTIME_OVERLAP: 'startsAt',
  SHOWTIME_IN_PAST: 'startsAt',
  MOVIE_HIDDEN: 'movieId',
  MOVIE_NOT_FOUND: 'movieId',
};

/**
 * Seans ekleme (smart; film araması ve kayıt burada). Saat seçilen İstanbul gününde yorumlanır. Yalnızca görünür
 * filmler önerilir: gizli filme seans eklenemez (K40). Fiyat boşsa salonun varsayılanı, ofset boşsa sunucu
 * varsayılanı kullanılır. Başarıdan sonra film seçili kalır (aynı filme art arda seans eklemek için), saat temizlenir.
 */
@Component({
  selector: 'app-seans-ekleme',
  templateUrl: './seans-ekleme.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeansEklemeComponent implements OnDestroy {
  @Input() salon!: YonetimSalon;
  /** İstanbul günü `YYYY-MM-DD`. */
  @Input() gun = '';
  @Output() readonly eklendi = new EventEmitter<void>();
  @Output() readonly iptal = new EventEmitter<void>();

  readonly enAzOfset = SATIS_OFSETI_EN_AZ;
  readonly enFazlaOfset = SATIS_OFSETI_EN_FAZLA;
  readonly aramaKontrolu = new FormControl('');
  readonly adaylar$: Observable<readonly YonetimFilmOzeti[]>;
  readonly form: FormGroup;
  readonly film: AbstractControl;
  readonly saat: AbstractControl;
  readonly fiyat: AbstractControl;
  readonly ofset: AbstractControl;
  secilenFilm: YonetimFilmOzeti | null = null;
  hataMesaji: string | null = null;
  gonderiliyor = false;

  private readonly gonder$ = new Subject<void>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    filmService: YonetimFilmService,
    private readonly seansService: SeansYonetimiService,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    this.form = formBuilder.group({
      movieId: [null, Validators.required],
      startsAt: ['', Validators.required],
      ticketPrice: ['', SinemaDogrulayicilar.fiyat],
      salesCloseOffsetMinutes: ['', [Validators.pattern(TAM_SAYI_DESENI), Validators.min(SATIS_OFSETI_EN_AZ),
        Validators.max(SATIS_OFSETI_EN_FAZLA)]],
    });
    this.film = this.form.controls['movieId'];
    this.saat = this.form.controls['startsAt'];
    this.fiyat = this.form.controls['ticketPrice'];
    this.ofset = this.form.controls['salesCloseOffsetMinutes'];

    this.adaylar$ = this.aramaKontrolu.valueChanges.pipe(
      map((deger: unknown) => String(deger ?? '').trim()),
      debounceTime(ARAMA_BEKLEMESI_MS),
      distinctUntilChanged(),
      switchMap((arama) => arama.length < EN_AZ_KARAKTER ? of([]) : filmService.ara({ arama, sayfa: 0 }).pipe(
        map((sayfa) => sayfa.content.filter((aday) => aday.visible)),
        catchError(() => of([])),
      )),
    );

    this.gonder$.pipe(
      filter(() => this.formGecerli()),
      tap(() => this.gonderimBasladi()),
      exhaustMap(() => this.seansService.olustur(this.istek()).pipe(
        tap(() => this.kaydedildi()),
        catchError((hata: unknown) => {
          this.hataMesaji = formHatasiniIsle(this.form, hata, KOD_ALANLARI);
          return EMPTY;
        }),
        finalize(() => this.gonderimBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  filmSec(film: YonetimFilmOzeti): void {
    this.secilenFilm = film;
    this.film.setValue(film.id);
    this.aramaKontrolu.setValue('', { emitEvent: false });
  }

  filmiDegistir(): void {
    this.secilenFilm = null;
    this.film.setValue(null);
  }

  gonder(): void {
    this.gonder$.next();
  }

  adayIzle(_: number, film: YonetimFilmOzeti): number {
    return film.id;
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private formGecerli(): boolean {
    this.form.markAllAsTouched();
    return this.form.valid;
  }

  private istek(): SeansOlusturmaIstegi {
    const fiyat = fiyatiNormalizeEt(this.fiyat.value);
    const ofset = String(this.ofset.value ?? '').trim();
    return {
      movieId: Number(this.film.value),
      hallId: this.salon.id,
      startsAt: istanbulAni(this.gun, String(this.saat.value)),
      ticketPrice: fiyat === '' ? null : fiyat,
      salesCloseOffsetMinutes: ofset === '' ? null : Number(ofset),
    };
  }

  private gonderimBasladi(): void {
    this.hataMesaji = null;
    this.gonderiliyor = true;
  }

  private kaydedildi(): void {
    this.saat.reset('');
    this.eklendi.emit();
  }

  private gonderimBitti(): void {
    this.gonderiliyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }
}
