import { ChangeDetectionStrategy, Component, Input, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, EMPTY, merge, Observable, of, ReplaySubject, Subject, timer } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  exhaustMap,
  finalize,
  map,
  scan,
  shareReplay,
  startWith,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Kullanici } from '../../../../core/oturum/oturum.models';
import { OturumService } from '../../../../core/oturum/oturum.service';
import { SekmeGorunurlugu } from '../../../../core/zaman/sekme-gorunurlugu';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import {
  DolulukCevabi,
  dolulukHaritasi,
  HaritaHucresi,
  haritaKur,
  KoltukDurumu,
  KoltukHaritasiGorunumu,
  SeansDuzeni,
} from '../../models/koltuk-haritasi.models';
import { EN_FAZLA_KOLTUK, Rezervasyon } from '../../models/rezervasyon.models';
import { SeansDetayi } from '../../models/seans-detayi.models';
import { KoltukHaritasiService } from '../../services/koltuk-haritasi.service';
import { RezervasyonService } from '../../services/rezervasyon.service';

/** Son başarılı doluluk korunur; yenileme başarısızsa harita eski veriyle kalır ve uyarı çıkar. */
interface DolulukDurumu {
  readonly dolular: ReadonlyMap<number, KoltukDurumu>;
  readonly hata: boolean;
}

export interface TutmaHatasi {
  readonly mesaj: string;
  /** 409 ACTIVE_RESERVATION_EXISTS: kullanıcının bu seanstaki bekleyen işlemi */
  readonly bekleyenRezervasyonId: number | null;
}

interface TutmaDurumu {
  readonly gonderiliyor: boolean;
  readonly hata: TutmaHatasi | null;
}

interface KoltukSecimiGorunumu {
  readonly duzen: YuklemeDurumu<SeansDuzeni>;
  readonly harita: KoltukHaritasiGorunumu | null;
  readonly dolulukHatasi: boolean;
  readonly kullanici: Kullanici | null;
  /** Seçili ve hâlâ boş koltuklar (başkası alınca seçimden düşer) */
  readonly secilenler: readonly HaritaHucresi[];
  readonly secilenEtiketleri: string;
  readonly toplamTutar: number;
  readonly tutma: TutmaDurumu;
}

const BASLANGIC_DOLULUGU: DolulukDurumu = { dolular: new Map(), hata: false };
const BEKLENMEYEN_HATA = 'Koltuklar tutulamadı. Lütfen tekrar deneyin.';
const KURUS = 100;

/**
 * Koltuk seçimi ve tutma (smart). Düzen bir kez alınır (seansa mühürlü); doluluk 5 sn'de bir yoklanır (K33), sekme
 * arka plandayken durur. Doğruluk haritanın tazeliğine bağlı değildir: eski haritada dolu koltuk seçilirse tutma
 * 409 döner, harita hemen yenilenir ve o koltuklar seçimden çıkar (§4.9). Tutulan işlem sayfasına gidilir.
 */
