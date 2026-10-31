import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, Subject } from 'rxjs';
import { catchError, exhaustMap, filter, finalize, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import {
  EPOSTA_EN_FAZLA_KARAKTER,
  KISI_ADI_EN_FAZLA_KARAKTER,
  SinemaDogrulayicilar,
} from '../../../../shared/form/sinema-dogrulayicilar';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import { KayitIstegi } from '../../models/kayit.models';
import { KayitService } from '../../services/kayit.service';
import { GirisSayfasiDurumu } from '../giris-sayfasi/giris-sayfasi.component';

/** Alanla ilgili iş hataları formda ilgili alanın altında gösterilir. */
const KOD_ALANLARI: Readonly<Record<string, string>> = { EMAIL_ALREADY_REGISTERED: 'email' };

/**
 * Üye kaydı (K48: ad, soyad, TR cep telefonu iyzico alıcı bilgisi içindir; bilette görünmez). Kurallar sunucuyla
 * aynıdır, asıl doğrulama sunucudadır. Başarılı kayıt oturum açmaz: kullanıcı e-postası dolu giriş sayfasına gider.
 */
@Component({
  selector: 'app-kayit-sayfasi',
  templateUrl: './kayit-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KayitSayfasiComponent implements OnDestroy {
  readonly form: FormGroup;
  readonly ad: AbstractControl;
  readonly soyad: AbstractControl;
  readonly eposta: AbstractControl;
  readonly telefon: AbstractControl;
  readonly parola: AbstractControl;
  hataMesaji: string | null = null;
  gonderiliyor = false;

  private readonly gonder$ = new Subject<void>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    private readonly kayitService: KayitService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    const kisiAdi = [Validators.required, Validators.maxLength(KISI_ADI_EN_FAZLA_KARAKTER), SinemaDogrulayicilar.kisiAdi];
    this.form = formBuilder.group({
      firstName: ['', kisiAdi],
      lastName: ['', kisiAdi],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(EPOSTA_EN_FAZLA_KARAKTER)]],
      phone: ['', [Validators.required, SinemaDogrulayicilar.telefon]],
      password: ['', [Validators.required, SinemaDogrulayicilar.parola]],
    });
    this.ad = this.form.controls['firstName'];
    this.soyad = this.form.controls['lastName'];
    this.eposta = this.form.controls['email'];
    this.telefon = this.form.controls['phone'];
    this.parola = this.form.controls['password'];

    this.gonder$.pipe(
      filter(() => this.formGecerli()),
      tap(() => this.gonderimBasladi()),
      exhaustMap(() => {
        const istek = this.istek();
        return this.kayitService.kayitOl(istek).pipe(
          tap(() => this.kayitBasarili(istek.email)),
          catchError((hata: unknown) => {
            this.hataMesaji = formHatasiniIsle(this.form, hata, KOD_ALANLARI);
            return EMPTY;
          }),
          finalize(() => this.gonderimBitti()),
        );
      }),
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
  private istek(): KayitIstegi {
    return {
      firstName: String(this.ad.value).trim(),
      lastName: String(this.soyad.value).trim(),
      email: String(this.eposta.value).trim(),
      phone: String(this.telefon.value).trim(),
      password: String(this.parola.value),
    };
  }

  private gonderimBasladi(): void {
    this.hataMesaji = null;
    this.gonderiliyor = true;
  }

  private gonderimBitti(): void {
    this.gonderiliyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }

  private kayitBasarili(eposta: string): void {
    this.bildirimService.basari('Kaydınız oluşturuldu. Şimdi giriş yapabilirsiniz.');
    const durum: GirisSayfasiDurumu = { eposta };
    this.router.navigate(['/giris'], { queryParamsHandling: 'preserve', state: durum });
  }
}
