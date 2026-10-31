import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GirisGerekliGuard } from '../../core/oturum/oturum.guards';
import { RezervasyonSayfasiComponent } from './containers/rezervasyon-sayfasi/rezervasyon-sayfasi.component';
import { SeansSayfasiComponent } from './containers/seans-sayfasi/seans-sayfasi.component';

// Modül kökte ('') bağlanır; tam yollar burada (seans, rezervasyonlarım, ödeme sonucu aynı akışın parçaları)
const rotalar: Routes = [
  { path: 'seans/:id', component: SeansSayfasiComponent },
  { path: 'rezervasyonlarim/:id', component: RezervasyonSayfasiComponent, canActivate: [GirisGerekliGuard] },
];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class RezervasyonRoutingModule {}
