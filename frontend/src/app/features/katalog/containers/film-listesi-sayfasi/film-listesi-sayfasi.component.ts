import { ChangeDetectionStrategy, Component } from '@angular/core';

/** /filmler: film listesi (M5 alt görev 7'de doldurulur). */
@Component({
  selector: 'app-film-listesi-sayfasi',
  template: `<h1 class="h3 mb-4">Vizyondaki filmler</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmListesiSayfasiComponent {}
