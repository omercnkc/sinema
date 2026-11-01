import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { formHatasiniIsle } from '../../../../shared/form/sunucu-hatalari';
import { SubeIstegi, YonetimSube } from '../../models/sube-yonetimi.models';

// Sunucudaki SubeIstegi sınırları
const AD_EN_FAZLA = 100;
const ADRES_EN_FAZLA = 255;
/** Örnek veri İstanbul'da (K2); yeni şubede şehir alanı bununla dolu gelir, değiştirilebilir. */
const VARSAYILAN_SEHIR = 'İstanbul';

/**
 * Şube oluşturma/düzenleme formu (dumb): servis çağırmaz, geçerli formu `kaydet` ile verir. Sunucu hatası `hata`
 * girişiyle gelir; alan hataları ilgili alanın altına, kalanı form üstüne yazılır.
 */
@Component({
  selector: 'app-sube-formu',
  templateUrl: './sube-formu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubeFormuComponent {
  @Input() gonderiliyor = false;
  @Output() readonly kaydet = new EventEmitter<SubeIstegi>();
  @Output() readonly iptal = new EventEmitter<void>();

  readonly form: FormGroup;
  readonly ad: AbstractControl;
  readonly sehir: AbstractControl;
  readonly adres: AbstractControl;
  hataMesaji: string | null = null;

  constructor(formBuilder: FormBuilder) {
    this.form = formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(AD_EN_FAZLA)]],
      city: [VARSAYILAN_SEHIR, [Validators.required, Validators.maxLength(AD_EN_FAZLA)]],
      address: ['', [Validators.required, Validators.maxLength(ADRES_EN_FAZLA)]],
    });
    this.ad = this.form.controls['name'];
    this.sehir = this.form.controls['city'];
    this.adres = this.form.controls['address'];
  }

  /** Düzenlenecek şube; yeni şubede null. */
  @Input() set sube(sube: YonetimSube | null) {
    if (sube) {
      this.form.setValue({ name: sube.name, city: sube.city, address: sube.address });
    }
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
      name: String(this.ad.value).trim(),
      city: String(this.sehir.value).trim(),
      address: String(this.adres.value).trim(),
    });
  }
}
