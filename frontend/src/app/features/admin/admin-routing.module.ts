import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminDuzeniComponent } from './containers/admin-duzeni/admin-duzeni.component';
import { YoneticiEklemeSayfasiComponent } from './containers/yonetici-ekleme-sayfasi/yonetici-ekleme-sayfasi.component';

// Rol denetimi kökteki rotada (canLoad + canActivate); alt rotalar ondan miras alır
const rotalar: Routes = [
  {
    path: '',
    component: AdminDuzeniComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'yoneticiler' },
      { path: 'yoneticiler', component: YoneticiEklemeSayfasiComponent },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(rotalar)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
