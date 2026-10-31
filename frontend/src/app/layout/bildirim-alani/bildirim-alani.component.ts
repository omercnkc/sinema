import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable } from 'rxjs';
import { Bildirim, BildirimService, BildirimTuru } from '../../core/bildirim/bildirim.service';

/** Bildirimlerin (toast) gösterildiği köşe; ng-bootstrap toast, birkaç saniye sonra kendiliğinden kapanır. */
@Component({
  selector: 'app-bildirim-alani',
  templateUrl: './bildirim-alani.component.html',
  styleUrls: ['./bildirim-alani.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BildirimAlaniComponent {
  static readonly GOSTERIM_SURESI_MS = 6000;

  readonly bildirimler$: Observable<readonly Bildirim[]>;
  readonly gosterimSuresi = BildirimAlaniComponent.GOSTERIM_SURESI_MS;
  // Bootstrap 5.1 sınıfları (text-bg-* 5.2'de geldi); metin rengi kontrast için arka plana göre
  readonly renkSiniflari: Readonly<Record<BildirimTuru, string>> = {
    hata: 'bg-danger text-white',
    basari: 'bg-success text-white',
    bilgi: 'bg-dark text-white',
  };

  constructor(private readonly bildirimService: BildirimService) {
    this.bildirimler$ = bildirimService.bildirimler$;
  }

  kapat(kimlik: number): void {
    this.bildirimService.kaldir(kimlik);
  }

  kimlikIzle(_: number, bildirim: Bildirim): number {
    return bildirim.kimlik;
  }
}
