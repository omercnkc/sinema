import { NgModule } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../../shared/shared.module';
import { KoltukHaritasiComponent } from './components/koltuk-haritasi/koltuk-haritasi.component';
import { KoltukSecimiComponent } from './containers/koltuk-secimi/koltuk-secimi.component';
import { OdemeSonucuSayfasiComponent } from './containers/odeme-sonucu-sayfasi/odeme-sonucu-sayfasi.component';
import { RezervasyonlarimSayfasiComponent } from './containers/rezervasyonlarim-sayfasi/rezervasyonlarim-sayfasi.component';
import { RezervasyonSayfasiComponent } from './containers/rezervasyon-sayfasi/rezervasyon-sayfasi.component';
import { SeansSayfasiComponent } from './containers/seans-sayfasi/seans-sayfasi.component';
import { RezervasyonRoutingModule } from './rezervasyon-routing.module';

/** Satın alma akışı (§8.2 booking): seans ve koltuk seçimi, tutma, rezervasyonlarım, bilet. Kökte bağlanır. */
@NgModule({
  declarations: [SeansSayfasiComponent, KoltukSecimiComponent, KoltukHaritasiComponent, RezervasyonSayfasiComponent,
    OdemeSonucuSayfasiComponent, RezervasyonlarimSayfasiComponent],
  imports: [SharedModule, NgbPaginationModule, RezervasyonRoutingModule],
})
export class RezervasyonModule {}
