import { Hucre, SeansDuzeni } from '../../../shared/components/koltuk-haritasi/koltuk-haritasi.models';
import { HucreTipi, KoltukTipi } from '../../../shared/models/duzen.models';

/*
 * Salon düzeni editörünün saf modeli (Angular'dan bağımsız, birim testle sınanır). Düzen değişmezdir: her işlem
 * yeni bir düzen döner; değişiklik yoksa aynı nesne döner (sürükleyerek boyamada gereksiz çizim olmaz).
 * Boş hücre saklanmaz (koridor/boşluk = hücre yok, sunucuyla aynı). Koltuk etiketleri saklanmaz, her seferinde
 * düzenden hesaplanır (`koltukEtiketleri`): elle düzeltme yok, etiket hiçbir zaman bayatlamaz.
 */

/** Sunucu sınırı (DuzenIstegi.EN_COK_SATIR / EN_COK_SUTUN). */
export const IZGARA_EN_FAZLA = 50;
export const IZGARA_EN_AZ = 1;

export type Firca = 'KOLTUK' | 'ENGELLI_KOLTUK' | 'PERDE' | 'GIRIS' | 'CIKIS' | 'DUVAR' | 'SIL';

export interface EditorHucresi {
  readonly satir: number;
  readonly sutun: number;
  readonly tip: HucreTipi;
  /** Yalnızca koltukta dolu. */
  readonly koltukTipi: KoltukTipi | null;
}

export interface EditorDuzeni {
  readonly satirSayisi: number;
  readonly sutunSayisi: number;
  /** Anahtar `konumAnahtari(satir, sutun)`. */
  readonly hucreler: ReadonlyMap<string, EditorHucresi>;
}

export interface KoltukEtiketi {
  readonly sira: string;
  readonly numara: number;
}

/** `POST/PUT /admin/catalog/halls/{id}/layouts` gövdesi (catalog DuzenIstegi). */
export interface DuzenIstegi {
  readonly gridRows: number;
  readonly gridCols: number;
  readonly cells: readonly {
    readonly row: number;
    readonly col: number;
    readonly type: HucreTipi;
    readonly seatRowLabel: string | null;
    readonly seatNumber: number | null;
    readonly seatType: KoltukTipi | null;
  }[];
}

const FIRCA_HUCRELERI: Readonly<Record<Exclude<Firca, 'SIL'>, Pick<EditorHucresi, 'tip' | 'koltukTipi'>>> = {
  KOLTUK: { tip: 'SEAT', koltukTipi: 'STANDARD' },
  ENGELLI_KOLTUK: { tip: 'SEAT', koltukTipi: 'ACCESSIBLE' },
  PERDE: { tip: 'SCREEN', koltukTipi: null },
  GIRIS: { tip: 'ENTRANCE', koltukTipi: null },
  CIKIS: { tip: 'EXIT', koltukTipi: null },
  DUVAR: { tip: 'WALL', koltukTipi: null },
};

const ALFABE_UZUNLUGU = 26;
const ILK_HARF_KODU = 'A'.charCodeAt(0);

export function konumAnahtari(satir: number, sutun: number): string {
  return `${satir},${sutun}`;
}

export function bosDuzen(satirSayisi: number, sutunSayisi: number): EditorDuzeni {
  return { satirSayisi: sinirla(satirSayisi), sutunSayisi: sinirla(sutunSayisi), hucreler: new Map() };
}

/** Hücreyi fırçayla boyar (SIL hücreyi kaldırır). Izgara dışı ya da zaten aynıysa aynı düzen döner. */
export function boya(duzen: EditorDuzeni, satir: number, sutun: number, firca: Firca): EditorDuzeni {
  if (!izgaradaMi(duzen, satir, sutun)) {
    return duzen;
  }
  const anahtar = konumAnahtari(satir, sutun);
  const mevcut = duzen.hucreler.get(anahtar);
  if (firca === 'SIL') {
    return mevcut ? hucrelerleYeni(duzen, (hucreler) => hucreler.delete(anahtar)) : duzen;
  }
  const hedef = FIRCA_HUCRELERI[firca];
  if (mevcut && mevcut.tip === hedef.tip && mevcut.koltukTipi === hedef.koltukTipi) {
    return duzen;
  }
  return hucrelerleYeni(duzen, (hucreler) => hucreler.set(anahtar, { satir, sutun, ...hedef }));
}

