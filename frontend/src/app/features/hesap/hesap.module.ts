import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { GirisSayfasiComponent } from './containers/giris-sayfasi/giris-sayfasi.component';
import { KayitSayfasiComponent } from './containers/kayit-sayfasi/kayit-sayfasi.component';
import { HesapRoutingModule } from './hesap-routing.module';

/** Giriş ve kayıt sayfaları (tembel yüklenir; giriş yapmış kullanıcı bu sayfalara giremez). */
@NgModule({
  declarations: [GirisSayfasiComponent, KayitSayfasiComponent],
  imports: [SharedModule, HesapRoutingModule],
})
export class HesapModule {}
