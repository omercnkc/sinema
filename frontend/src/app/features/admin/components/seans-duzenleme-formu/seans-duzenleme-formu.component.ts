import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { fiyatiNormalizeEt, SinemaDogrulayicilar } from '../../../../shared/form/sinema-dogrulayicilar';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import {
  SATIS_OFSETI_EN_AZ, SATIS_OFSETI_EN_FAZLA, SeansGuncellemeIstegi, YonetimSeansOzeti,
} from '../../models/seans-yonetimi.models';

/** Dakika; eksi işaretli olabilir (seans başlamadan önce kapanış). */
export const TAM_SAYI_DESENI = /^-?\d+$/;

/**
 * Seansın fiyatı ve satış kapanış ofseti (dumb). Film, salon ve saat değişmez (K34). Yeni fiyat yalnızca bundan
 * sonraki satışlara uygulanır; satılmış biletlerin fiyatı rezervasyonda saklıdır.
 */
@Component({
  selector: 'app-seans-duzenleme-formu',
  templateUrl: './seans-duzenleme-formu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeansDuzenlemeFormuComponent {
  @Input() gonderiliyor = false;
  @Output() readonly kaydet = new EventEmitter<SeansGuncellemeIstegi>();
  @Output() readonly iptal = new EventEmitter<void>();

  readonly enAzOfset = SATIS_OFSETI_EN_AZ;
  readonly enFazlaOfset = SATIS_OFSETI_EN_FAZLA;
  readonly form: FormGroup;
  readonly fiyat: AbstractControl;
  readonly ofset: AbstractControl;
  hataMesaji: string | null = null;

  constructor(formBuilder: FormBuilder) {
    this.form = formBuilder.group({
      ticketPrice: ['', [Validators.required, SinemaDogrulayicilar.fiyat]],
      salesCloseOffsetMinutes: ['', [Validators.required, Validators.pattern(TAM_SAYI_DESENI),
        Validators.min(SATIS_OFSETI_EN_AZ), Validators.max(SATIS_OFSETI_EN_FAZLA)]],
    });
    this.fiyat = this.form.controls['ticketPrice'];
    this.ofset = this.form.controls['salesCloseOffsetMinutes'];
  }

  @Input() set seans(seans: YonetimSeansOzeti) {
    this.form.reset({ ticketPrice: seans.ticketPrice, salesCloseOffsetMinutes: String(seans.salesCloseOffsetMinutes) });
  }

  @Input() set hata(hata: unknown) {
    this.hataMesaji = hata ? formHatasiniIsle(this.form, hata) : null;
  }

  gonder(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    this.kaydet.emit({
      ticketPrice: fiyatiNormalizeEt(this.fiyat.value),
      salesCloseOffsetMinutes: Number(this.ofset.value),
    });
  }
}
