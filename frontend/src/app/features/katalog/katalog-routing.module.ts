import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FilmListesiSayfasiComponent } from './containers/film-listesi-sayfasi/film-listesi-sayfasi.component';

const rotalar: Routes = [{ path: '', component: FilmListesiSayfasiComponent }];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class KatalogRoutingModule {}
