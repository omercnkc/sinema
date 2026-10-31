import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Veri beklenirken gösterilen dönen simge; ekran okuyucu etiketi okur (role="status"). */
@Component({
  selector: 'app-yukleniyor',
  template: `
    <div class="d-flex justify-content-center py-4" role="status">
      <div class="spinner-border text-primary" aria-hidden="true"></div>
      <span class="visually-hidden">{{ etiket }}</span>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class YukleniyorComponent {
  @Input() etiket = 'Yükleniyor…';
}