/** Izgarayı yeniden boyutlandırır; dışarıda kalan hücreler silinir (sınır 1–50). */
export function boyutDegistir(duzen: EditorDuzeni, satirSayisi: number, sutunSayisi: number): EditorDuzeni {
  const yeniSatir = sinirla(satirSayisi);
  const yeniSutun = sinirla(sutunSayisi);
  if (yeniSatir === duzen.satirSayisi && yeniSutun === duzen.sutunSayisi) {
    return duzen;
  }
  const hucreler = new Map(Array.from(duzen.hucreler).filter(([, h]) => h.satir < yeniSatir && h.sutun < yeniSutun));
  return { satirSayisi: yeniSatir, sutunSayisi: yeniSutun, hucreler };
}

/** Boyut küçültülürse kaybolacak hücre sayısı (kullanıcı uygulamadan önce görür; geri alma yok, K41). */
export function silinecekHucreSayisi(duzen: EditorDuzeni, satirSayisi: number, sutunSayisi: number): number {
  const yeniSatir = sinirla(satirSayisi);
  const yeniSutun = sinirla(sutunSayisi);
  return Array.from(duzen.hucreler.values()).filter((h) => h.satir >= yeniSatir || h.sutun >= yeniSutun).length;
}

/**
 * Koltuk etiketleri: sıra harfi perdeye en yakın koltuk satırından başlar (A, B, … Z, AA, AB…; koltuksuz
 * satırlar atlanır), numara her satırda soldan sağa 1'den başlar (koridor numarayı atlatmaz). Perde yoksa
 * üstte sayılır.
 */
export function koltukEtiketleri(duzen: EditorDuzeni): ReadonlyMap<string, KoltukEtiketi> {
  const koltuklar = Array.from(duzen.hucreler.values()).filter((h) => h.tip === 'SEAT');
  const koltukSatirlari = Array.from(new Set(koltuklar.map((h) => h.satir))).sort((a, b) => a - b);
  if (!perdeUstteMi(duzen, koltukSatirlari)) {
    koltukSatirlari.reverse();
  }
  const etiketler = new Map<string, KoltukEtiketi>();
  koltukSatirlari.forEach((satir, sira) => {
    koltuklar.filter((h) => h.satir === satir).sort((a, b) => a.sutun - b.sutun)
      .forEach((h, i) => etiketler.set(konumAnahtari(h.satir, h.sutun), { sira: siraHarfi(sira), numara: i + 1 }));
  });
  return etiketler;
}

/** İstemci tarafı kurallar (asıl doğrulama sunucuda, DuzenDogrulayici). Boş liste = kaydedilebilir. */
export function dogrula(duzen: EditorDuzeni): string[] {
  const hucreler = Array.from(duzen.hucreler.values());
  const sorunlar: string[] = [];
  if (!hucreler.some((h) => h.tip === 'SEAT')) {
    sorunlar.push('En az bir koltuk ekleyin.');
  }
  if (!hucreler.some((h) => h.tip === 'SCREEN')) {
    sorunlar.push('Perdeyi çizin (müşteri haritası ve koltuk harfleri perdeye göre yönlenir).');
  }
  return sorunlar;
}

export function koltukSayilari(duzen: EditorDuzeni): { readonly toplam: number; readonly engelli: number } {
  const koltuklar = Array.from(duzen.hucreler.values()).filter((h) => h.tip === 'SEAT');
  return { toplam: koltuklar.length, engelli: koltuklar.filter((h) => h.koltukTipi === 'ACCESSIBLE').length };
}

