import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { BiletDogrulamaSayfasiComponent } from './bilet-dogrulama-sayfasi.component';

const rotalar: Routes = [{ path: ':kod', component: BiletDogrulamaSayfasiComponent }];

/** /bilet/:kod (K30). QR'ı okutan herkes açar; satın alma modülünü indirmesin diye ayrı ve küçük tutulur. */
@NgModule({
  declarations: [BiletDogrulamaSayfasiComponent],
  imports: [SharedModule, RouterModule.forChild(rotalar)],
})
export class BiletDogrulamaModule {}
