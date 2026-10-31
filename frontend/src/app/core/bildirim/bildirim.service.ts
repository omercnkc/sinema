import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type BildirimTuru = 'basari' | 'hata' | 'bilgi';

export interface Bildirim {
  readonly kimlik: number;
  readonly tur: BildirimTuru;
  readonly mesaj: string;
}

/**
 * Ekranın köşesindeki kısa bildirimler (toast). Durum servis + BehaviorSubject ile tutulur (rules §6.4); gösterimi
 * layout'taki BildirimAlaniComponent yapar. Aynı mesaj üst üste gelirse (ör. ağ kesikken art arda istekler) bir kez
 * gösterilir.
 */
@Injectable({ providedIn: 'root' })
export class BildirimService {
  private readonly bildirimler = new BehaviorSubject<readonly Bildirim[]>([]);
  private sonKimlik = 0;

  readonly bildirimler$: Observable<readonly Bildirim[]> = this.bildirimler.asObservable();

  basari(mesaj: string): void {
    this.ekle('basari', mesaj);
  }

  hata(mesaj: string): void {
    this.ekle('hata', mesaj);
  }

  bilgi(mesaj: string): void {
    this.ekle('bilgi', mesaj);
  }

  kaldir(kimlik: number): void {
    this.bildirimler.next(this.bildirimler.value.filter((bildirim) => bildirim.kimlik !== kimlik));
  }

  private ekle(tur: BildirimTuru, mesaj: string): void {
    const mevcut = this.bildirimler.value;
    if (mevcut.some((bildirim) => bildirim.tur === tur && bildirim.mesaj === mesaj)) {
      return;
    }
    this.bildirimler.next([...mevcut, { kimlik: ++this.sonKimlik, tur, mesaj }]);
  }
}
