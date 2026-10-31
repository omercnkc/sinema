import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { environment } from '../../../environments/environment';
import { BildirimService } from '../bildirim/bildirim.service';
import { SESSIZ_HATA } from '../hata/hata.interceptor';
import { Kullanici } from './oturum.models';
import { OturumService } from './oturum.service';

describe('OturumService', () => {
  const AUTH = `${environment.apiTabanAdresi}/auth`;
  const KULLANICI: Kullanici = {
    id: 7, email: 'ayse@ornek.com', role: 'USER', firstName: 'Ayşe', lastName: 'Yılmaz', phone: '+905321234567',
  };
  let servis: OturumService;
  let sunucu: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule, RouterTestingModule] });
    servis = TestBed.inject(OturumService);
    sunucu = TestBed.inject(HttpTestingController);
  });

  afterEach(() => sunucu.verify());

  function sonKullanici(): Kullanici | null {
    let son: Kullanici | null = null;
    servis.kullanici$.subscribe((kullanici) => (son = kullanici)).unsubscribe();
    return son;
  }

  it('should log in with credentials cookie and load the user', () => {
    servis.girisYap({ email: 'ayse@ornek.com', password: 'Parola123' }).subscribe();

    const giris = sunucu.expectOne(`${AUTH}/login`);
    expect(giris.request.withCredentials).toBeTrue();
    giris.flush({ accessToken: 'erisim-1', expiresInSeconds: 900 });
    sunucu.expectOne(`${AUTH}/me`).flush(KULLANICI);

    expect(servis.erisimTokeni).toBe('erisim-1');
    expect(sonKullanici()).toEqual(KULLANICI);
  });

  it('should share one refresh request between concurrent callers', () => {
    const tokenlar: string[] = [];
    servis.yenile().subscribe((token) => tokenlar.push(token));
    servis.yenile().subscribe((token) => tokenlar.push(token));

    const yenileme = sunucu.expectOne(`${AUTH}/refresh`);
    expect(yenileme.request.withCredentials).toBeTrue();
    expect(yenileme.request.context.get(SESSIZ_HATA)).toBeTrue();
    yenileme.flush({ accessToken: 'erisim-2', expiresInSeconds: 900 });

    expect(tokenlar).toEqual(['erisim-2', 'erisim-2']);
    expect(servis.erisimTokeni).toBe('erisim-2');
  });

  it('should restore previous session on startup', () => {
    let sonuc: Kullanici | null | undefined;
    servis.oturumuGeriYukle().subscribe((kullanici) => (sonuc = kullanici));

    sunucu.expectOne(`${AUTH}/refresh`).flush({ accessToken: 'erisim-3', expiresInSeconds: 900 });
    const ben = sunucu.expectOne(`${AUTH}/me`);
    ben.flush(KULLANICI);

    expect(sonuc).toEqual(KULLANICI);
  });

  it('should continue as guest when there is no previous session', () => {
    let tamamlandi = false;
    servis.oturumuGeriYukle().subscribe({ next: (kullanici) => expect(kullanici).toBeNull(), complete: () => (tamamlandi = true) });

    sunucu.expectOne(`${AUTH}/refresh`).flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    expect(tamamlandi).toBeTrue();
    expect(servis.erisimTokeni).toBeNull();
    expect(sonKullanici()).toBeNull();
  });

  it('should clear local session on logout even when server fails', () => {
    servis.girisYap({ email: 'ayse@ornek.com', password: 'Parola123' }).subscribe();
    sunucu.expectOne(`${AUTH}/login`).flush({ accessToken: 'erisim-1', expiresInSeconds: 900 });
    sunucu.expectOne(`${AUTH}/me`).flush(KULLANICI);

    servis.cikisYap().subscribe();
    sunucu.expectOne(`${AUTH}/logout`).flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(servis.erisimTokeni).toBeNull();
    expect(sonKullanici()).toBeNull();
  });

  it('should send user to login with return address when session ends', () => {
    const router = TestBed.inject(Router);
    const yonlendir = spyOn(router, 'navigate').and.resolveTo(true);
    const bilgi = spyOn(TestBed.inject(BildirimService), 'bilgi');

    servis.oturumSonaErdi();

    expect(yonlendir).toHaveBeenCalledWith(['/giris'], { queryParams: { donus: '/' } });
    expect(bilgi).toHaveBeenCalled();
  });
});
