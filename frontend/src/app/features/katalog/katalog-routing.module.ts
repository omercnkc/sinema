import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FilmDetaySayfasiComponent } from './containers/film-detay-sayfasi/film-detay-sayfasi.component';
import { FilmListesiSayfasiComponent } from './containers/film-listesi-sayfasi/film-listesi-sayfasi.component';

const rotalar: Routes = [
  { path: '', component: FilmListesiSayfasiComponent },
  { path: ':id', component: FilmDetaySayfasiComponent },
];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class KatalogRoutingModule {}
