import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { istanbulGunleri, istanbulGunu } from './is-gunu';

describe('İstanbul günü', () => {
  beforeAll(() => registerLocaleData(localeTr));

  it('should use Istanbul date regardless of browser time zone', () => {
    // 21:30 UTC = İstanbul'da ertesi gün 00:30
    expect(istanbulGunu(new Date('2026-11-01T21:30:00Z'))).toBe('2026-11-02');
    expect(istanbulGunu(new Date('2026-11-01T20:59:00Z'))).toBe('2026-11-01');
  });

  it('should list days from today with Turkish labels', () => {
    const gunler = istanbulGunleri(new Date('2026-10-31T09:00:00Z'), 4, 'tr');

    expect(gunler.map((gun) => gun.tarih)).toEqual(['2026-10-31', '2026-11-01', '2026-11-02', '2026-11-03']);
    expect(gunler.map((gun) => gun.etiket)).toEqual(['Bugün', 'Yarın', 'Pzt 2 Kas', 'Sal 3 Kas']);
  });
});
