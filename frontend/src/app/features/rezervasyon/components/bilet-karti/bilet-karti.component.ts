import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { Rezervasyon } from '../../models/rezervasyon.models';

/** QR: hata düzeltme M (%15) ekran ve kâğıtta okunur; boyut px. */
const QR_BOYUTU = 192;

/**
 * Bilet (dumb, K30): film ve afiş, şube, salon, tarih/saat, koltuklar, bilet kodu, tutar ve doğrulama adresini
 * taşıyan QR. Kart yazdırılabilir (sayfadaki diğer öğeler `d-print-none`). Kişisel veri (ad, e-posta) yok.
 */
@Component({
  selector: 'app-bilet-karti',
  templateUrl: './bilet-karti.component.html',
  styleUrls: ['./bilet-karti.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BiletKartiComponent {
  @Input() rezervasyon!: Rezervasyon;
  @Input() afisYolu: string | null = null;
  /** QR içeriği: herkese açık doğrulama sayfasının tam adresi (`{köken}/bilet/{kod}`) */
  @Input() dogrulamaAdresi = '';

  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly qrBoyutu = QR_BOYUTU;

  koltukIzle(_: number, koltuk: { cellId: number }): number {
    return koltuk.cellId;
  }
}
