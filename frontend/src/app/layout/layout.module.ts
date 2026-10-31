import { NgModule } from '@angular/core';
import { NgbCollapseModule, NgbToastModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../shared/shared.module';
import { BildirimAlaniComponent } from './bildirim-alani/bildirim-alani.component';
import { FooterComponent } from './footer/footer.component';
import { HeaderComponent } from './header/header.component';
import { SayfaBulunamadiComponent } from './sayfa-bulunamadi/sayfa-bulunamadi.component';

/** Sayfa iskeleti: üst menü, alt bilgi, bildirim alanı ve 404 sayfası (rules §6.3). Yalnızca AppModule içe aktarır. */
@NgModule({
  declarations: [HeaderComponent, FooterComponent, BildirimAlaniComponent, SayfaBulunamadiComponent],
  imports: [SharedModule, NgbCollapseModule, NgbToastModule],
  exports: [HeaderComponent, FooterComponent, BildirimAlaniComponent],
})
export class LayoutModule {}
