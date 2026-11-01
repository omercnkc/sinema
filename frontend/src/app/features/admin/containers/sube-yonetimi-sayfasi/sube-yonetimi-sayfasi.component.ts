import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { BehaviorSubject, EMPTY, merge, Observable, Subject } from 'rxjs';
import { catchError, exhaustMap, finalize, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { sessizYukleme, YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { SalonIstegi, SubeIstegi, YonetimSalon, YonetimSube } from '../../models/sube-yonetimi.models';
import { SubeYonetimiService } from '../../services/sube-yonetimi.service';

/**
 * Açık düzenleyici (aynı anda tek). `anahtar` şablonda hangi kartın/satırın form gösterdiğini seçer
 * (ör. "sube-3", "yeni-salon-3", "salon-12").
 */
export type Duzenleme =
  | { readonly tur: 'yeni-sube'; readonly anahtar: string }
  | { readonly tur: 'sube'; readonly anahtar: string; readonly sube: YonetimSube }
  | { readonly tur: 'yeni-salon'; readonly anahtar: string; readonly subeId: number }
  | { readonly tur: 'salon'; readonly anahtar: string; readonly salon: YonetimSalon };

interface Kayit {
  readonly istek$: Observable<unknown>;
  readonly basariMesaji: string;
}

/**
 * /admin/subeler (smart): şubeler ve salonları, ekleme ve düzenleme. Formlar sayfa içinde açılır (modal yok).
 * Silme yok: seanslar ve biletler şube/salona bağlı. Salonun düzeni (yayımlı mı) burada görünür; düzen editörü
 * M7 alt görev 5-6'da bağlanır.
 */
@Component({
  selector: 'app-sube-yonetimi-sayfasi',
  templateUrl: './sube-yonetimi-sayfasi.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubeYonetimiSayfasiComponent implements OnDestroy {
  readonly durum$: Observable<YuklemeDurumu<YonetimSube[]>>;
  duzenleme: Duzenleme | null = null;
  gonderiliyor = false;
  /** Son kaydın hatası; açık forma iletilir (alan hataları alanın altında). */
  kayitHatasi: unknown = null;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);
  private readonly sessizYenile$ = new Subject<void>();
  private readonly kayit$ = new Subject<Kayit>();
  private readonly yikim$ = new Subject<void>();

  constructor(
    private readonly subeService: SubeYonetimiService,
    private readonly bildirimService: BildirimService,
    private readonly degisiklikAlgilayici: ChangeDetectorRef,
  ) {
    this.durum$ = this.tekrarDene$.pipe(
      switchMap(() => merge(
        yuklemeDurumu(subeService.listele()),
        this.sessizYenile$.pipe(switchMap(() => sessizYukleme(subeService.listele()))),
      )),
    );

    this.kayit$.pipe(
      tap(() => this.gonderimBasladi()),
      exhaustMap((kayit) => kayit.istek$.pipe(
        tap(() => this.kaydedildi(kayit.basariMesaji)),
        catchError((hata: unknown) => {
          this.kayitHatasi = hata;
          return EMPTY;
        }),
        finalize(() => this.gonderimBitti()),
      )),
      takeUntil(this.yikim$),
    ).subscribe();
  }

  yeniSube(): void {
    this.ac({ tur: 'yeni-sube', anahtar: 'yeni-sube' });
  }

  subeDuzenle(sube: YonetimSube): void {
    this.ac({ tur: 'sube', anahtar: `sube-${sube.id}`, sube });
  }

  yeniSalon(sube: YonetimSube): void {
    this.ac({ tur: 'yeni-salon', anahtar: `yeni-salon-${sube.id}`, subeId: sube.id });
  }

  salonDuzenle(salon: YonetimSalon): void {
    this.ac({ tur: 'salon', anahtar: `salon-${salon.id}`, salon });
  }

  vazgec(): void {
    this.duzenleme = null;
    this.kayitHatasi = null;
  }

  subeKaydet(istek: SubeIstegi): void {
    const duzenleme = this.duzenleme;
    if (duzenleme?.tur === 'sube') {
      this.kayit$.next({ istek$: this.subeService.subeGuncelle(duzenleme.sube.id, istek), basariMesaji: `${istek.name} güncellendi.` });
    } else if (duzenleme?.tur === 'yeni-sube') {
      this.kayit$.next({ istek$: this.subeService.subeOlustur(istek), basariMesaji: `${istek.name} şubesi eklendi.` });
    }
  }

  salonKaydet(istek: SalonIstegi): void {
    const duzenleme = this.duzenleme;
    if (duzenleme?.tur === 'salon') {
      this.kayit$.next({ istek$: this.subeService.salonGuncelle(duzenleme.salon.id, istek), basariMesaji: `${istek.name} güncellendi.` });
    } else if (duzenleme?.tur === 'yeni-salon') {
      this.kayit$.next({ istek$: this.subeService.salonOlustur(duzenleme.subeId, istek), basariMesaji: `${istek.name} eklendi.` });
    }
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  subeIzle(_: number, sube: YonetimSube): number {
    return sube.id;
  }

  salonIzle(_: number, salon: YonetimSalon): number {
    return salon.id;
  }

  ngOnDestroy(): void {
    this.yikim$.next();
    this.yikim$.complete();
  }

  /** Başka bir düzenleyici açılırsa öncekinin kaydedilmemiş değişiklikleri bırakılır (kayıt sürerken açılamaz). */
  private ac(duzenleme: Duzenleme): void {
    if (this.gonderiliyor) {
      return;
    }
    this.duzenleme = duzenleme;
    this.kayitHatasi = null;
  }

  private gonderimBasladi(): void {
    this.gonderiliyor = true;
    this.kayitHatasi = null;
  }

  private kaydedildi(basariMesaji: string): void {
    this.duzenleme = null;
    this.bildirimService.basari(basariMesaji);
    this.sessizYenile$.next();
  }

  private gonderimBitti(): void {
    this.gonderiliyor = false;
    this.degisiklikAlgilayici.markForCheck();
  }
}
