import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output } from '@angular/core';
import { HaritaHucresi, Hucre, KoltukDurumu, KoltukHaritasiGorunumu } from '../../models/koltuk-haritasi.models';

/** Ok tuşu → [satır, sütun] yönü */
const YONLER: Readonly<Record<string, readonly [number, number]>> = {
  ArrowUp: [-1, 0],
  ArrowDown: [1, 0],
  ArrowLeft: [0, -1],
  ArrowRight: [0, 1],
};

/** İlk sütun sıra harfleri için ayrılır; düzen sütunları bir kaydırılır. */
const SIRA_SUTUNU = 1;

/** Perdenin bir satırdaki kapsamı (tek bant olarak çizilir). */
interface PerdeBandi {
  readonly satir: number;
  readonly ilkSutun: number;
  readonly sonSutun: number;
}

/**
 * Koltuk haritası (dumb): düzeni CSS grid ile çizer, koltuk tıklamasını dışarı bildirir. Durumlar renge ek olarak
 * desen/simgeyle ayrılır (renk körlüğü). Klavyede haritaya tek sekme durağıyla girilir, ok tuşlarıyla koltuklar
 * arasında gezilir (roving tabindex); Enter/Boşluk düğmenin kendi davranışıyla tıklar.
 */
@Component({
  selector: 'app-koltuk-haritasi',
  templateUrl: './koltuk-haritasi.component.html',
  styleUrls: ['./koltuk-haritasi.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KoltukHaritasiComponent {
  /** Tıklanan koltuk durumuyla birlikte; seçilip seçilemeyeceğine kapsayıcı karar verir. */
  @Output() readonly koltukTiklandi = new EventEmitter<HaritaHucresi>();

  readonly durumSiniflari: Readonly<Record<KoltukDurumu, string>> = {
    BOS: 'koltuk--bos',
    TUTULU: 'koltuk--tutulu',
    SATILDI: 'koltuk--satildi',
    SECILI: 'koltuk--secili',
  };

  koltuklar: readonly HaritaHucresi[] = [];
  digerHucreler: readonly HaritaHucresi[] = [];
  perdeBantlari: readonly PerdeBandi[] = [];
  gorunumVerisi: KoltukHaritasiGorunumu | null = null;
  izgaraSutunlari = '';
  izgaraSatirlari = '';
  odakKimligi: number | null = null;

  constructor(private readonly eleman: ElementRef<HTMLElement>) {}

  @Input() set gorunum(gorunum: KoltukHaritasiGorunumu) {
    this.gorunumVerisi = gorunum;
    this.koltuklar = gorunum.hucreler.filter((h) => h.hucre.type === 'SEAT');
    this.digerHucreler = gorunum.hucreler.filter((h) => h.hucre.type !== 'SEAT' && h.hucre.type !== 'SCREEN');
    this.perdeBantlari = KoltukHaritasiComponent.perdeBantlari(gorunum.hucreler);
    this.izgaraSutunlari = `repeat(${gorunum.sutunSayisi + SIRA_SUTUNU}, var(--hucre-boyu))`;
    this.izgaraSatirlari = `repeat(${gorunum.satirSayisi}, var(--hucre-boyu))`;
    if (!this.koltuklar.some((h) => h.hucre.cellId === this.odakKimligi)) {
      this.odakKimligi = this.koltuklar[0]?.hucre.cellId ?? null;
    }
  }

  tikla(koltuk: HaritaHucresi): void {
    this.odakKimligi = koltuk.hucre.cellId;
    this.koltukTiklandi.emit(koltuk);
  }

  tusaBasildi(olay: KeyboardEvent, koltuk: HaritaHucresi): void {
    const yon = YONLER[olay.key];
    if (!yon) {
      return;
    }
    olay.preventDefault();
    const hedef = this.komsuKoltuk(koltuk.hucre, yon);
    if (hedef) {
      this.odakKimligi = hedef.cellId;
      this.eleman.nativeElement.querySelector<HTMLElement>(`[data-hucre="${hedef.cellId}"]`)?.focus();
    }
  }

  hucreIzle(_: number, hucre: HaritaHucresi): number {
    return hucre.hucre.cellId;
  }

  /** Perde bandı ve sıra etiketi satır başına tektir. */
  satirIzle(_: number, oge: { readonly satir: number }): number {
    return oge.satir;
  }

  /** Aynı satırda yöndeki en yakın koltuk; dikeyde yöndeki en yakın satırın en yakın sütunu. */
  private komsuKoltuk(kaynak: Hucre, [satirYonu, sutunYonu]: readonly [number, number]): Hucre | null {
    const adaylar = this.koltuklar.map((h) => h.hucre).filter((h) =>
      satirYonu === 0
        ? h.row === kaynak.row && Math.sign(h.col - kaynak.col) === sutunYonu
        : Math.sign(h.row - kaynak.row) === satirYonu);
    const uzaklik = (h: Hucre): number =>
      satirYonu === 0 ? Math.abs(h.col - kaynak.col) : Math.abs(h.row - kaynak.row) * 1000 + Math.abs(h.col - kaynak.col);
    return adaylar.reduce<Hucre | null>((enYakin, h) => (!enYakin || uzaklik(h) < uzaklik(enYakin) ? h : enYakin), null);
  }

  private static perdeBantlari(hucreler: readonly HaritaHucresi[]): PerdeBandi[] {
    const bantlar = new Map<number, PerdeBandi>();
    for (const { hucre } of hucreler.filter((h) => h.hucre.type === 'SCREEN')) {
      const bant = bantlar.get(hucre.row);
      bantlar.set(hucre.row, {
        satir: hucre.row,
        ilkSutun: Math.min(bant?.ilkSutun ?? hucre.col, hucre.col),
        sonSutun: Math.max(bant?.sonSutun ?? hucre.col, hucre.col),
      });
    }
    return Array.from(bantlar.values());
  }
}
