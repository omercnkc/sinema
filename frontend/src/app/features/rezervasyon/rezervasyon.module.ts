import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { SeansSayfasiComponent } from './containers/seans-sayfasi/seans-sayfasi.component';
import { RezervasyonRoutingModule } from './rezervasyon-routing.module';

/** Koltuk seçimi (/seans/:id); M6'da harita, tutma, rezervasyonlarım ve bilet bu modüle eklenir (§8.2 booking). */
@NgModule({
  declarations: [SeansSayfasiComponent],
  imports: [SharedModule, RezervasyonRoutingModule],
})
export class RezervasyonModule {}
