import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Üst menü. Küçük ekranda menü düğmeyle açılır/kapanır (ng-bootstrap Collapse; Bootstrap'in JS'i kullanılmaz, K41).
 * Sağdaki oturum alanı (giriş/kayıt ya da kullanıcı menüsü) M5 alt görev 5'te dolar.
 */
@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  menuKapali = true;

  menuyuDegistir(): void {
    this.menuKapali = !this.menuKapali;
  }

  /** Mobilde bir bağlantıya tıklanınca menü kapanır (sayfa değişti). */
  menuyuKapat(): void {
    this.menuKapali = true;
  }
}
