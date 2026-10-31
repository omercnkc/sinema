import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HataMesajiComponent } from './components/hata-mesaji/hata-mesaji.component';
import { YukleniyorComponent } from './components/yukleniyor/yukleniyor.component';
import { AlanHatasiPipe } from './form/alan-hatasi.pipe';
import { FilmAfisiComponent } from './components/film-afisi/film-afisi.component';
import { TmdbAfisPipe } from './pipes/tmdb-afis.pipe';

const BILESENLER = [YukleniyorComponent, HataMesajiComponent, FilmAfisiComponent, AlanHatasiPipe, TmdbAfisPipe];

/**
 * Feature modüllerinin ortak ihtiyaçları (rules §6.3): yeniden kullanılan bileşen, direktif ve pipe'lar burada
 * tanımlanıp dışa açılır. ng-bootstrap'in yalnızca kullanılan modülleri (NgbPaginationModule vb.) eklendikçe buraya
 * girer; NgbModule'ün tamamı içe aktarılmaz (paket boyutu, §8.1).
 */
@NgModule({
  declarations: BILESENLER,
  imports: [CommonModule],
  exports: [CommonModule, ReactiveFormsModule, RouterModule, ...BILESENLER],
})
export class SharedModule {}
