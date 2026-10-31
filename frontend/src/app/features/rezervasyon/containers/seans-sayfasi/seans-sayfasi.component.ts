import { ChangeDetectionStrategy, Component } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, throwError } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { SeansDetayi } from '../../models/seans-detayi.models';
import { SeansDetayService } from '../../services/seans-detay.service';

const SEANS_YOK = new UygulamaHatasi(HttpStatusCode.NotFound, 'SHOWTIME_NOT_FOUND', 'Seans bulunamadı.');

/**
 * /seans/:id (smart): seçilen seansın başlığı ve satış açıksa koltuk seçimi. Misafir de açabilir (K3: haritayı
 * görür, seçemez).
 */
@Component({
  selector: 'app-seans-sayfasi',
  templateUrl: './seans-sayfasi.component.html',
  styleUrls: ['./seans-sayfasi.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeansSayfasiComponent {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly durum$: Observable<YuklemeDurumu<SeansDetayi>>;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);

  constructor(seansDetayService: SeansDetayService, rota: ActivatedRoute) {
    const seansId$ = rota.paramMap.pipe(map((parametreler) => Number(parametreler.get('id'))), distinctUntilChanged());
    this.durum$ = combineLatest([seansId$, this.tekrarDene$]).pipe(
      switchMap(([seansId]) => yuklemeDurumu(
        Number.isInteger(seansId) && seansId > 0 ? seansDetayService.getir(seansId) : throwError(() => SEANS_YOK),
      )),
    );
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  tekrarDenenebilir(hata: UygulamaHatasi): boolean {
    return hata.durum !== HttpStatusCode.NotFound && hata.durum !== HttpStatusCode.BadRequest;
  }
}
