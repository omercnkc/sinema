import { DatePipe, registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { IS_SAAT_DILIMI } from './is-saat-dilimi';

/**
 * §9.3-4 doğrulaması: Angular 13 DatePipe'ın '+0300' ofseti ve tr yerel ayarıyla davranışı. Testi çalıştıran
 * makinenin saat diliminden bağımsız olmalı (UTC'deki bir CI ile İstanbul'daki geliştirici aynı sonucu görür).
 */
describe('İş saat dilimi (DatePipe)', () => {
  let pipe: DatePipe;

  beforeAll(() => registerLocaleData(localeTr));
  beforeEach(() => (pipe = new DatePipe('tr')));

  it('should show UTC instant as Istanbul local time', () => {
    expect(pipe.transform('2026-11-01T17:00:00Z', 'HH:mm', IS_SAAT_DILIMI)).toBe('20:00');
  });

  it('should move to next day after 21:00 UTC', () => {
    expect(pipe.transform('2026-11-01T21:30:00Z', 'dd.MM.yyyy HH:mm', IS_SAAT_DILIMI)).toBe('02.11.2026 00:30');
  });

  it('should use Turkish month and weekday names', () => {
    expect(pipe.transform('2026-11-01T17:00:00Z', 'd MMMM EEEE', IS_SAAT_DILIMI)).toBe('1 Kasım Pazar');
  });
});
