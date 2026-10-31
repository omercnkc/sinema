import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SayfaBulunamadiComponent } from './layout/sayfa-bulunamadi/sayfa-bulunamadi.component';

/** Kök rotalar (§8.3). Özellik modülleri tembel yüklenir: ilk açılışta yalnızca gereken kod iner. */
const rotalar: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'filmler' },
  { path: 'filmler', loadChildren: () => import('./features/katalog/katalog.module').then((m) => m.KatalogModule) },
  { path: 'bilet', loadChildren: () => import('./features/bilet-dogrulama/bilet-dogrulama.module').then((m) => m.BiletDogrulamaModule) },
  // Kökte birden çok yolu olan modüller ('' altında, yollar modülün içinde). Eşleşmeyen yol için yönlendirici sıradaki
  // modülü dener: /seans/:id ve /rezervasyonlarim → rezervasyon, /giris ve /kayit → hesap
  { path: '', loadChildren: () => import('./features/rezervasyon/rezervasyon.module').then((m) => m.RezervasyonModule) },
  { path: '', loadChildren: () => import('./features/hesap/hesap.module').then((m) => m.HesapModule) },
  { path: '**', component: SayfaBulunamadiComponent },
];

@NgModule({
  // Yeni sayfada en üstten başlanır, geri tuşunda önceki kaydırma konumuna dönülür
  imports: [RouterModule.forRoot(rotalar, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule],
})
export class AppRoutingModule {}
