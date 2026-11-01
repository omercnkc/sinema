import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, exhaustMap, filter, finalize, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { EPOSTA_EN_FAZLA_KARAKTER, SinemaDogrulayicilar } from '../../../../shared/form/sinema-dogrulayicilar';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import { YoneticiCevabi, YoneticiIstegi } from '../../models/yonetici.models';
import { YoneticiService } from '../../services/yonetici.service';

/** Alanla ilgili iş hataları formda ilgili alanın altında gösterilir. */
const KOD_ALANLARI: Readonly<Record<string, string>> = { EMAIL_ALREADY_REGISTERED: 'email' };

/**
 * Yeni yönetici ekleme (K36). Yalnızca e-posta ve parola; kurallar auth-service `AdminIstegi` ile aynı. Başarıda
 * form temizlenir ve bildirim çıkar (aynı formdan art arda birden çok yönetici eklenebilir).
 */
@Component({
  selector: 'app-yonetici-ekleme-sayfasi',
  templateUrl: './yonetici-ekleme-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class YoneticiEklemeSayfasiComponent implements OnDestroy {
  readonly form: FormGroup;
  readonly eposta: AbstractControl;
  readonly parola: AbstractControl;
  hataMesaji: string | null = null;
  gonderiliyor = false;

  private readonly gonder$ = new Subject<void>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    private readonly yoneticiService: YoneticiService,
    private readonly bildirimService: BildirimService,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    this.form = formBuilder.group({
      email: ['', [Validators.required, Validators.email, Validators.maxLength(EPOSTA_EN_FAZLA_KARAKTER)]],
      password: ['', [Validators.required, SinemaDogrulayicilar.parola]],
    });
    this.eposta = this.form.controls['email'];
    this.parola = this.form.controls['password'];

    this.gonder$.pipe(
      filter(() => this.formGecerli()),
      tap(() => this.gonderimBasladi()),
      exhaustMap(() => this.yoneticiService.ekle(this.istek()).pipe(
        tap((cevap) => this.eklendi(cevap)),
        catchError((hata: unknown) => {
          this.hataMesaji = formHatasiniIsle(this.form, hata, KOD_ALANLARI);
          return EMPTY;
        }),
        finalize(() => this.gonderimBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  gonder(): void {
    this.gonder$.next();
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private formGecerli(): boolean {
    this.form.markAllAsTouched();
    return this.form.valid;
  }

  /** Untyped form (Angular 13) değeri burada tipli isteğe çevrilir. */
  private istek(): YoneticiIstegi {
    return { email: String(this.eposta.value).trim(), password: String(this.parola.value) };
  }

  private gonderimBasladi(): void {
    this.hataMesaji = null;
    this.gonderiliyor = true;
  }

  private gonderimBitti(): void {
    this.gonderiliyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }

  private eklendi(cevap: YoneticiCevabi): void {
    this.bildirimService.basari(`${cevap.email} yönetici olarak eklendi.`);
    this.form.reset({ email: '', password: '' });
  }
}