/** Sunucuya gidecek istek: hücreler satır/sütun sırasıyla, koltuklar etiketleriyle. */
export function istegeCevir(duzen: EditorDuzeni): DuzenIstegi {
  const etiketler = koltukEtiketleri(duzen);
  const cells = Array.from(duzen.hucreler.values())
    .sort((a, b) => a.satir - b.satir || a.sutun - b.sutun)
    .map((h) => {
      const etiket = etiketler.get(konumAnahtari(h.satir, h.sutun));
      return {
        row: h.satir,
        col: h.sutun,
        type: h.tip,
        seatRowLabel: etiket?.sira ?? null,
        seatNumber: etiket?.numara ?? null,
        seatType: h.tip === 'SEAT' ? h.koltukTipi : null,
      };
    });
  return { gridRows: duzen.satirSayisi, gridCols: duzen.sutunSayisi, cells };
}

/** Sunucudaki düzeni (taslak ya da kopyalanacak yayımlı sürüm) editör modeline çevirir; etiketler yeniden hesaplanır. */
export function cevaptanKur(satirSayisi: number, sutunSayisi: number, hucreler: readonly Hucre[]): EditorDuzeni {
  const harita = new Map<string, EditorHucresi>(hucreler.map((h) => [
    konumAnahtari(h.row, h.col),
    { satir: h.row, sutun: h.col, tip: h.type, koltukTipi: h.type === 'SEAT' ? h.seatType ?? 'STANDARD' : null },
  ]));
  return { satirSayisi: sinirla(satirSayisi), sutunSayisi: sinirla(sutunSayisi), hucreler: harita };
}

/**
 * Müşteri koltuk haritasıyla önizleme için: hücrelere yalnızca görüntüde kullanılan geçici kimlikler verilir
 * (gerçek kimlikleri sunucu kayıtta üretir).
 */
export function onizlemeDuzeni(duzen: EditorDuzeni): SeansDuzeni {
  const istek = istegeCevir(duzen);
  return {
    showtimeId: 0,
    layoutId: 0,
    gridRows: istek.gridRows,
    gridCols: istek.gridCols,
    cells: istek.cells.map((hucre, sira) => ({ ...hucre, cellId: sira + 1 })),
  };
}

/** 0 → A, 25 → Z, 26 → AA (sunucu deseni [A-Z]{1,3}; 50 satırda en çok iki harf). */
export function siraHarfi(sira: number): string {
  const harf = String.fromCharCode(ILK_HARF_KODU + (sira % ALFABE_UZUNLUGU));
  const onceki = Math.floor(sira / ALFABE_UZUNLUGU);
  return onceki === 0 ? harf : siraHarfi(onceki - 1) + harf;
}

function perdeUstteMi(duzen: EditorDuzeni, koltukSatirlari: readonly number[]): boolean {
  const perdeSatirlari = Array.from(duzen.hucreler.values()).filter((h) => h.tip === 'SCREEN').map((h) => h.satir);
  if (perdeSatirlari.length === 0 || koltukSatirlari.length === 0) {
    return true;
  }
  return ortalama(perdeSatirlari) <= ortalama(koltukSatirlari);
}

function ortalama(sayilar: readonly number[]): number {
  return sayilar.reduce((toplam, sayi) => toplam + sayi, 0) / sayilar.length;
}

function izgaradaMi(duzen: EditorDuzeni, satir: number, sutun: number): boolean {
  return satir >= 0 && sutun >= 0 && satir < duzen.satirSayisi && sutun < duzen.sutunSayisi;
}

function sinirla(sayi: number): number {
  return Math.min(IZGARA_EN_FAZLA, Math.max(IZGARA_EN_AZ, Math.trunc(sayi) || IZGARA_EN_AZ));
}

function hucrelerleYeni(duzen: EditorDuzeni, degistir: (hucreler: Map<string, EditorHucresi>) => void): EditorDuzeni {
  const hucreler = new Map(duzen.hucreler);
  degistir(hucreler);
  return { ...duzen, hucreler };
}
