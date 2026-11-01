import { of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../core/hata/uygulama-hatasi';
import { sessizYukleme, YuklemeDurumu, yuklemeDurumu } from './yukleme-durumu';

describe('yuklemeDurumu', () => {
  function topla<T>(kaynak: ReturnType<typeof yuklemeDurumu>): YuklemeDurumu<T>[] {
    const durumlar: YuklemeDurumu<T>[] = [];
    kaynak.subscribe((durum) => durumlar.push(durum as YuklemeDurumu<T>));
    return durumlar;
  }

  it('should emit loading then data', () => {
    expect(topla<number>(yuklemeDurumu(of(42)))).toEqual([
      { yukleniyor: true, veri: null, hata: null },
      { yukleniyor: false, veri: 42, hata: null },
    ]);
  });

  it('should turn errors into state instead of failing the stream', () => {
    const hata = new UygulamaHatasi(503, 'CATALOG_UNAVAILABLE', 'Katalog şu an kullanılamıyor.');
    const durumlar = topla(yuklemeDurumu(throwError(() => hata)));

    expect(durumlar[1]).toEqual({ yukleniyor: false, veri: null, hata });
    expect(topla(yuklemeDurumu(throwError(() => new Error('x'))))[1].hata?.kod).toBe('UNEXPECTED');
  });

  it('should refresh silently: no loading state and errors swallowed', () => {
    expect(topla<number>(sessizYukleme(of(7)))).toEqual([{ yukleniyor: false, veri: 7, hata: null }]);
    expect(topla(sessizYukleme(throwError(() => new Error('x'))))).toEqual([]);
  });
});
