import { NgModule } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { QRCodeModule } from 'angularx-qrcode';
import { SharedModule } from '../../shared/shared.module';
import { BiletKartiComponent } from './components/bilet-karti/bilet-karti.component';
import { KoltukSecimiComponent } from './containers/koltuk-secimi/koltuk-secimi.component';
import { OdemeSonucuSayfasiComponent } from './containers/odeme-sonucu-sayfasi/odeme-sonucu-sayfasi.component';
import { RezervasyonlarimSayfasiComponent } from './containers/rezervasyonlarim-sayfasi/rezervasyonlarim-sayfasi.component';
import { RezervasyonSayfasiComponent } from './containers/rezervasyon-sayfasi/rezervasyon-sayfasi.component';
import { SeansSayfasiComponent } from './containers/seans-sayfasi/seans-sayfasi.component';
import { RezervasyonRoutingModule } from './rezervasyon-routing.module';

/** Satın alma akışı (§8.2 booking): seans ve koltuk seçimi, tutma, rezervasyonlarım, bilet. Kökte bağlanır. */
@NgModule({
  declarations: [SeansSayfasiComponent, KoltukSecimiComponent, RezervasyonSayfasiComponent,
    OdemeSonucuSayfasiComponent, RezervasyonlarimSayfasiComponent, BiletKartiComponent],
  imports: [SharedModule, NgbPaginationModule, QRCodeModule, RezervasyonRoutingModule],
})
export class RezervasyonModule {}
