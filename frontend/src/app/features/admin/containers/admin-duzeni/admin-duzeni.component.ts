import { ChangeDetectionStrategy, Component } from '@angular/core';

interface MenuOgesi {
  readonly etiket: string;
  readonly yol: string;
}

/** Yan menü. Bölümler ilgili alt görevle eklenir (filmler, şubeler ve salonlar, seanslar). */
const MENU: readonly MenuOgesi[] = [{ etiket: 'Yöneticiler', yol: 'yoneticiler' }];

/** Yönetim panelinin iskeleti: md ve üstünde solda dikey menü, küçük ekranda üstte yatay menü; sağda alt sayfa. */
@Component({
  selector: 'app-admin-duzeni',
  templateUrl: './admin-duzeni.component.html',
  styleUrls: ['./admin-duzeni.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDuzeniComponent {
  readonly menu = MENU;

  menuIzi(_: number, oge: MenuOgesi): string {
    return oge.yol;
  }
}
