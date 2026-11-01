import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { GocDegisikligi, GocRaporu, SeansGocRaporu } from '../../models/duzen-gocu.models';

/**
 * Göç önizlemesi (dumb): taslak yayımlanırsa başlamamış seansların biletlerine ne olacağı. Çözülemeyen rezervasyon
 * varsa yayımlama yapılamaz; düzende yer açılıp taslak yeniden kaydedilmelidir.
 */
@Component({
  selector: 'app-goc-raporu',
  templateUrl: './goc-raporu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GocRaporuComponent {
  @Input() rapor!: GocRaporu;

  readonly saatDilimi = IS_SAAT_DILIMI;

  seansIzle(_: number, seans: SeansGocRaporu): number {
    return seans.showtimeId;
  }

  degisiklikIzle(_: number, degisiklik: GocDegisikligi): string {
    return `${degisiklik.reservationId}-${degisiklik.oldLabel}`;
  }

  rezervasyonIzle(_: number, rezervasyon: { readonly reservationId: number }): number {
    return rezervasyon.reservationId;
  }
}
