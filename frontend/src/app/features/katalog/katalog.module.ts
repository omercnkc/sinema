import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { FilmListesiSayfasiComponent } from './containers/film-listesi-sayfasi/film-listesi-sayfasi.component';
import { KatalogRoutingModule } from './katalog-routing.module';

/** Film listesi, film detayı ve seans seçimi (§8.2); /filmler altında tembel yüklenir. */
@NgModule({
  declarations: [FilmListesiSayfasiComponent],
  imports: [SharedModule, KatalogRoutingModule],
})
export class KatalogModule {}
