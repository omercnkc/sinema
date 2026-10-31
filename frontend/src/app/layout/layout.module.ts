import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgbCollapseModule, NgbDropdownModule, NgbToastModule } from '@ng-bootstrap/ng-bootstrap';
import { BildirimAlaniComponent } from './bildirim-alani/bildirim-alani.component';
import { FooterComponent } from './footer/footer.component';
import { HeaderComponent } from './header/header.component';
import { SayfaBulunamadiComponent } from './sayfa-bulunamadi/sayfa-bulunamadi.component';

/**
 * Sayfa iskeleti: üst menü, alt bilgi, bildirim alanı ve 404 sayfası (rules §6.3). Yalnızca AppModule içe aktarır.
 * Yalnızca ihtiyacı olan modülleri alır (SharedModule'ün form modülüne ihtiyacı yok).
 */
@NgModule({
  declarations: [HeaderComponent, FooterComponent, BildirimAlaniComponent, SayfaBulunamadiComponent],
  imports: [CommonModule, RouterModule, NgbCollapseModule, NgbDropdownModule, NgbToastModule],
  exports: [HeaderComponent, FooterComponent, BildirimAlaniComponent],
})
export class LayoutModule {}
