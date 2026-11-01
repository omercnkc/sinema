import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';

/** Kaydedilmemiş değişikliği olan sayfa (ör. düzen editörü). */
export interface KaydedilmemisDegisiklikli {
  readonly kaydedilmemisDegisiklikVar: boolean;
}

const UYARI = 'Kaydedilmemiş değişiklikler var. Sayfadan ayrılırsanız kaybolacak. Devam edilsin mi?';

/**
 * Kaydedilmemiş değişiklik varken uygulama içi gezinmeden önce onay ister (class tabanlı, Angular 13). Sekme
 * kapatma/yenileme için sayfa ayrıca `beforeunload` dinler; bu koruma yalnızca yönlendiriciyi kapsar.
 */
@Injectable({ providedIn: 'root' })
export class KaydedilmemisDegisiklikGuard implements CanDeactivate<KaydedilmemisDegisiklikli> {
  canDeactivate(sayfa: KaydedilmemisDegisiklikli): boolean {
    return !sayfa.kaydedilmemisDegisiklikVar || window.confirm(UYARI);
  }
}
