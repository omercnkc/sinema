import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AKILLI_ISARET_ADLARI, AkilliIsaret } from '../../../../shared/models/film.models';
import { listeyeAyir, SinemaDogrulayicilar } from '../../../../shared/form/sinema-dogrulayicilar';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import { FilmFormuBaslangici, FilmIstegi, TR_YAS_SINIRLARI } from '../../models/film-formu.models';

// Sınırlar sunucudaki FilmIstegi ile aynı
const BASLIK_EN_FAZLA = 255;
const OZET_EN_FAZLA = 5000;
const SURE_EN_AZ = 1;
const SURE_EN_FAZLA = 600;
const TUR_EN_FAZLA = 10;
const TUR_EN_FAZLA_KARAKTER = 50;
const OYUNCU_EN_FAZLA = 20;
const KISI_EN_FAZLA_KARAKTER = 255;
/** TMDB görsel yolu (ör. "/abc123.jpg"); tam adres istemcide kurulur (K41 hotlink). */
const AFIS_YOLU_DESENI = /^\/[A-Za-z0-9._-]+$/;
/** 0–10, en çok bir ondalık; virgül ya da nokta. */
const PUAN_DESENI = /^(10([.,]0)?|\d([.,]\d)?)$/;
const TUR_AYRACI = ',';
const SATIR_AYRACI = /\r?\n/;

const AKILLI_ISARETLER = Object.keys(AKILLI_ISARET_ADLARI) as AkilliIsaret[];

interface IsaretSecenegi {
  readonly deger: AkilliIsaret;
  readonly ad: string;
}

function bosMetin(deger: unknown): string | null {
  const metin = String(deger ?? '').trim();
  return metin.length > 0 ? metin : null;
}

/**
 * Film oluşturma/düzenleme formu (dumb). Kayıtlı film ya da TMDB önizlemesi `baslangic` ile gelir; admin düzenler,
 * geçerli form `kaydet` ile FilmIstegi olarak verilir. Türler virgülle, oyuncular satır satır yazılır. Kurallar
 * sunucuyla aynı, asıl doğrulama sunucuda; sunucu hatası `hata` girişiyle forma yerleşir.
 */
