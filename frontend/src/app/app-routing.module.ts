import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SayfaBulunamadiComponent } from './layout/sayfa-bulunamadi/sayfa-bulunamadi.component';

/** Kök rotalar (§8.3). Özellik modülleri tembel yüklenir: ilk açılışta yalnızca gereken kod iner. */
const rotalar: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'filmler' },
  { path: 'filmler', loadChildren: () => import('./features/katalog/katalog.module').then((m) => m.KatalogModule) },
  { path: '**', component: SayfaBulunamadiComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(rotalar)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
