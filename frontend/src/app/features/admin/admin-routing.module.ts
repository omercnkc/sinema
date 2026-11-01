import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminDuzeniComponent } from './containers/admin-duzeni/admin-duzeni.component';
import { DuzenSayfasiComponent } from './containers/duzen-sayfasi/duzen-sayfasi.component';
import { FilmFormuSayfasiComponent } from './containers/film-formu-sayfasi/film-formu-sayfasi.component';
import { FilmYonetimiSayfasiComponent } from './containers/film-yonetimi-sayfasi/film-yonetimi-sayfasi.component';
import { SubeYonetimiSayfasiComponent } from './containers/sube-yonetimi-sayfasi/sube-yonetimi-sayfasi.component';
import { KaydedilmemisDegisiklikGuard } from './services/kaydedilmemis-degisiklik.guard';
import { YoneticiEklemeSayfasiComponent } from './containers/yonetici-ekleme-sayfasi/yonetici-ekleme-sayfasi.component';

// Rol denetimi kökteki rotada (canLoad + canActivate); alt rotalar ondan miras alır
const rotalar: Routes = [
  {
    path: '',
    component: AdminDuzeniComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'filmler' },
      { path: 'filmler', component: FilmYonetimiSayfasiComponent },
      // 'yeni' sabit yolu ':id'den önce: aksi halde "yeni" bir kimlik sanılırdı
      { path: 'filmler/yeni', component: FilmFormuSayfasiComponent },
      { path: 'filmler/:id', component: FilmFormuSayfasiComponent },
      { path: 'subeler', component: SubeYonetimiSayfasiComponent },
      {
        path: 'subeler/salonlar/:salonId/duzen',
        component: DuzenSayfasiComponent,
        canDeactivate: [KaydedilmemisDegisiklikGuard],
      },
      { path: 'yoneticiler', component: YoneticiEklemeSayfasiComponent },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
