import { NgModule } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../../shared/shared.module';
import { AdminRoutingModule } from './admin-routing.module';
import { DuzenEditoruComponent } from './components/duzen-editoru/duzen-editoru.component';
import { FilmFormuComponent } from './components/film-formu/film-formu.component';
import { SalonFormuComponent } from './components/salon-formu/salon-formu.component';
import { SubeFormuComponent } from './components/sube-formu/sube-formu.component';
import { AdminDuzeniComponent } from './containers/admin-duzeni/admin-duzeni.component';
import { DuzenSayfasiComponent } from './containers/duzen-sayfasi/duzen-sayfasi.component';
import { FilmFormuSayfasiComponent } from './containers/film-formu-sayfasi/film-formu-sayfasi.component';
import { FilmYonetimiSayfasiComponent } from './containers/film-yonetimi-sayfasi/film-yonetimi-sayfasi.component';
import { SubeYonetimiSayfasiComponent } from './containers/sube-yonetimi-sayfasi/sube-yonetimi-sayfasi.component';
import { TmdbAramaComponent } from './containers/tmdb-arama/tmdb-arama.component';
import { YoneticiEklemeSayfasiComponent } from './containers/yonetici-ekleme-sayfasi/yonetici-ekleme-sayfasi.component';

/**
 * Yönetim paneli (M7). Tembel yüklenir ve kökte `RolGuard.canLoad` ile korunur: yönetici olmayan kullanıcı bu
 * paketi hiç indirmez. Asıl yetki kontrolü sunucudadır (gateway + servislerde ADMIN).
 */
@NgModule({
  declarations: [
    AdminDuzeniComponent,
    FilmYonetimiSayfasiComponent,
    FilmFormuSayfasiComponent,
    FilmFormuComponent,
    TmdbAramaComponent,
    SubeYonetimiSayfasiComponent,
    SubeFormuComponent,
    SalonFormuComponent,
    DuzenEditoruComponent,
    DuzenSayfasiComponent,
    YoneticiEklemeSayfasiComponent,
  ],
  imports: [SharedModule, NgbPaginationModule, AdminRoutingModule],
})
export class AdminModule {}
