import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AdminRoutingModule } from './admin-routing.module';
import { AdminDuzeniComponent } from './containers/admin-duzeni/admin-duzeni.component';
import { YoneticiEklemeSayfasiComponent } from './containers/yonetici-ekleme-sayfasi/yonetici-ekleme-sayfasi.component';

/**
 * Yönetim paneli (M7). Tembel yüklenir ve kökte `RolGuard.canLoad` ile korunur: yönetici olmayan kullanıcı bu
 * paketi hiç indirmez. Asıl yetki kontrolü sunucudadır (gateway + servislerde ADMIN).
 */
@NgModule({
  declarations: [AdminDuzeniComponent, YoneticiEklemeSayfasiComponent],
  imports: [SharedModule, AdminRoutingModule],
})
export class AdminModule {}
