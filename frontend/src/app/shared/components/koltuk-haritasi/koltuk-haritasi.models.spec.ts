import { dolulukHaritasi, haritaKur, Hucre, SeansDuzeni } from './koltuk-haritasi.models';

/** Testlerde kısa düzen: 0. satır perde, 1. satır A sırası (A1, boşluk, A2), 2. satır B1 engelli + giriş. */
export function ornekDuzen(): SeansDuzeni {
  const koltuk = (cellId: number, row: number, col: number, sira: string, no: number, engelli = false): Hucre => ({
    cellId, row, col, type: 'SEAT', seatRowLabel: sira, seatNumber: no, seatType: engelli ? 'ACCESSIBLE' : 'STANDARD',
  });
  const diger = (cellId: number, row: number, col: number, type: Hucre['type']): Hucre => ({
    cellId, row, col, type, seatRowLabel: null, seatNumber: null, seatType: null,
  });
  return {
    showtimeId: 101, layoutId: 1, gridRows: 3, gridCols: 4,
    cells: [
      diger(1, 0, 0, 'SCREEN'), diger(2, 0, 3, 'SCREEN'),
      koltuk(10, 1, 0, 'A', 1), koltuk(11, 1, 3, 'A', 2),
      koltuk(20, 2, 1, 'B', 1, true), diger(30, 2, 3, 'ENTRANCE'),
    ],
  };
}

describe('koltuk haritası modeli', () => {
  it('should map occupancy statuses', () => {
    const dolular = dolulukHaritasi({ serverTime: '', occupied: [{ cellId: 10, status: 'HELD' }, { cellId: 11, status: 'SOLD' }] });

    expect(dolular.get(10)).toBe('TUTULU');
    expect(dolular.get(11)).toBe('SATILDI');
    expect(dolulukHaritasi(null).size).toBe(0);
  });

  it('should build seats with state, server-style label and screen reader text', () => {
    const gorunum = haritaKur(ornekDuzen(), new Map([[10, 'SATILDI']]), new Set([11, 20]));
    const koltuk = (id: number) => gorunum.hucreler.find((h) => h.hucre.cellId === id);

    expect(koltuk(10)?.durum).toBe('SATILDI');
    expect(koltuk(11)?.durum).toBe('SECILI');
    expect(koltuk(11)?.etiket).toBe('A-2');
    expect(koltuk(20)?.erisilebilirEtiket).toBe('B sırası 1 numara, seçildi, engelli koltuğu');
    expect(koltuk(30)?.durum).toBeNull();
    expect(koltuk(30)?.erisilebilirEtiket).toBe('Giriş');
  });

  it('should show occupied even if the seat was selected before someone else took it', () => {
    const gorunum = haritaKur(ornekDuzen(), new Map([[11, 'TUTULU']]), new Set([11]));

    expect(gorunum.hucreler.find((h) => h.hucre.cellId === 11)?.durum).toBe('TUTULU');
  });

  it('should label each row with its first seat letter', () => {
    expect(haritaKur(ornekDuzen(), new Map(), new Set()).siraEtiketleri).toEqual([
      { satir: 1, etiket: 'A' }, { satir: 2, etiket: 'B' },
    ]);
  });
});
