import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MisafirGuard } from '../../core/oturum/oturum.guards';
import { GirisSayfasiComponent } from './containers/giris-sayfasi/giris-sayfasi.component';
import { KayitSayfasiComponent } from './containers/kayit-sayfasi/kayit-sayfasi.component';

const rotalar: Routes = [
  { path: 'giris', component: GirisSayfasiComponent, canActivate: [MisafirGuard] },
  { path: 'kayit', component: KayitSayfasiComponent, canActivate: [MisafirGuard] },
];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class HesapRoutingModule {}
