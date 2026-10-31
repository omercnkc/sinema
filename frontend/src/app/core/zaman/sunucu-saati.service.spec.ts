import { SunucuSaati } from './sunucu-saati.service';

describe('SunucuSaati', () => {
  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2026-10-31T10:00:00Z'));
  });

  afterEach(() => jasmine.clock().uninstall());

  it('should follow server time when client clock is off', () => {
    const saat = new SunucuSaati();
    saat.farkiGuncelle('2026-10-31T10:02:00Z');

    expect(new Date(saat.simdi()).toISOString()).toBe('2026-10-31T10:02:00.000Z');
    jasmine.clock().tick(5000);
    expect(new Date(saat.simdi()).toISOString()).toBe('2026-10-31T10:02:05.000Z');
  });

  it('should ignore invalid server time', () => {
    const saat = new SunucuSaati();
    saat.farkiGuncelle('gecersiz');

    expect(new Date(saat.simdi()).toISOString()).toBe('2026-10-31T10:00:00.000Z');
  });
});
