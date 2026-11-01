import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Kullanici } from '../../core/oturum/oturum.models';
import { OturumService } from '../../core/oturum/oturum.service';

/**
 * Üst menü. Küçük ekranda menü düğmeyle açılır/kapanır (ng-bootstrap Collapse; Bootstrap'in JS'i kullanılmaz, K41).
 * Sağda misafire giriş/kayıt bağlantıları, giriş yapmış kullanıcıya açılır kullanıcı menüsü gösterilir.
 */
@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  menuKapali = true;
  readonly kullanici$: Observable<Kullanici | null>;
  /** "Yönetim" bağlantısı yalnızca yöneticiye görünür (yetkiyi asıl sunucu denetler). */
  readonly yonetici$: Observable<boolean>;

  constructor(private readonly oturumService: OturumService, private readonly router: Router) {
    this.kullanici$ = oturumService.kullanici$;
    this.yonetici$ = this.kullanici$.pipe(map((kullanici) => kullanici?.role === 'ADMIN'));
  }

  menuyuDegistir(): void {
    this.menuKapali = !this.menuKapali;
  }

  /** Mobilde bir bağlantıya tıklanınca menü kapanır (sayfa değişti). */
  menuyuKapat(): void {
    this.menuKapali = true;
  }

  /** Çıkış isteği tek seferlik HTTP çağrısıdır ve tamamlanır; abonelik kendiliğinden kapanır. */
  cikisYap(): void {
    this.menuyuKapat();
    this.oturumService.cikisYap().subscribe(() => this.router.navigateByUrl('/'));
  }
}
