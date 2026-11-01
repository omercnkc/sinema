import {
  bosDuzen, boya, cevaptanKur, onizlemeDuzeni, boyutDegistir, dogrula, EditorDuzeni, Firca, istegeCevir, koltukEtiketleri, koltukSayilari,
  konumAnahtari, silinecekHucreSayisi, siraHarfi,
} from './duzen-editoru.models';

describe('duzen editoru modeli', () => {
  /** [satır, sütun, fırça] listesini sırayla boyar. */
  function ciz(duzen: EditorDuzeni, adimlar: readonly [number, number, Firca][]): EditorDuzeni {
    return adimlar.reduce((sonuc, [satir, sutun, firca]) => boya(sonuc, satir, sutun, firca), duzen);
  }

  function etiket(duzen: EditorDuzeni, satir: number, sutun: number): string {
    const e = koltukEtiketleri(duzen).get(konumAnahtari(satir, sutun));
    return e ? `${e.sira}-${e.numara}` : '';
  }

  it('should paint and erase cells immutably and return same layout when nothing changes', () => {
    const bos = bosDuzen(3, 4);
    const koltuklu = boya(bos, 1, 2, 'KOLTUK');

    expect(bos.hucreler.size).toBe(0);
    expect(koltuklu.hucreler.get('1,2')).toEqual({ satir: 1, sutun: 2, tip: 'SEAT', koltukTipi: 'STANDARD' });
    expect(boya(koltuklu, 1, 2, 'KOLTUK')).toBe(koltuklu);
    expect(boya(koltuklu, 5, 0, 'KOLTUK')).toBe(koltuklu);
    expect(boya(koltuklu, 1, 2, 'ENGELLI_KOLTUK').hucreler.get('1,2')?.koltukTipi).toBe('ACCESSIBLE');
    expect(boya(koltuklu, 1, 2, 'SIL').hucreler.size).toBe(0);
    expect(boya(bos, 0, 0, 'SIL')).toBe(bos);
  });

  it('should label rows from the screen and number seats left to right skipping aisles', () => {
    // Perde üstte; 2. satır koltuksuz (koridor), 1. satırda ortada koridor
    const duzen = ciz(bosDuzen(5, 5), [
      [0, 1, 'PERDE'], [0, 2, 'PERDE'], [0, 3, 'PERDE'],
      [1, 0, 'KOLTUK'], [1, 1, 'KOLTUK'], [1, 3, 'ENGELLI_KOLTUK'], [1, 4, 'KOLTUK'],
      [3, 2, 'KOLTUK'], [4, 0, 'KOLTUK'],
    ]);

    expect([etiket(duzen, 1, 0), etiket(duzen, 1, 1), etiket(duzen, 1, 3), etiket(duzen, 1, 4)])
      .toEqual(['A-1', 'A-2', 'A-3', 'A-4']);
    expect(etiket(duzen, 3, 2)).toBe('B-1');
    expect(etiket(duzen, 4, 0)).toBe('C-1');
  });

  it('should start lettering from the bottom when screen is below seats', () => {
    const duzen = ciz(bosDuzen(4, 2), [[0, 0, 'KOLTUK'], [2, 0, 'KOLTUK'], [3, 0, 'PERDE'], [3, 1, 'PERDE']]);

    expect(etiket(duzen, 2, 0)).toBe('A-1');
    expect(etiket(duzen, 0, 0)).toBe('B-1');
  });

  it('should produce row letters beyond Z', () => {
    expect([siraHarfi(0), siraHarfi(25), siraHarfi(26), siraHarfi(27), siraHarfi(49)]).toEqual(['A', 'Z', 'AA', 'AB', 'AX']);
  });

  it('should resize grid, drop outside cells and report how many will be lost', () => {
    const duzen = ciz(bosDuzen(4, 4), [[0, 0, 'PERDE'], [3, 3, 'KOLTUK'], [1, 3, 'DUVAR']]);

    expect(silinecekHucreSayisi(duzen, 3, 4)).toBe(1);
    expect(silinecekHucreSayisi(duzen, 4, 3)).toBe(2);
    const kucuk = boyutDegistir(duzen, 3, 3);
    expect([kucuk.satirSayisi, kucuk.sutunSayisi, kucuk.hucreler.size]).toEqual([3, 3, 1]);
    expect(boyutDegistir(duzen, 4, 4)).toBe(duzen);
    expect(boyutDegistir(duzen, 99, 0)).toEqual(jasmine.objectContaining({ satirSayisi: 50, sutunSayisi: 1 }));
  });

  it('should require at least one seat and a screen', () => {
    expect(dogrula(bosDuzen(2, 2)).length).toBe(2);
    expect(dogrula(boya(bosDuzen(2, 2), 0, 0, 'PERDE'))).toEqual(['En az bir koltuk ekleyin.']);
    expect(dogrula(ciz(bosDuzen(2, 2), [[0, 0, 'PERDE'], [1, 0, 'KOLTUK']]))).toEqual([]);
  });

  it('should build server request ordered by position with seat labels only on seats', () => {
    const duzen = ciz(bosDuzen(3, 3), [[2, 1, 'ENGELLI_KOLTUK'], [0, 0, 'PERDE'], [2, 0, 'KOLTUK'], [1, 2, 'CIKIS']]);

    expect(istegeCevir(duzen)).toEqual({
      gridRows: 3,
      gridCols: 3,
      cells: [
        { row: 0, col: 0, type: 'SCREEN', seatRowLabel: null, seatNumber: null, seatType: null },
        { row: 1, col: 2, type: 'EXIT', seatRowLabel: null, seatNumber: null, seatType: null },
        { row: 2, col: 0, type: 'SEAT', seatRowLabel: 'A', seatNumber: 1, seatType: 'STANDARD' },
        { row: 2, col: 1, type: 'SEAT', seatRowLabel: 'A', seatNumber: 2, seatType: 'ACCESSIBLE' },
      ],
    });
    expect(koltukSayilari(duzen)).toEqual({ toplam: 2, engelli: 1 });
  });

  it('should rebuild editor layout from server cells and recompute labels', () => {
    const duzen = cevaptanKur(3, 3, [
      { cellId: 9, row: 0, col: 0, type: 'SCREEN', seatRowLabel: null, seatNumber: null, seatType: null },
      { cellId: 10, row: 2, col: 1, type: 'SEAT', seatRowLabel: 'X', seatNumber: 7, seatType: 'ACCESSIBLE' },
    ]);

    expect(duzen.hucreler.get('2,1')).toEqual({ satir: 2, sutun: 1, tip: 'SEAT', koltukTipi: 'ACCESSIBLE' });
    expect(etiket(duzen, 2, 1)).toBe('A-1');
  });

  it('should build preview layout with temporary cell ids', () => {
    const onizleme = onizlemeDuzeni(ciz(bosDuzen(2, 2), [[0, 0, 'PERDE'], [1, 1, 'KOLTUK']]));

    expect(onizleme.cells.map((h) => [h.cellId, h.type, h.seatRowLabel])).toEqual([[1, 'SCREEN', null], [2, 'SEAT', 'A']]);
    expect([onizleme.gridRows, onizleme.gridCols]).toEqual([2, 2]);
  });
});
