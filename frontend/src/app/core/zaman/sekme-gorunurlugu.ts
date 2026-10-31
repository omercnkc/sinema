import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { fromEvent, Observable } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, startWith } from 'rxjs/operators';

/**
 * Sekme görünür mü? Yoklama yapan akışlar (koltuk doluluğu) sekme arka plandayken durur, öne gelince hemen
 * yeniler: gereksiz istek ve pil tüketimi olmaz (§8.5).
 */
@Injectable({ providedIn: 'root' })
export class SekmeGorunurlugu {
  readonly gorunur$: Observable<boolean>;

  constructor(@Inject(DOCUMENT) belge: Document) {
    this.gorunur$ = fromEvent(belge, 'visibilitychange').pipe(
      startWith(null),
      map(() => belge.visibilityState !== 'hidden'),
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }
}
