import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SayfaBulunamadiComponent } from './layout/sayfa-bulunamadi/sayfa-bulunamadi.component';

/** Kök rotalar (§8.3). Özellik modülleri tembel yüklenir: ilk açılışta yalnızca gereken kod iner. */
const rotalar: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'filmler' },
  { path: 'filmler', loadChildren: () => import('./features/katalog/katalog.module').then((m) => m.KatalogModule) },
  // /giris ve /kayit: kök düzeyde iki yol, tek tembel modül (yollar modülün içinde)
  { path: '', loadChildren: () => import('./features/hesap/hesap.module').then((m) => m.HesapModule) },
  { path: '**', component: SayfaBulunamadiComponent },
];

@NgModule({
  // Yeni sayfada en üstten başlanır, geri tuşunda önceki kaydırma konumuna dönülür
  imports: [RouterModule.forRoot(rotalar, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule],
})
export class AppRoutingModule {}
