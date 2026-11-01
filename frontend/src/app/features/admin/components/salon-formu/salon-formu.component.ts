import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { fiyatiNormalizeEt, SinemaDogrulayicilar } from '../../../../shared/form/sinema-dogrulayicilar';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import { SalonIstegi, YonetimSalon } from '../../models/sube-yonetimi.models';

const AD_EN_FAZLA = 100;

/** Aynı şubede aynı adlı ikinci salon olamaz: hata ad alanının altında gösterilir. */
const KOD_ALANLARI: Readonly<Record<string, string>> = { HALL_NAME_TAKEN: 'name' };

/**
 * Salon oluşturma/düzenleme formu (dumb). Fiyat virgüllü ya da noktalı yazılabilir, sunucuya noktalı string gider.
 * Fiyat değişikliği yalnızca bundan sonra eklenen seanslara kopyalanır (K23); düzenlemede bu bilgi formda yazar.
 */
@Component({
  selector: 'app-salon-formu',
  templateUrl: './salon-formu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalonFormuComponent {
  @Input() gonderiliyor = false;
  @Output() readonly kaydet = new EventEmitter<SalonIstegi>();
  @Output() readonly iptal = new EventEmitter<void>();

  readonly form: FormGroup;
  readonly ad: AbstractControl;
  readonly fiyat: AbstractControl;
  hataMesaji: string | null = null;
  duzenleme = false;

  constructor(formBuilder: FormBuilder) {
    this.form = formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(AD_EN_FAZLA)]],
      defaultTicketPrice: ['', [Validators.required, SinemaDogrulayicilar.fiyat]],
    });
    this.ad = this.form.controls['name'];
    this.fiyat = this.form.controls['defaultTicketPrice'];
  }

  /** Düzenlenecek salon; yeni salonda null. */
  @Input() set salon(salon: YonetimSalon | null) {
    this.duzenleme = salon !== null;
    if (salon) {
      this.form.setValue({ name: salon.name, defaultTicketPrice: salon.defaultTicketPrice });
    }
  }

  @Input() set hata(hata: unknown) {
    this.hataMesaji = hata ? formHatasiniIsle(this.form, hata, KOD_ALANLARI) : null;
  }

  gonder(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    this.kaydet.emit({ name: String(this.ad.value).trim(), defaultTicketPrice: fiyatiNormalizeEt(this.fiyat.value) });
  }
}
