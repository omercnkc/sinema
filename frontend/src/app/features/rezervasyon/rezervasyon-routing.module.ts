import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SeansSayfasiComponent } from './containers/seans-sayfasi/seans-sayfasi.component';

const rotalar: Routes = [{ path: ':id', component: SeansSayfasiComponent }];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class RezervasyonRoutingModule {}
