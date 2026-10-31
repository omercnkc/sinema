import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FilmOzeti } from '../../models/film.models';

/** Listede bir film (dumb): afiş, ad, yaş sınırı, süre, türler, TMDB puanı. Kartın tamamı detaya bağlantıdır. */
@Component({
  selector: 'app-film-karti',
  templateUrl: './film-karti.component.html',
  styleUrls: ['./film-karti.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmKartiComponent {
  @Input() film!: FilmOzeti;
}
