import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BehaviorSubject, combineLatest, EMPTY, Observable, of, ReplaySubject, timer } from 'rxjs';
import { catchError, distinctUntilChanged, exhaustMap, map, scan, shareReplay, startWith, switchMap } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import { Kullanici } from '../../../../core/oturum/oturum.models';
import { OturumService } from '../../../../core/oturum/oturum.service';
import { SekmeGorunurlugu } from '../../../../core/zaman/sekme-gorunurlugu';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import {
  DolulukCevabi,
  dolulukHaritasi,
  haritaKur,
  KoltukDurumu,
  KoltukHaritasiGorunumu,
  SeansDuzeni,
} from '../../models/koltuk-haritasi.models';
import { KoltukHaritasiService } from '../../services/koltuk-haritasi.service';

/** Son başarılı doluluk korunur; yenileme başarısızsa harita eski veriyle kalır ve uyarı çıkar. */
interface DolulukDurumu {
  readonly dolular: ReadonlyMap<number, KoltukDurumu>;
  readonly hata: boolean;
}

interface KoltukSecimiGorunumu {
  readonly duzen: YuklemeDurumu<SeansDuzeni>;
  readonly harita: KoltukHaritasiGorunumu | null;
  readonly dolulukHatasi: boolean;
  readonly kullanici: Kullanici | null;
}

const BASLANGIC_DOLULUGU: DolulukDurumu = { dolular: new Map(), hata: false };

/**
 * Koltuk seçimi (smart). Düzen bir kez alınır (seansa mühürlü, değişmez); doluluk 5 sn'de bir yoklanır (K33), sekme
 * arka plandayken durur. Doğruluk haritanın tazeliğine bağlı değildir: eski haritada dolu koltuk seçilirse tutma
 * isteği 409 döner (§4.9). Seçim ve tutma M6 alt görev 2'de.
 */
@Component({
  selector: 'app-koltuk-secimi',
  templateUrl: './koltuk-secimi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KoltukSecimiComponent {
  readonly gorunum$: Observable<KoltukSecimiGorunumu>;
  /** Misafir giriş yapınca bu seansa döner */
  donusAdresi = '/';

  private readonly seansId$ = new ReplaySubject<number>(1);
  private readonly secim$ = new BehaviorSubject<ReadonlySet<number>>(new Set());
  private readonly duzenTekrar$ = new BehaviorSubject<void>(undefined);

  @Input() set seansId(deger: number) {
    this.donusAdresi = `/seans/${deger}`;
    this.seansId$.next(deger);
  }

  constructor(haritaService: KoltukHaritasiService, sekme: SekmeGorunurlugu, oturumService: OturumService) {
    const seansId$ = this.seansId$.pipe(distinctUntilChanged());
    const duzen$ = combineLatest([seansId$, this.duzenTekrar$]).pipe(
      switchMap(([seansId]) => yuklemeDurumu(haritaService.duzen(seansId))),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const doluluk$ = combineLatest([seansId$, sekme.gorunur$]).pipe(
      switchMap(([seansId, gorunur]) => (gorunur ? KoltukSecimiComponent.yokla(haritaService, seansId) : EMPTY)),
      scan((onceki: DolulukDurumu, cevap: DolulukCevabi | null): DolulukDurumu =>
        cevap ? { dolular: dolulukHaritasi(cevap), hata: false } : { ...onceki, hata: true }, BASLANGIC_DOLULUGU),
      startWith(BASLANGIC_DOLULUGU),
    );
    this.gorunum$ = combineLatest([duzen$, doluluk$, this.secim$, oturumService.kullanici$]).pipe(
      map(([duzen, doluluk, secim, kullanici]) => ({
        duzen,
        harita: duzen.veri ? haritaKur(duzen.veri, doluluk.dolular, secim) : null,
        dolulukHatasi: doluluk.hata,
        kullanici,
      })),
    );
  }

  duzeniTekrarDene(): void {
    this.duzenTekrar$.next();
  }

  /**
   * Hemen, sonra her aralıkta bir doluluk; önceki istek sürerken yenisi başlamaz (`exhaustMap`). Hata akışı
   * bitirmez: null olarak bildirilir.
   */
  private static yokla(haritaService: KoltukHaritasiService, seansId: number): Observable<DolulukCevabi | null> {
    return timer(0, environment.dolulukYenilemeMs).pipe(
      exhaustMap(() => haritaService.doluluk(seansId).pipe(catchError(() => of(null)))),
    );
  }
}
