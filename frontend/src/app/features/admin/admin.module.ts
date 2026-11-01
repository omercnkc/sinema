import { NgModule } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../../shared/shared.module';
import { AdminRoutingModule } from './admin-routing.module';
import { SalonFormuComponent } from './components/salon-formu/salon-formu.component';
import { SubeFormuComponent } from './components/sube-formu/sube-formu.component';
import { AdminDuzeniComponent } from './containers/admin-duzeni/admin-duzeni.component';
import { FilmYonetimiSayfasiComponent } from './containers/film-yonetimi-sayfasi/film-yonetimi-sayfasi.component';
import { SubeYonetimiSayfasiComponent } from './containers/sube-yonetimi-sayfasi/sube-yonetimi-sayfasi.component';
import { YoneticiEklemeSayfasiComponent } from './containers/yonetici-ekleme-sayfasi/yonetici-ekleme-sayfasi.component';

/**
 * Yönetim paneli (M7). Tembel yüklenir ve kökte `RolGuard.canLoad` ile korunur: yönetici olmayan kullanıcı bu
 * paketi hiç indirmez. Asıl yetki kontrolü sunucudadır (gateway + servislerde ADMIN).
 */
@NgModule({
  declarations: [
    AdminDuzeniComponent,
    FilmYonetimiSayfasiComponent,
    SubeYonetimiSayfasiComponent,
    SubeFormuComponent,
    SalonFormuComponent,
    YoneticiEklemeSayfasiComponent,
  ],
  imports: [SharedModule, NgbPaginationModule, AdminRoutingModule],
})
export class AdminModule {}
