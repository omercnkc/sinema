import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient, HttpContext } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BildirimService } from '../bildirim/bildirim.service';
import { HataInterceptor, SESSIZ_HATA } from './hata.interceptor';
import { UygulamaHatasi } from './uygulama-hatasi';

describe('HataInterceptor', () => {
  const ADRES = '/api/deneme';
  let http: HttpClient;
  let sunucu: HttpTestingController;
  let bildirim: jasmine.SpyObj<BildirimService>;

  beforeEach(() => {
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['hata']);
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: HataInterceptor, multi: true },
        { provide: BildirimService, useValue: bildirim },
      ],
    });
    http = TestBed.inject(HttpClient);
    sunucu = TestBed.inject(HttpTestingController);
  });

  afterEach(() => sunucu.verify());

  function hataBekle(context?: HttpContext): Promise<UygulamaHatasi> {
    return new Promise((coz) => http.get(ADRES, { context }).subscribe({ error: coz }));
  }

  it('should convert problem+json to UygulamaHatasi and leave business errors to the caller', async () => {
    const hata = hataBekle();
    sunucu.expectOne(ADRES).flush(
      { status: 409, code: 'SEATS_UNAVAILABLE', detail: 'x', correlationId: 'k-1', unavailableCellIds: [5, 6] },
      { status: 409, statusText: 'Conflict' },
    );

    const sonuc = await hata;
    expect(sonuc).toEqual(jasmine.any(UygulamaHatasi));
    expect(sonuc.kod).toBe('SEATS_UNAVAILABLE');
    expect(sonuc.korelasyonKimligi).toBe('k-1');
    expect(sonuc.ekAlanlar['unavailableCellIds']).toEqual([5, 6]);
    expect(sonuc.ekAlanlar['detail']).toBeUndefined();
    expect(bildirim.hata).not.toHaveBeenCalled();
  });

  it('should carry field errors for validation failures', async () => {
    const hata = hataBekle();
    sunucu.expectOne(ADRES).flush(
      { status: 400, code: 'VALIDATION_FAILED', errors: [{ field: 'phone', message: 'Geçerli bir telefon girin.' }] },
      { status: 400, statusText: 'Bad Request' },
    );

    expect((await hata).alanHatalari).toEqual([{ field: 'phone', message: 'Geçerli bir telefon girin.' }]);
  });

  it('should notify user on network error and server error', async () => {
    const agHatasi = hataBekle();
    sunucu.expectOne(ADRES).error(new ProgressEvent('error'), { status: 0 });
    expect((await agHatasi).kod).toBe('HTTP_0');

    const sunucuHatasi = hataBekle();
    sunucu.expectOne(ADRES).flush({ code: 'CATALOG_UNAVAILABLE', detail: 'Seans bilgisi şu an alınamıyor.' },
      { status: 503, statusText: 'Service Unavailable' });
    await sunucuHatasi;

    expect(bildirim.hata.calls.allArgs()).toEqual([
      [jasmine.stringContaining('Sunucuya ulaşılamıyor')],
      ['Seans bilgisi şu an alınamıyor.'],
    ]);
  });

  it('should read wait time from Retry-After header when body has none', async () => {
    const hata = hataBekle();
    sunucu.expectOne(ADRES).flush({ code: 'RATE_LIMITED', detail: 'Çok fazla istek.' },
      { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '30' } });

    expect((await hata).beklemeSaniyesi).toBe(30);
    expect(bildirim.hata).toHaveBeenCalledWith('Çok fazla istek. 30 saniye sonra tekrar deneyin.');
  });

  it('should not notify for unauthorized or silenced requests', async () => {
    const yetkisiz = hataBekle();
    sunucu.expectOne(ADRES).flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });
    await yetkisiz;

    const sessiz = hataBekle(new HttpContext().set(SESSIZ_HATA, true));
    sunucu.expectOne(ADRES).flush({}, { status: 500, statusText: 'Server Error' });
    await sessiz;

    expect(bildirim.hata).not.toHaveBeenCalled();
  });
});
