import { ChangeDetectionStrategy, Component } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { UygulamaHatasi } from '../../core/hata/uygulama-hatasi';
import { IS_SAAT_DILIMI } from '../../core/zaman/is-saat-dilimi';
import { YuklemeDurumu, yuklemeDurumu } from '../../shared/yukleme/yukleme-durumu';
import { BiletDogrulamaService, BiletDogrulamasi } from './bilet-dogrulama.service';

/**
 * /bilet/:kod (herkese açık): QR okutulunca açılır, biletin geçerli olup olmadığını ve üzerindeki bilgileri gösterir
 * (K30). Bilinmeyen, ödenmemiş ya da süresi dolmuş kod ayırt edilmeden "geçersiz" (404). Kişisel veri gösterilmez.
 */
@Component({
  selector: 'app-bilet-dogrulama-sayfasi',
  templateUrl: './bilet-dogrulama-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BiletDogrulamaSayfasiComponent {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly durum$: Observable<YuklemeDurumu<BiletDogrulamasi>>;
  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);

  constructor(servis: BiletDogrulamaService, rota: ActivatedRoute) {
    const kod$ = rota.paramMap.pipe(map((p) => (p.get('kod') ?? '').trim()), distinctUntilChanged());
    this.durum$ = combineLatest([kod$, this.tekrarDene$]).pipe(
      switchMap(([kod]) => yuklemeDurumu(servis.dogrula(kod))),
    );
  }

  /** 404 = geçersiz bilet (hata değil, sonuç) */
  gecersiz(hata: UygulamaHatasi): boolean {
    return hata.durum === HttpStatusCode.NotFound;
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  koltukIzle(_: number, koltuk: string): string {
    return koltuk;
  }
}
