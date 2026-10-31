import { NgModule } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../../shared/shared.module';
import { FilmKartiComponent } from './components/film-karti/film-karti.component';
import { FilmSeanslariComponent } from './containers/film-seanslari/film-seanslari.component';
import { FilmDetaySayfasiComponent } from './containers/film-detay-sayfasi/film-detay-sayfasi.component';
import { FilmListesiSayfasiComponent } from './containers/film-listesi-sayfasi/film-listesi-sayfasi.component';
import { KatalogRoutingModule } from './katalog-routing.module';

/** Film listesi, film detayı ve seans seçimi (§8.2); /filmler altında tembel yüklenir. */
@NgModule({
  declarations: [FilmListesiSayfasiComponent, FilmDetaySayfasiComponent, FilmSeanslariComponent, FilmKartiComponent],
  imports: [SharedModule, NgbPaginationModule, KatalogRoutingModule],
})
export class KatalogModule {}
