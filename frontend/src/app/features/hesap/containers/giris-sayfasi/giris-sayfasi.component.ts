import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Subject } from 'rxjs';
import { catchError, exhaustMap, filter, finalize, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { GirisBilgileri, Kullanici } from '../../../../core/oturum/oturum.models';
import { OturumService } from '../../../../core/oturum/oturum.service';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';

/** Kayıt sayfasından gelirken e-posta router state'iyle taşınır (adres çubuğuna/geçmişe kişisel veri yazılmaz). */
export interface GirisSayfasiDurumu {
  readonly eposta?: string;
}

/**
 * Giriş sayfası. Başarılı girişte guard'ın verdiği dönüş adresine (`?donus=`) gider; adres yalnızca uygulama içi
 * bir yol olabilir (açık yönlendirme yok). Hatalı parola, hesap kilidi (429, kalan süreyle) ve ağ hataları formun
 * üstünde gösterilir.
 */
@Component({
  selector: 'app-giris-sayfasi',
  templateUrl: './giris-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GirisSayfasiComponent implements OnDestroy {
  readonly form: FormGroup;
  readonly eposta: AbstractControl;
  readonly parola: AbstractControl;
  hataMesaji: string | null = null;
  gonderiliyor = false;

  private readonly gonder$ = new Subject<void>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    private readonly oturumService: OturumService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
    private readonly rota: ActivatedRoute,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    const durum = router.getCurrentNavigation()?.extras.state as GirisSayfasiDurumu | undefined;
    this.form = formBuilder.group({
      email: [durum?.eposta ?? '', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
    this.eposta = this.form.controls['email'];
    this.parola = this.form.controls['password'];

    // Çift tıklama/Enter ikinci isteği başlatmaz: süren giriş bitene kadar yeni gönderimler yok sayılır (exhaustMap)
    this.gonder$.pipe(
      filter(() => this.formGecerli()),
      tap(() => this.gonderimBasladi()),
      exhaustMap(() => this.oturumService.girisYap(this.bilgiler()).pipe(
        catchError((hata: unknown) => {
          this.hataMesaji = formHatasiniIsle(this.form, hata);
          return EMPTY;
        }),
        finalize(() => this.gonderimBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe((kullanici) => this.girisBasarili(kullanici));
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

  private bilgiler(): GirisBilgileri {
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

  private girisBasarili(kullanici: Kullanici): void {
    this.bildirimService.basari(`Hoş geldiniz${kullanici.firstName ? ', ' + kullanici.firstName : ''}.`);
    this.router.navigateByUrl(GirisSayfasiComponent.guvenliDonusAdresi(this.rota.snapshot.queryParamMap.get('donus')));
  }

  /** Yalnızca uygulama içi yol ("/..."); "//kotu.example" gibi başka siteye götüren adresler ana sayfaya çevrilir. */
  static guvenliDonusAdresi(donus: string | null): string {
    return donus && donus.startsWith('/') && !donus.startsWith('//') && !donus.startsWith('/\\') ? donus : '/';
  }
}
