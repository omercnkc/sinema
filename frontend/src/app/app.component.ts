import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Kök bileşen: yalnızca sayfa düzenini ve yönlendirilen sayfayı taşır (layout M5 alt görev 3'te). */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {}
