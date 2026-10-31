import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Eşleşmeyen her adres (404). */
@Component({
  selector: 'app-sayfa-bulunamadi',
  template: `
    <section class="text-center py-5">
      <h1 class="display-6">Sayfa bulunamadı</h1>
      <p class="text-muted">Aradığınız sayfa taşınmış ya da hiç var olmamış olabilir.</p>
      <a routerLink="/filmler" class="btn btn-primary">Filmlere dön</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SayfaBulunamadiComponent {}
