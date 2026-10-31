import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Alt bilgi ve TMDB atfı (K18, §9.3-2): TMDB API şartları, TMDB'nin onaylı logosunun (renk/oran değiştirilmeden) ve
 * İngilizce atıf metninin görünür bir yerde bulunmasını ister. Logo assets/tmdb altında, TMDB'nin logo sayfasından
 * olduğu gibi alındı.
 */
@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  readonly tmdbAdresi = 'https://www.themoviedb.org/';
}
