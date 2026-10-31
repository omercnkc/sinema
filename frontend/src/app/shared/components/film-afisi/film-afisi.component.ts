import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TmdbAfisBoyutu } from '../../pipes/tmdb-afis.pipe';

/**
 * Film afişi (2:3). Afiş yoksa (elle girilen film) aynı oranda, filmin baş harfiyle bir yer tutucu çizilir; ızgara
 * kaymaz. Görsel tembel yüklenir (kart listesinde ekrana girince).
 */
@Component({
  selector: 'app-film-afisi',
  template: `
    <img
      *ngIf="afisYolu | tmdbAfis: boyut as adres; else yerTutucu"
      class="film-afisi"
      [src]="adres"
      [alt]="baslik + ' afişi'"
      loading="lazy"
    />
    <ng-template #yerTutucu>
      <div class="film-afisi film-afisi--bos" role="img" [attr.aria-label]="baslik + ' (afiş yok)'">
        <span aria-hidden="true">{{ baslik.charAt(0) }}</span>
      </div>
    </ng-template>
  `,
  styleUrls: ['./film-afisi.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmAfisiComponent {
  @Input() afisYolu: string | null = null;
  @Input() baslik = '';
  @Input() boyut: TmdbAfisBoyutu = 'w342';
}
