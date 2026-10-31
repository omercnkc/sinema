/** Düzen hücre tipleri (catalog HucreTipi). Boşluk saklanmaz: hücre yoksa koridor/boş alandır. */
export type HucreTipi = 'SEAT' | 'ENTRANCE' | 'EXIT' | 'SCREEN' | 'WALL';
/** K28: fiyat farkı yok, yalnızca görsel/erişilebilirlik. */
export type KoltukTipi = 'STANDARD' | 'ACCESSIBLE';

/** `GET /catalog/showtimes/{id}/layout` hücresi. `cellId` koltuğun kalıcı kimliği (tutarken booking'e gider). */
export interface Hucre {
  readonly cellId: number;
  readonly row: number;
  readonly col: number;
  readonly type: HucreTipi;
  readonly seatRowLabel: string | null;
  readonly seatNumber: number | null;
  readonly seatType: KoltukTipi | null;
}

/** Seansın mühürlü düzeni (değişmez sürüm; bir kez alınır). */
export interface SeansDuzeni {
  readonly showtimeId: number;
  readonly layoutId: number;
  readonly gridRows: number;
  readonly gridCols: number;
  readonly cells: readonly Hucre[];
}

/** `GET /bookings/showtimes/{id}/seat-status`: yalnızca dolu koltuklar; listede olmayan boştur. */
export interface DolulukCevabi {
  readonly serverTime: string;
  readonly occupied: readonly { readonly cellId: number; readonly status: 'HELD' | 'SOLD' }[];
}

export type KoltukDurumu = 'BOS' | 'TUTULU' | 'SATILDI' | 'SECILI';

/** Haritada çizilen hücre; koltuksa durumu ve ekran okuyucu etiketi hazır gelir (şablonda hesap yok). */
export interface HaritaHucresi {
  readonly hucre: Hucre;
  readonly durum: KoltukDurumu | null;
  /** "F-7": sunucunun koltuk etiketiyle aynı biçim (rezervasyon ve bilet) */
  readonly etiket: string | null;
  /** "F sırası 7 numara, boş, engelli koltuğu" */
  readonly erisilebilirEtiket: string;
}

/** Satır başındaki sıra harfi (düzende o satırdaki ilk koltuğun etiketi). */
export interface SiraEtiketi {
  readonly satir: number;
  readonly etiket: string;
}

export interface KoltukHaritasiGorunumu {
  readonly satirSayisi: number;
  readonly sutunSayisi: number;
  readonly hucreler: readonly HaritaHucresi[];
  readonly siraEtiketleri: readonly SiraEtiketi[];
}

const DURUM_ADLARI: Readonly<Record<KoltukDurumu, string>> = {
  BOS: 'boş',
  TUTULU: 'başkası tarafından tutuluyor',
  SATILDI: 'satıldı',
  SECILI: 'seçildi',
};

const HUCRE_ADLARI: Readonly<Record<Exclude<HucreTipi, 'SEAT'>, string>> = {
  SCREEN: 'Perde',
  ENTRANCE: 'Giriş',
  EXIT: 'Çıkış',
  WALL: 'Duvar',
};

/** Doluluk listesini koltuk kimliği → durum eşlemesine çevirir. */
export function dolulukHaritasi(doluluk: DolulukCevabi | null): ReadonlyMap<number, KoltukDurumu> {
  return new Map((doluluk?.occupied ?? []).map((koltuk) => [koltuk.cellId, koltuk.status === 'SOLD' ? 'SATILDI' : 'TUTULU']));
}

/**
 * Düzen + doluluk + seçim → çizilecek görünüm (saf fonksiyon). Seçili koltuk başkası tarafından alınmışsa dolu
 * görünür (doluluk seçimden önce gelir).
 */
export function haritaKur(
  duzen: SeansDuzeni,
  dolular: ReadonlyMap<number, KoltukDurumu>,
  secili: ReadonlySet<number>,
): KoltukHaritasiGorunumu {
  const hucreler = duzen.cells.map((hucre) => haritaHucresi(hucre, dolular, secili));
  return {
    satirSayisi: duzen.gridRows,
    sutunSayisi: duzen.gridCols,
    hucreler,
    siraEtiketleri: siraEtiketleri(duzen.cells),
  };
}

function haritaHucresi(hucre: Hucre, dolular: ReadonlyMap<number, KoltukDurumu>, secili: ReadonlySet<number>): HaritaHucresi {
  if (hucre.type !== 'SEAT') {
    return { hucre, durum: null, etiket: null, erisilebilirEtiket: HUCRE_ADLARI[hucre.type] };
  }
  const durum = dolular.get(hucre.cellId) ?? (secili.has(hucre.cellId) ? 'SECILI' : 'BOS');
  const engelli = hucre.seatType === 'ACCESSIBLE' ? ', engelli koltuğu' : '';
  return {
    hucre,
    durum,
    etiket: `${hucre.seatRowLabel}-${hucre.seatNumber}`,
    erisilebilirEtiket: `${hucre.seatRowLabel} sırası ${hucre.seatNumber} numara, ${DURUM_ADLARI[durum]}${engelli}`,
  };
}

function siraEtiketleri(hucreler: readonly Hucre[]): SiraEtiketi[] {
  const satirlar = new Map<number, Hucre>();
  for (const hucre of hucreler) {
    const mevcut = satirlar.get(hucre.row);
    if (hucre.type === 'SEAT' && hucre.seatRowLabel && (!mevcut || hucre.col < mevcut.col)) {
      satirlar.set(hucre.row, hucre);
    }
  }
  return Array.from(satirlar.values())
    .sort((a, b) => a.row - b.row)
    .map((hucre) => ({ satir: hucre.row, etiket: hucre.seatRowLabel as string }));
}
