import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { BehaviorSubject, of } from 'rxjs';
import { UygulamaBildirimi } from '../../core/bildirim-kutusu/bildirim-kutusu.models';
import { BildirimKutusuService } from '../../core/bildirim-kutusu/bildirim-kutusu.service';
import { BildirimZiliComponent } from './bildirim-zili.component';

describe('BildirimZiliComponent', () => {
  const OKUNMAMIS: UygulamaBildirimi = {
    id: 9, type: 'SEAT_CHANGED', reservationId: 18, read: false, createdAt: '2026-11-01T17:00:00Z',
    message: 'Salon düzeni değişti: Son Tren (Kadıköy, Salon 1) biletinizdeki koltuk F-7 → E-7 oldu.',
  };
  const OKUNMUS: UygulamaBildirimi = { ...OKUNMAMIS, id: 8, read: true, message: 'Eski bildirim' };
  let fixture: ComponentFixture<BildirimZiliComponent>;
  let sayi$: BehaviorSubject<number>;
  let kutu: { okunmamisSayisi$: BehaviorSubject<number>; sonBildirimler: jasmine.Spy; okundu: jasmine.Spy };
  let yonlendir: jasmine.Spy;

  beforeEach(() => {
    sayi$ = new BehaviorSubject(2);
    kutu = {
      okunmamisSayisi$: sayi$,
      sonBildirimler: jasmine.createSpy('sonBildirimler').and.returnValue(
        of({ content: [OKUNMAMIS, OKUNMUS], page: 0, size: 10, totalElements: 2, totalPages: 1 })),
      okundu: jasmine.createSpy('okundu').and.returnValue(of(undefined)),
    };
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, NgbDropdownModule],
      declarations: [BildirimZiliComponent],
      providers: [{ provide: BildirimKutusuService, useValue: kutu }],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(BildirimZiliComponent);
    fixture.detectChanges();
  });

  function zil(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('#bildirim-zili');
  }

  function ogeler(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('[data-test="bildirim"]'));
  }

  it('should show unread badge with accessible label, and no badge when all read', () => {
    expect(fixture.nativeElement.querySelector('[data-test="okunmamis-sayisi"]').textContent.trim()).toBe('2');
    expect(zil().getAttribute('aria-label')).toBe('Bildirimler, 2 okunmamış');

    sayi$.next(0);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-test="okunmamis-sayisi"]')).toBeNull();
    expect(zil().getAttribute('aria-label')).toBe('Bildirimler');
  });

  it('should load notifications when opened, unread highlighted', () => {
    expect(kutu.sonBildirimler).not.toHaveBeenCalled();

    zil().click();
    fixture.detectChanges();

    expect(kutu.sonBildirimler).toHaveBeenCalledTimes(1);
    expect(ogeler().length).toBe(2);
    expect(ogeler()[0].textContent).toContain('F-7 → E-7');
    expect(ogeler()[0].classList).toContain('bildirim-zili__oge--okunmamis');
    expect(ogeler()[1].classList).not.toContain('bildirim-zili__oge--okunmamis');
  });

  it('should mark unread as read and open the ticket; read ones only navigate', () => {
    zil().click();
    fixture.detectChanges();

    ogeler()[0].click();
    expect(kutu.okundu).toHaveBeenCalledOnceWith(9);
    expect(yonlendir).toHaveBeenCalledWith(['/rezervasyonlarim', 18]);

    zil().click();
    fixture.detectChanges();
    ogeler()[1].click();
    expect(kutu.okundu).toHaveBeenCalledTimes(1);
  });
});
