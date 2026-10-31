import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NgbCollapseModule, NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { BehaviorSubject, of } from 'rxjs';
import { Kullanici } from '../../core/oturum/oturum.models';
import { OturumService } from '../../core/oturum/oturum.service';
import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  let fixture: ComponentFixture<HeaderComponent>;
  let kok: HTMLElement;
  let kullanici$: BehaviorSubject<Kullanici | null>;
  let oturum: { kullanici$: BehaviorSubject<Kullanici | null>; cikisYap: jasmine.Spy };

  beforeEach(async () => {
    kullanici$ = new BehaviorSubject<Kullanici | null>(null);
    oturum = { kullanici$, cikisYap: jasmine.createSpy('cikisYap').and.returnValue(of(undefined)) };
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, NgbCollapseModule, NgbDropdownModule],
      declarations: [HeaderComponent],
      providers: [{ provide: OturumService, useValue: oturum }],
    }).compileComponents();
    fixture = TestBed.createComponent(HeaderComponent);
    kok = fixture.nativeElement;
    fixture.detectChanges();
  });

  function oturumAlani(): HTMLElement {
    return kok.querySelector('[data-test="oturum-alani"]') as HTMLElement;
  }

  it('should link brand to home and show films menu', () => {
    expect(kok.querySelector('a.navbar-brand')?.getAttribute('href')).toBe('/');
    expect(kok.querySelector('a.nav-link')?.textContent?.trim()).toBe('Filmler');
  });

  it('should toggle menu and reflect state in aria-expanded', () => {
    const dugme = kok.querySelector<HTMLButtonElement>('button.navbar-toggler');
    expect(dugme?.getAttribute('aria-expanded')).toBe('false');

    dugme?.click();
    fixture.detectChanges();

    expect(dugme?.getAttribute('aria-expanded')).toBe('true');
  });

  it('should show login and register links to guests', () => {
    const baglantilar = Array.from(oturumAlani().querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(baglantilar).toEqual(['/giris', '/kayit']);
  });

  it('should show user menu and log out', () => {
    const yonlendir = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    kullanici$.next({ id: 1, email: 'ayse@ornek.com', role: 'USER', firstName: 'Ayşe', lastName: 'Yılmaz', phone: null });
    fixture.detectChanges();

    expect(oturumAlani().querySelector('#kullanici-menusu')?.textContent?.trim()).toBe('Ayşe');
    expect(oturumAlani().querySelector('a[ngbDropdownItem]')?.getAttribute('href')).toBe('/rezervasyonlarim');
    fixture.componentInstance.cikisYap();

    expect(oturum.cikisYap).toHaveBeenCalled();
    expect(yonlendir).toHaveBeenCalledWith('/');
  });

  it('should fall back to email when user has no name', () => {
    kullanici$.next({ id: 1, email: 'admin@sinema.local', role: 'ADMIN', firstName: null, lastName: null, phone: null });
    fixture.detectChanges();

    expect(oturumAlani().querySelector('#kullanici-menusu')?.textContent?.trim()).toBe('admin@sinema.local');
  });
});
