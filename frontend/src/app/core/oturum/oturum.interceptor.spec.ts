import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Observable, Subject, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OturumInterceptor } from './oturum.interceptor';
import { OturumService } from './oturum.service';

describe('OturumInterceptor', () => {
  const API = environment.apiTabanAdresi;
  let http: HttpClient;
  let sunucu: HttpTestingController;
  let oturum: { erisimTokeni: string | null; yenile: jasmine.Spy<() => Observable<string>>; oturumSonaErdi: jasmine.Spy };

  beforeEach(() => {
    oturum = { erisimTokeni: 'eski', yenile: jasmine.createSpy('yenile'), oturumSonaErdi: jasmine.createSpy('oturumSonaErdi') };
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: OturumInterceptor, multi: true },
        { provide: OturumService, useValue: oturum },
      ],
    });
    http = TestBed.inject(HttpClient);
    sunucu = TestBed.inject(HttpTestingController);
  });

  afterEach(() => sunucu.verify());

  function yetkisiz(): [object, { status: number; statusText: string }] {
    return [{ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' }];
  }

  it('should add bearer token only to our API', () => {
    http.get(`${API}/bookings/reservations`).subscribe();
    http.get('https://image.tmdb.org/t/p/w500/a.jpg').subscribe();

    expect(sunucu.expectOne(`${API}/bookings/reservations`).request.headers.get('Authorization')).toBe('Bearer eski');
    expect(sunucu.expectOne('https://image.tmdb.org/t/p/w500/a.jpg').request.headers.has('Authorization')).toBeFalse();
  });

  it('should not add token to session endpoints', () => {
    http.post(`${API}/auth/refresh`, null).subscribe();

    expect(sunucu.expectOne(`${API}/auth/refresh`).request.headers.has('Authorization')).toBeFalse();
  });

  it('should refresh once and retry concurrent requests after 401', () => {
    const yenileme = new Subject<string>();
    oturum.yenile.and.returnValue(yenileme);
    const cevaplar: unknown[] = [];
    http.get(`${API}/a`).subscribe((cevap) => cevaplar.push(cevap));
    http.get(`${API}/b`).subscribe((cevap) => cevaplar.push(cevap));

    sunucu.expectOne(`${API}/a`).flush(...yetkisiz());
    sunucu.expectOne(`${API}/b`).flush(...yetkisiz());
    yenileme.next('yeni');
    yenileme.complete();

    const tekrarA = sunucu.expectOne(`${API}/a`);
    const tekrarB = sunucu.expectOne(`${API}/b`);
    expect(tekrarA.request.headers.get('Authorization')).toBe('Bearer yeni');
    tekrarA.flush('A');
    tekrarB.flush('B');
    expect(cevaplar).toEqual(['A', 'B']);
  });

  it('should not loop when retried request gets 401 again', () => {
    oturum.yenile.and.returnValue(new Observable((abone) => { abone.next('yeni'); abone.complete(); }));
    let hata: unknown;
    http.get(`${API}/a`).subscribe({ error: (h) => (hata = h) });

    sunucu.expectOne(`${API}/a`).flush(...yetkisiz());
    sunucu.expectOne(`${API}/a`).flush(...yetkisiz());

    expect((hata as HttpErrorResponse).status).toBe(401);
    expect(oturum.yenile).toHaveBeenCalledTimes(1);
  });

  it('should end session and pass original error when refresh fails', () => {
    oturum.yenile.and.returnValue(throwError(() => new Error('refresh reddedildi')));
    let hata: unknown;
    http.get(`${API}/a`).subscribe({ error: (h) => (hata = h) });

    sunucu.expectOne(`${API}/a`).flush(...yetkisiz());

    expect(oturum.oturumSonaErdi).toHaveBeenCalled();
    expect((hata as HttpErrorResponse).status).toBe(401);
  });

  it('should not try refresh for guest requests', () => {
    oturum.erisimTokeni = null;
    http.get(`${API}/a`).subscribe({ error: () => undefined });

    sunucu.expectOne(`${API}/a`).flush(...yetkisiz());

    expect(oturum.yenile).not.toHaveBeenCalled();
  });
});
