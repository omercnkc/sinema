import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Sayfa içinde gösterilen hata (ör. liste yüklenemedi). Mesaj hazır gelir (UygulamaHatasi.mesaj); "Tekrar dene"
 * düğmesi yalnızca dinleyen varsa anlamlıdır, bu yüzden görünürlüğü ayrı bir girişle seçilir.
 */
@Component({
  selector: 'app-hata-mesaji',
  template: `
    <div class="alert alert-danger d-flex flex-wrap align-items-center gap-2" role="alert">
      <span class="me-auto">{{ mesaj }}</span>
      <button *ngIf="tekrarDenenebilir" type="button" class="btn btn-sm btn-outline-danger" (click)="tekrarDene.emit()">
        Tekrar dene
      </button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HataMesajiComponent {
  @Input() mesaj = '';
  @Input() tekrarDenenebilir = false;
  @Output() readonly tekrarDene = new EventEmitter<void>();
}
