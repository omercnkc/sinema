import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Observable, ReplaySubject, timer } from 'rxjs';
import { distinctUntilChanged, map, switchMap, takeWhile, tap } from 'rxjs/operators';
import { SunucuSaati } from '../../../core/zaman/sunucu-saati.service';

const SANIYE_MS = 1000;
const SANIYE_PER_DAKIKA = 60;

/**
 * Bitiş anına geri sayım (mm:ss), sunucu saatine göre. Sıfıra inince bir kez `bitti` yayar. Ekran okuyucu her
 * saniyeyi okumasın diye `role="timer"` (varsayılan aria-live kapalı); kalan süre ayrıca dakikada bir bildirilir.
 */
@Component({
  selector: 'app-geri-sayim',
  template: `
    <ng-container *ngIf="kalan$ | async as kalan">
      <span role="timer" class="geri-sayim" [class.text-danger]="kalan.saniye <= uyariSaniyesi">{{ kalan.metin }}</span>
      <span class="visually-hidden" aria-live="polite">{{ kalan.dakikaMetni }}</span>
    </ng-container>
  `,
  styles: ['.geri-sayim { font-variant-numeric: tabular-nums; font-weight: 700; }'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GeriSayimComponent {
  /** Son bu kadar saniyede kırmızı */
  @Input() uyariSaniyesi = 30;
  @Output() readonly bitti = new EventEmitter<void>();

  readonly kalan$: Observable<{ saniye: number; metin: string; dakikaMetni: string }>;
  private readonly bitis$ = new ReplaySubject<number>(1);

  @Input() set bitis(zaman: string) {
    this.bitis$.next(Date.parse(zaman));
  }

  constructor(sunucuSaati: SunucuSaati) {
    this.kalan$ = this.bitis$.pipe(
      switchMap((bitis) => timer(0, SANIYE_MS).pipe(
        map(() => Math.max(0, Math.ceil((bitis - sunucuSaati.simdi()) / SANIYE_MS))),
        distinctUntilChanged(),
        takeWhile((saniye) => saniye > 0, true),
        tap((saniye) => saniye === 0 && this.bitti.emit()),
      )),
      map((saniye) => ({ saniye, metin: GeriSayimComponent.bicimle(saniye), dakikaMetni: GeriSayimComponent.dakika(saniye) })),
    );
  }

  private static bicimle(saniye: number): string {
    const dakika = Math.floor(saniye / SANIYE_PER_DAKIKA);
    return `${dakika}:${String(saniye % SANIYE_PER_DAKIKA).padStart(2, '0')}`;
  }

  /** Dakika değiştikçe değişen metin: ekran okuyucu yalnızca o zaman okur. */
  private static dakika(saniye: number): string {
    const dakika = Math.ceil(saniye / SANIYE_PER_DAKIKA);
    return saniye > 0 ? `Kalan süre yaklaşık ${dakika} dakika` : 'Süre doldu';
  }
}
