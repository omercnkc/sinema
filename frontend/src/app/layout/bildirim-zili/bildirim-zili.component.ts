import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { UygulamaBildirimi } from '../../core/bildirim-kutusu/bildirim-kutusu.models';
import { BildirimKutusuService } from '../../core/bildirim-kutusu/bildirim-kutusu.service';
import { IS_SAAT_DILIMI } from '../../core/zaman/is-saat-dilimi';
import { Sayfa } from '../../shared/models/sayfa.models';
import { YuklemeDurumu, yuklemeDurumu } from '../../shared/yukleme/yukleme-durumu';

/**
 * Üst menüdeki bildirim zili (giriş yapmış kullanıcı). Rozet okunmamış sayısını gösterir; menü açılınca son
 * bildirimler yüklenir. Bildirime tıklanınca okundu işaretlenir ve ilgili bilete gidilir.
 */
@Component({
  selector: 'app-bildirim-zili',
  templateUrl: './bildirim-zili.component.html',
  styleUrls: ['./bildirim-zili.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BildirimZiliComponent {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly okunmamisSayisi$: Observable<number>;
  readonly liste$: Observable<YuklemeDurumu<Sayfa<UygulamaBildirimi>>>;

  private readonly acildi$ = new Subject<void>();

  constructor(private readonly kutuService: BildirimKutusuService, private readonly router: Router) {
    this.okunmamisSayisi$ = kutuService.okunmamisSayisi$;
    this.liste$ = this.acildi$.pipe(switchMap(() => yuklemeDurumu(kutuService.sonBildirimler())));
  }

  menuDegisti(acik: boolean): void {
    if (acik) {
      this.acildi$.next();
    }
  }

  /** Tek seferlik HTTP çağrısı, tamamlanır; abonelik kendiliğinden kapanır. Okundu işareti başarısız olsa da gidilir. */
  sec(bildirim: UygulamaBildirimi): void {
    if (!bildirim.read) {
      this.kutuService.okundu(bildirim.id).subscribe({ error: () => undefined });
    }
    if (bildirim.reservationId !== null) {
      this.router.navigate(['/rezervasyonlarim', bildirim.reservationId]);
    }
  }

  bildirimIzle(_: number, bildirim: UygulamaBildirimi): number {
    return bildirim.id;
  }
}