@Component({
  selector: 'app-koltuk-secimi',
  templateUrl: './koltuk-secimi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KoltukSecimiComponent implements OnDestroy {
  readonly gorunum$: Observable<KoltukSecimiGorunumu>;
  /** Misafir giriş yapınca bu seansa döner */
  donusAdresi = '/';

  private seansBilgisi: SeansDetayi | null = null;
  private readonly seansId$ = new ReplaySubject<number>(1);
  private readonly secim$ = new BehaviorSubject<ReadonlySet<number>>(new Set());
  private readonly duzenTekrar$ = new BehaviorSubject<void>(undefined);
  private readonly hemenYenile$ = new Subject<void>();
  private readonly tutma$ = new BehaviorSubject<TutmaDurumu>({ gonderiliyor: false, hata: null });
  private readonly devam$ = new Subject<readonly number[]>();
  private readonly yikim$ = new Subject<void>();

  @Input() set seans(seans: SeansDetayi) {
    this.seansBilgisi = seans;
    this.donusAdresi = `/seans/${seans.id}`;
    this.seansId$.next(seans.id);
  }

  constructor(
    haritaService: KoltukHaritasiService,
    sekme: SekmeGorunurlugu,
    oturumService: OturumService,
    private readonly rezervasyonService: RezervasyonService,
    private readonly bildirimService: BildirimService,
    private readonly router: Router,
  ) {
    const seansId$ = this.seansId$.pipe(distinctUntilChanged());
    const duzen$ = combineLatest([seansId$, this.duzenTekrar$]).pipe(
      switchMap(([seansId]) => yuklemeDurumu(haritaService.duzen(seansId))),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    const doluluk$ = combineLatest([seansId$, sekme.gorunur$]).pipe(
      switchMap(([seansId, gorunur]) => (gorunur ? this.yokla(haritaService, seansId) : EMPTY)),
      scan((onceki: DolulukDurumu, cevap: DolulukCevabi | null): DolulukDurumu =>
        cevap ? { dolular: dolulukHaritasi(cevap), hata: false } : { ...onceki, hata: true }, BASLANGIC_DOLULUGU),
      startWith(BASLANGIC_DOLULUGU),
    );
    this.gorunum$ = combineLatest([duzen$, doluluk$, this.secim$, oturumService.kullanici$, this.tutma$]).pipe(
      map(([duzen, doluluk, secim, kullanici, tutma]) => {
        const harita = duzen.veri ? haritaKur(duzen.veri, doluluk.dolular, secim) : null;
        const secilenler = harita ? harita.hucreler.filter((h) => h.durum === 'SECILI') : [];
        return {
          duzen, harita, kullanici, tutma, secilenler,
          dolulukHatasi: doluluk.hata,
          secilenEtiketleri: secilenler.map((h) => h.etiket).join(', '),
          toplamTutar: this.toplamTutar(secilenler.length),
        };
      }),
    );

    // Çift tıklama ikinci tutma isteği başlatmaz (exhaustMap)
    this.devam$.pipe(
      exhaustMap((koltuklar) => this.tut(koltuklar)),
      takeUntil(this.yikim$),
    ).subscribe((rezervasyon) => this.router.navigate(['/rezervasyonlarim', rezervasyon.reservationId]));
  }

  koltukTiklandi(koltuk: HaritaHucresi, kullanici: Kullanici | null): void {
    if (!kullanici) {
      this.router.navigate(['/giris'], { queryParams: { donus: this.donusAdresi } });
      return;
    }
    if (koltuk.durum !== 'BOS' && koltuk.durum !== 'SECILI') {
      return;
    }
    const secim = new Set(this.secim$.value);
    if (secim.has(koltuk.hucre.cellId)) {
      secim.delete(koltuk.hucre.cellId);
    } else if (secim.size >= EN_FAZLA_KOLTUK) {
      this.bildirimService.bilgi(`Bir işlemde en çok ${EN_FAZLA_KOLTUK} koltuk seçebilirsiniz.`);
      return;
    } else {
      secim.add(koltuk.hucre.cellId);
    }
    this.secim$.next(secim);
    this.tutma$.next({ gonderiliyor: false, hata: null });
  }

  devam(secilenler: readonly HaritaHucresi[]): void {
    if (secilenler.length) {
      this.devam$.next(secilenler.map((h) => h.hucre.cellId));
    }
  }

  duzeniTekrarDene(): void {
    this.duzenTekrar$.next();
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private tut(koltuklar: readonly number[]): Observable<Rezervasyon> {
    const seansId = this.seansBilgisi?.id;
    if (seansId === undefined) {
      return EMPTY;
    }
    this.tutma$.next({ gonderiliyor: true, hata: null });
    return this.rezervasyonService.tut({ showtimeId: seansId, seatCellIds: koltuklar }).pipe(
      catchError((hata: unknown) => {
        this.tutma$.next({ gonderiliyor: false, hata: this.tutmaHatasi(hata) });
        return EMPTY;
      }),
      finalize(() => this.tutma$.next({ ...this.tutma$.value, gonderiliyor: false })),
    );
  }

  /** 409'da dolan koltuklar seçimden çıkar ve harita beklemeden yenilenir. */
  private tutmaHatasi(hata: unknown): TutmaHatasi {
    if (!(hata instanceof UygulamaHatasi)) {
      return { mesaj: BEKLENMEYEN_HATA, bekleyenRezervasyonId: null };
    }
    if (hata.kod === 'SEATS_UNAVAILABLE') {
      const dolular = hata.ekAlanlar['unavailableCellIds'];
      const dolanlar = new Set(Array.isArray(dolular) ? dolular.map(Number) : []);
      this.secim$.next(new Set([...this.secim$.value].filter((id) => !dolanlar.has(id))));
      this.hemenYenile$.next();
    }
    const bekleyen = hata.kod === 'ACTIVE_RESERVATION_EXISTS' ? Number(hata.ekAlanlar['reservationId']) : NaN;
    return { mesaj: hata.mesaj, bekleyenRezervasyonId: Number.isInteger(bekleyen) ? bekleyen : null };
  }

  /** Gösterim içindir; tutulacak tutarı sunucu hesaplar. Kuruş üzerinden (kayan nokta hatası olmasın). */
  private toplamTutar(adet: number): number {
    const birimKurus = Math.round(Number(this.seansBilgisi?.ticketPrice ?? 0) * KURUS);
    return (birimKurus * adet) / KURUS;
  }

  /**
   * Hemen, sonra her aralıkta ve istendiğinde (409) bir doluluk; önceki istek sürerken yenisi başlamaz
   * (`exhaustMap`). Hata akışı bitirmez: null olarak bildirilir.
   */
  private yokla(haritaService: KoltukHaritasiService, seansId: number): Observable<DolulukCevabi | null> {
    return merge(timer(0, environment.dolulukYenilemeMs), this.hemenYenile$).pipe(
      exhaustMap(() => haritaService.doluluk(seansId).pipe(catchError(() => of(null)))),
    );
  }
}
