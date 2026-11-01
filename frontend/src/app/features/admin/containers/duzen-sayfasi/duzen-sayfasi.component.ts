import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, EMPTY, forkJoin, Observable, of, Subject, throwError } from 'rxjs';
import { catchError, exhaustMap, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { IS_SAAT_DILIMI } from '../../../../core/zaman/is-saat-dilimi';
import { haritaKur, KoltukHaritasiGorunumu } from '../../../../shared/components/koltuk-haritasi/koltuk-haritasi.models';
import { bosDuzen, cevaptanKur, dogrula, EditorDuzeni, istegeCevir, onizlemeDuzeni } from '../../models/duzen-editoru.models';
import { DuzenCevabi, DuzenOzeti } from '../../models/duzen-yonetimi.models';
import { YonetimSalon } from '../../models/sube-yonetimi.models';
import { DuzenYonetimiService } from '../../services/duzen-yonetimi.service';
import { KaydedilmemisDegisiklikli } from '../../services/kaydedilmemis-degisiklik.guard';
import { SubeYonetimiService } from '../../services/sube-yonetimi.service';

/** Düzeni olmayan salonda başlangıç ızgarası (admin boyutu editörde değiştirir). */
const VARSAYILAN_SATIR = 10;
const VARSAYILAN_SUTUN = 14;
const SALON_YOK = new UygulamaHatasi(404, 'HALL_NOT_FOUND', 'Salon bulunamadı.');
const BEKLENMEYEN_HATA = 'Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.';
const GECERSIZ_DUZEN_KODU = 'LAYOUT_INVALID';
const IHLALLER_ALANI = 'violations';

/** Editöre ne açıldı: kayıtlı taslak, yayımlı düzenin kopyası (henüz kaydedilmemiş yeni taslak) ya da boş ızgara. */
export type DuzenKaynagi = 'TASLAK' | 'KOPYA' | 'BOS';

interface SayfaVerisi {
  readonly salon: YonetimSalon;
  readonly subeAdi: string;
  readonly surumler: readonly DuzenOzeti[];
  readonly taslakId: number | null;
  readonly kaynak: DuzenKaynagi;
  readonly kopyaSurumu: number | null;
  readonly duzen: EditorDuzeni;
}

type Islem = 'KAYDET' | 'YAYIMLA';

/**
 * /admin/subeler/salonlar/:salonId/duzen (smart). Salon başına tek taslak vardır (sunucu kuralı): taslak varsa
 * o açılır, yoksa yayımlı düzenin kopyasıyla (ya da boş ızgarayla) başlanır ve ilk kayıtta taslak oluşur.
 * Yayımlanan sürüm yeni seanslara uygulanır, mevcut seanslar eski sürümde kalır (K17); salonda süren seans
 * varken yayımlanamaz (K13). Kaydedilmemiş çizim, sayfadan ayrılırken onay ister.
 */
@Component({
  selector: 'app-duzen-sayfasi',
  templateUrl: './duzen-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DuzenSayfasiComponent implements OnDestroy, KaydedilmemisDegisiklikli {
  readonly saatDilimi = IS_SAAT_DILIMI;
  readonly salonId: number;

  yukleniyor = true;
  yuklemeHatasi: UygulamaHatasi | null = null;
  veri: SayfaVerisi | null = null;
  duzen: EditorDuzeni | null = null;
  taslakId: number | null = null;
  kaydedilmemisDegisiklikVar = false;
  islemSuruyor = false;
  islemHatasi: string | null = null;
  ihlaller: readonly string[] = [];
  sorunVar = false;
  yayimlamaOnayiAcik = false;
  onizleme: KoltukHaritasiGorunumu | null = null;

  private readonly yukle$ = new BehaviorSubject<void>(undefined);
  private readonly islem$ = new Subject<Islem>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    rota: ActivatedRoute,
    private readonly duzenService: DuzenYonetimiService,
    private readonly subeService: SubeYonetimiService,
    private readonly bildirimService: BildirimService,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    this.salonId = Number(rota.snapshot.paramMap.get('salonId'));

    this.yukle$.pipe(
      tap(() => this.yuklemeBasladi()),
      switchMap(() => this.sayfaVerisi().pipe(
        tap((veri) => this.yuklendi(veri)),
        catchError((hata: unknown) => {
          this.yuklemeHatasi = hata instanceof UygulamaHatasi ? hata : new UygulamaHatasi(0, 'UNEXPECTED', BEKLENMEYEN_HATA);
          return EMPTY;
        }),
        finalize(() => this.yuklemeBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();

    this.islem$.pipe(
      tap(() => this.islemBasladi()),
      exhaustMap((islem) => (islem === 'KAYDET' ? this.kaydet() : this.yayimla()).pipe(
        catchError((hata: unknown) => {
          this.islemHatasiniGoster(hata);
          return EMPTY;
        }),
        finalize(() => this.islemBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  /** Kaydet: değişiklik varken ve istemci kuralları sağlanınca. Taslak kayıtlı ve değişiklik yoksa gereksizdir. */
  get kaydedilebilir(): boolean {
    return !this.islemSuruyor && !this.sorunVar && (this.kaydedilmemisDegisiklikVar || this.taslakId === null);
  }

  /** Yayımla: yalnızca sunucuya kaydedilmiş, değişikliği olmayan taslak. */
  get yayimlanabilir(): boolean {
    return !this.islemSuruyor && this.taslakId !== null && !this.kaydedilmemisDegisiklikVar;
  }

  duzenDegisti(duzen: EditorDuzeni): void {
    this.duzen = duzen;
    this.kaydedilmemisDegisiklikVar = true;
    this.yayimlamaOnayiAcik = false;
    this.duzenDurumunuGuncelle();
  }

  kaydetIstendi(): void {
    if (this.kaydedilebilir) {
      this.islem$.next('KAYDET');
    }
  }

  yayimlamaOnayiniAc(): void {
    this.yayimlamaOnayiAcik = this.yayimlanabilir;
  }

  yayimlamaOnayiniKapat(): void {
    this.yayimlamaOnayiAcik = false;
  }

  yayimlaIstendi(): void {
    if (this.yayimlanabilir) {
      this.islem$.next('YAYIMLA');
    }
  }

  onizlemeyiDegistir(): void {
    this.onizleme = this.onizleme === null && this.duzen ? onizlemeGorunumu(this.duzen) : null;
  }

  tekrarDene(): void {
    this.yukle$.next();
  }

  surumIzle(_: number, surum: DuzenOzeti): number {
    return surum.id;
  }

  ihlalIzle(_: number, ihlal: string): string {
    return ihlal;
  }

  /** Sekme kapatma/yenileme: tarayıcının kendi onay penceresi (metni tarayıcı belirler). */
  @HostListener('window:beforeunload', ['$event'])
  sayfadanAyriliyor(olay: BeforeUnloadEvent): void {
    if (this.kaydedilmemisDegisiklikVar) {
      olay.preventDefault();
      olay.returnValue = '';
    }
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  private sayfaVerisi(): Observable<SayfaVerisi> {
    if (!Number.isInteger(this.salonId) || this.salonId < 1) {
      return throwError(() => SALON_YOK);
    }
    return forkJoin({ subeler: this.subeService.listele(), surumler: this.duzenService.listele(this.salonId) }).pipe(
      switchMap(({ subeler, surumler }) => {
        const sube = subeler.find((s) => s.halls.some((h) => h.id === this.salonId));
        const salon = sube?.halls.find((h) => h.id === this.salonId);
        if (!sube || !salon) {
          return throwError(() => SALON_YOK);
        }
        return this.baslangicDuzeni(surumler).pipe(map((baslangic) => ({ salon, subeAdi: sube.name, surumler, ...baslangic })));
      }),
    );
  }

  private baslangicDuzeni(surumler: readonly DuzenOzeti[]): Observable<Pick<SayfaVerisi, 'taslakId' | 'kaynak' | 'kopyaSurumu' | 'duzen'>> {
    const taslak = surumler.find((s) => s.status === 'DRAFT');
    const aktif = surumler.find((s) => s.active);
    if (taslak) {
      return this.duzenService.getir(this.salonId, taslak.id).pipe(
        map((cevap) => ({ taslakId: taslak.id, kaynak: 'TASLAK' as const, kopyaSurumu: null, duzen: editorDuzeni(cevap) })));
    }
    if (aktif) {
      return this.duzenService.getir(this.salonId, aktif.id).pipe(
        map((cevap) => ({ taslakId: null, kaynak: 'KOPYA' as const, kopyaSurumu: aktif.version, duzen: editorDuzeni(cevap) })));
    }
    return of({ taslakId: null, kaynak: 'BOS' as const, kopyaSurumu: null, duzen: bosDuzen(VARSAYILAN_SATIR, VARSAYILAN_SUTUN) });
  }

  private kaydet(): Observable<unknown> {
    const gonderilen = this.duzen as EditorDuzeni;
    const istek = istegeCevir(gonderilen);
    const kayit$ = this.taslakId === null
      ? this.duzenService.taslakOlustur(this.salonId, istek)
      : this.duzenService.taslakGuncelle(this.salonId, this.taslakId, istek);
    return kayit$.pipe(
      tap((cevap) => {
        this.taslakId = cevap.id;
        // Kayıt sürerken çizilen değişiklik hâlâ kaydedilmemiştir
        this.kaydedilmemisDegisiklikVar = this.duzen !== gonderilen;
        this.bildirimService.basari('Taslak kaydedildi. Müşteriler yayımlanana kadar mevcut düzeni görür.');
      }),
      switchMap(() => this.duzenService.listele(this.salonId)),
      tap((surumler) => {
        if (this.veri) {
          this.veri = { ...this.veri, surumler, kaynak: 'TASLAK' };
        }
      }),
    );
  }

  private yayimla(): Observable<unknown> {
    return this.duzenService.yayimla(this.salonId, this.taslakId as number).pipe(
      tap((cevap: DuzenCevabi) => {
        this.bildirimService.basari(`Sürüm ${cevap.version} yayımlandı. Bundan sonra eklenen seanslar bu düzeni kullanır.`);
        this.yayimlamaOnayiAcik = false;
        this.yukle$.next();
      }),
    );
  }

  private yuklemeBasladi(): void {
    this.yukleniyor = true;
    this.yuklemeHatasi = null;
  }

  private yuklendi(veri: SayfaVerisi): void {
    this.veri = veri;
    this.duzen = veri.duzen;
    this.taslakId = veri.taslakId;
    this.kaydedilmemisDegisiklikVar = false;
    this.islemHatasi = null;
    this.ihlaller = [];
    this.duzenDurumunuGuncelle();
  }

  private yuklemeBitti(): void {
    this.yukleniyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }

  private islemBasladi(): void {
    this.islemSuruyor = true;
    this.islemHatasi = null;
    this.ihlaller = [];
  }

  private islemBitti(): void {
    this.islemSuruyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }

  private islemHatasiniGoster(hata: unknown): void {
    if (!(hata instanceof UygulamaHatasi)) {
      this.islemHatasi = BEKLENMEYEN_HATA;
      return;
    }
    this.islemHatasi = hata.mesaj;
    const ihlaller = hata.ekAlanlar[IHLALLER_ALANI];
    this.ihlaller = hata.kod === GECERSIZ_DUZEN_KODU && Array.isArray(ihlaller) ? ihlaller.map(String) : [];
  }

  private duzenDurumunuGuncelle(): void {
    this.sorunVar = this.duzen ? dogrula(this.duzen).length > 0 : true;
    if (this.onizleme !== null && this.duzen) {
      this.onizleme = onizlemeGorunumu(this.duzen);
    }
  }
}

function editorDuzeni(cevap: DuzenCevabi): EditorDuzeni {
  return cevaptanKur(cevap.gridRows, cevap.gridCols, cevap.cells);
}

function onizlemeGorunumu(duzen: EditorDuzeni): KoltukHaritasiGorunumu {
  return haritaKur(onizlemeDuzeni(duzen), new Map(), new Set());
}