@Component({
  selector: 'app-film-formu',
  templateUrl: './film-formu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmFormuComponent {
  @Input() gonderiliyor = false;
  /** Düğme metni: yeni filmde "Filmi ekle", düzenlemede "Değişiklikleri kaydet". */
  @Input() duzenleme = false;
  @Output() readonly kaydet = new EventEmitter<FilmIstegi>();
  @Output() readonly iptal = new EventEmitter<void>();

  readonly yasSinirlari = TR_YAS_SINIRLARI;
  readonly isaretler: readonly IsaretSecenegi[] = AKILLI_ISARETLER.map((deger) => ({ deger, ad: AKILLI_ISARET_ADLARI[deger] }));
  readonly form: FormGroup;
  readonly baslik: AbstractControl;
  readonly ozet: AbstractControl;
  readonly afisYolu: AbstractControl;
  readonly sure: AbstractControl;
  readonly vizyonTarihi: AbstractControl;
  readonly puan: AbstractControl;
  readonly yonetmen: AbstractControl;
  readonly turler: AbstractControl;
  readonly oyuncular: AbstractControl;
  hataMesaji: string | null = null;
  /** TMDB kaynaklı filmin kimliği: formda alan değildir, kayda olduğu gibi eklenir (sonradan değişmez). */
  tmdbId: number | null = null;

  constructor(formBuilder: FormBuilder) {
    this.form = formBuilder.group({
      title: ['', [Validators.required, Validators.maxLength(BASLIK_EN_FAZLA)]],
      overview: ['', Validators.maxLength(OZET_EN_FAZLA)],
      posterPath: ['', [Validators.maxLength(BASLIK_EN_FAZLA), Validators.pattern(AFIS_YOLU_DESENI)]],
      runtimeMinutes: [null, [Validators.required, Validators.min(SURE_EN_AZ), Validators.max(SURE_EN_FAZLA)]],
      releaseDate: [''],
      certification: [''],
      voteAverage: ['', Validators.pattern(PUAN_DESENI)],
      director: ['', Validators.maxLength(KISI_EN_FAZLA_KARAKTER)],
      genres: ['', SinemaDogrulayicilar.liste(TUR_AYRACI, TUR_EN_FAZLA, TUR_EN_FAZLA_KARAKTER)],
      cast: ['', SinemaDogrulayicilar.liste(SATIR_AYRACI, OYUNCU_EN_FAZLA, KISI_EN_FAZLA_KARAKTER)],
      contentDescriptors: formBuilder.group(Object.fromEntries(AKILLI_ISARETLER.map((isaret) => [isaret, false]))),
    });
    this.baslik = this.form.controls['title'];
    this.ozet = this.form.controls['overview'];
    this.afisYolu = this.form.controls['posterPath'];
    this.sure = this.form.controls['runtimeMinutes'];
    this.vizyonTarihi = this.form.controls['releaseDate'];
    this.puan = this.form.controls['voteAverage'];
    this.yonetmen = this.form.controls['director'];
    this.turler = this.form.controls['genres'];
    this.oyuncular = this.form.controls['cast'];
  }

  /** Formu kayıtlı film ya da TMDB önizlemesiyle doldurur (önceki değerler ve dokunulmuş durumu silinir). */
  @Input() set baslangic(baslangic: FilmFormuBaslangici | null) {
    if (!baslangic) {
      return;
    }
    this.tmdbId = baslangic.tmdbId;
    this.form.reset({
      title: baslangic.title ?? '',
      overview: baslangic.overview ?? '',
      posterPath: baslangic.posterPath ?? '',
      runtimeMinutes: baslangic.runtimeMinutes,
      releaseDate: baslangic.releaseDate ?? '',
      certification: baslangic.certification ?? '',
      voteAverage: baslangic.voteAverage === null ? '' : String(baslangic.voteAverage),
      director: baslangic.director ?? '',
      genres: baslangic.genres.join(`${TUR_AYRACI} `),
      cast: baslangic.cast.join('\n'),
      contentDescriptors: Object.fromEntries(AKILLI_ISARETLER.map((isaret) =>
        [isaret, baslangic.contentDescriptors.includes(isaret)])),
    });
  }

  @Input() set hata(hata: unknown) {
    this.hataMesaji = hata ? formHatasiniIsle(this.form, hata) : null;
  }

  /** Önizleme yalnızca geçerli yol için (geçersiz yolla kırık görsel isteği atılmaz). */
  get onizlemeAfisi(): string | null {
    return this.afisYolu.valid ? bosMetin(this.afisYolu.value) : null;
  }

  gonder(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    this.kaydet.emit(this.istek());
  }

  yasIzle(_: number, yas: string): string {
    return yas;
  }

  isaretIzle(_: number, secenek: IsaretSecenegi): string {
    return secenek.deger;
  }

  /** Untyped form (Angular 13) değeri burada tipli isteğe çevrilir; boş metinler null gider. */
  private istek(): FilmIstegi {
    const deger = this.form.value as Record<string, unknown>;
    const isaretler = deger['contentDescriptors'] as Record<AkilliIsaret, boolean>;
    const puan = bosMetin(deger['voteAverage']);
    return {
      tmdbId: this.tmdbId,
      title: String(deger['title']).trim(),
      overview: bosMetin(deger['overview']),
      posterPath: bosMetin(deger['posterPath']),
      runtimeMinutes: Number(deger['runtimeMinutes']),
      releaseDate: bosMetin(deger['releaseDate']),
      certification: bosMetin(deger['certification']),
      voteAverage: puan === null ? null : Number(puan.replace(',', '.')),
      director: bosMetin(deger['director']),
      genres: [...new Set(listeyeAyir(deger['genres'], TUR_AYRACI))],
      cast: listeyeAyir(deger['cast'], SATIR_AYRACI),
      contentDescriptors: AKILLI_ISARETLER.filter((isaret) => isaretler[isaret]),
    };
  }
}
