import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Kullanici } from '../oturum/oturum.models';
import { OturumService } from '../oturum/oturum.service';
import { SekmeGorunurlugu } from '../zaman/sekme-gorunurlugu';
import { BildirimKutusuService } from './bildirim-kutusu.service';

describe('BildirimKutusuService', () => {
  const SAYI = `${environment.apiTabanAdresi}/bookings/notifications/unread-count`;
  const KULLANICI: Kullanici = { id: 1, email: 'a@b.com', role: 'USER', firstName: null, lastName: null, phone: null };
  let kullanici$: BehaviorSubject<Kullanici | null>;
  let gorunur$: BehaviorSubject<boolean>;
  let olaylar$: Subject<unknown>;
  let http: HttpTestingController;
  let servis: BildirimKutusuService;
  let sayilar: number[];
  let abonelik: Subscription;

  beforeEach(() => {
    kullanici$ = new BehaviorSubject<Kullanici | null>(null);
    gorunur$ = new BehaviorSubject<boolean>(true);
    olaylar$ = new Subject<unknown>();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: OturumService, useValue: { kullanici$ } },
        { provide: SekmeGorunurlugu, useValue: { gorunur$ } },
        { provide: Router, useValue: { events: olaylar$ } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    servis = TestBed.inject(BildirimKutusuService);
    sayilar = [];
  });

  /** timer(0, …) sıfır gecikmeyle de olsa fakeAsync'te ilerletilmeyi bekler. */
  function dinle(): void {
    abonelik = servis.okunmamisSayisi$.subscribe((sayi) => sayilar.push(sayi));
    tick();
  }

  afterEach(() => abonelik?.unsubscribe());

  it('should report zero for guests without asking the server', fakeAsync(() => {
    dinle();
    tick(environment.bildirimYenilemeMs);

    http.expectNone(SAYI);
    expect(sayilar).toEqual([0]);
  }));

  it('should poll while visible, pause when hidden, refresh on navigation and keep last count on error', fakeAsync(() => {
    kullanici$.next(KULLANICI);
    dinle();
    http.expectOne(SAYI).flush({ count: 2 });

    tick(environment.bildirimYenilemeMs);
    http.expectOne(SAYI).flush({ count: 3 });

    gorunur$.next(false);
    tick(environment.bildirimYenilemeMs * 2);
    http.expectNone(SAYI);

    olaylar$.next(new NavigationEnd(1, '/filmler', '/filmler'));
    http.expectOne(SAYI).flush('hata', { status: 503, statusText: 'Service Unavailable' });

    expect(sayilar).toEqual([2, 3]);
    kullanici$.next(null);
    expect(sayilar).toEqual([2, 3, 0]);
  }));

  it('should refresh the count right after marking a notification read', fakeAsync(() => {
    kullanici$.next(KULLANICI);
    dinle();
    http.expectOne(SAYI).flush({ count: 1 });

    servis.okundu(9).subscribe();
    http.expectOne(`${environment.apiTabanAdresi}/bookings/notifications/9/read`).flush(null);
    http.expectOne(SAYI).flush({ count: 0 });

    expect(sayilar).toEqual([1, 0]);
    gorunur$.next(false);
  }));
});
