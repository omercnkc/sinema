import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

/**
 * Feature modüllerinin ortak ihtiyaçları (rules §6.3): yeniden kullanılan bileşen, direktif ve pipe'lar burada
 * tanımlanıp dışa açılır. ng-bootstrap'in yalnızca kullanılan modülleri (NgbPaginationModule vb.) eklendikçe buraya
 * girer; NgbModule'ün tamamı içe aktarılmaz (paket boyutu, §8.1).
 */
@NgModule({
  imports: [CommonModule],
  exports: [CommonModule, ReactiveFormsModule, RouterModule],
})
export class SharedModule {}
