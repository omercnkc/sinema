import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { SunucuSaati } from '../../../../core/zaman/sunucu-saati.service';
import { Sayfa } from '../../../../shared/models/sayfa.models';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { DURUM_ROZETLERI, GorunenDurum, gorunenDurum, Rezervasyon } from '../../models/rezervasyon.models';
import { REZERVASYON_SAYFA_BOYUTU, RezervasyonService } from '../../services/rezervasyon.service';

/** Listede bir satır: rezervasyon ve ekranda gösterilen durumu (şablonda hesap yok). */
export interface RezervasyonSatiri {
  readonly rezervasyon: Rezervasyon;
  readonly durum: GorunenDurum;
  readonly koltuklar: string;
}

/**
 * /rezervasyonlarim (smart, giriş gerekir): kullanıcının işlemleri en yeni önce, sayfalı. Sayfa numarası adres
 * çubuğunda (`?sayfa=`, 1'den). Satır, işlem sayfasına (tutma ekranı ya da bilet) götürür.
 */
@Component({
  selector: 'app-rezervasyonlarim-sayfasi',
  templateUrl: './rezervasyonlarim-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RezervasyonlarimSayfasiComponent {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly sayfaBoyutu = REZERVASYON_SAYFA_BOYUTU;
  readonly rozetler = DURUM_ROZETLERI;
  readonly durum$: Observable<YuklemeDurumu<Sayfa<RezervasyonSatiri>>>;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);

  constructor(
    rezervasyonService: RezervasyonService,
    private readonly sunucuSaati: SunucuSaati,
    private readonly router: Router,
    private readonly rota: ActivatedRoute,
  ) {
    const sayfa$ = rota.queryParamMap.pipe(
      map((parametreler) => Number(parametreler.get('sayfa'))),
      map((sayfaNo) => (Number.isInteger(sayfaNo) && sayfaNo > 1 ? sayfaNo - 1 : 0)),
      distinctUntilChanged(),
    );
    this.durum$ = combineLatest([sayfa$, this.tekrarDene$]).pipe(
      switchMap(([sayfa]) => yuklemeDurumu(rezervasyonService.listele(sayfa).pipe(
        tap((cevap) => cevap.content[0] && this.sunucuSaati.farkiGuncelle(cevap.content[0].serverTime)),
        map((cevap) => ({ ...cevap, content: cevap.content.map((r) => this.satir(r)) })),
      ))),
    );
  }

  /** ng-bootstrap sayfalama 1'den sayar; ilk sayfa adreste yazılmaz. */
  sayfaDegisti(sayfaNo: number): void {
    this.router.navigate([], { relativeTo: this.rota, queryParams: { sayfa: sayfaNo > 1 ? sayfaNo : null } });
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  satirIzle(_: number, satir: RezervasyonSatiri): number {
    return satir.rezervasyon.reservationId;
  }

  private satir(rezervasyon: Rezervasyon): RezervasyonSatiri {
    return {
      rezervasyon,
      durum: gorunenDurum(rezervasyon, this.sunucuSaati.simdi()),
      koltuklar: rezervasyon.seats.map((koltuk) => koltuk.label).join(', '),
    };
  }
}
