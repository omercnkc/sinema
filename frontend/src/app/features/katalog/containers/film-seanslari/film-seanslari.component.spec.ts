import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';
import { SharedModule } from '../../../../shared/shared.module';
import { SeansOzeti } from '../../models/seans.models';
import { SeansService } from '../../services/seans.service';
import { FilmSeanslariComponent } from './film-seanslari.component';

describe('FilmSeanslariComponent', () => {
  const BUGUN = '2026-10-31';
  function seans(id: number, subeId: number, subeAdi: string, saatUtc: string, salesOpen = true): SeansOzeti {
    return {
      id, startsAt: `${BUGUN}T${saatUtc}:00Z`, endsAt: `${BUGUN}T23:00:00Z`, cinemaId: subeId, cinemaName: subeAdi,
      hallId: 1, hallName: 'Salon 1', ticketPrice: '160.00', salesCloseAt: `${BUGUN}T23:00:00Z`, salesOpen,
    };
  }
  let fixture: ComponentFixture<FilmSeanslariComponent>;
  let seansService: jasmine.SpyObj<SeansService>;
  let parametreler: BehaviorSubject<ParamMap>;
  let yonlendir: jasmine.Spy;

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(`${BUGUN}T09:00:00Z`));
    parametreler = new BehaviorSubject(convertToParamMap({}));
    seansService = jasmine.createSpyObj<SeansService>('SeansService', ['filminSeanslari', 'subeler']);
    seansService.subeler.and.returnValue(of([{ id: 1, name: 'Kadıköy', city: 'İstanbul', address: '' }]));
    seansService.filminSeanslari.and.returnValue(of([
      seans(10, 1, 'Kadıköy', '10:00', false), seans(11, 2, 'Beşiktaş', '14:00'), seans(12, 1, 'Kadıköy', '17:00'),
    ]));
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [FilmSeanslariComponent],
      providers: [
        { provide: SeansService, useValue: seansService },
        { provide: ActivatedRoute, useValue: { queryParamMap: parametreler } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(FilmSeanslariComponent);
    fixture.componentInstance.filmId = 3;
  });

  afterEach(() => jasmine.clock().uninstall());

  function kok(): HTMLElement {
    return fixture.nativeElement;
  }

  it('should load today by default and group showtimes by cinema in Istanbul time', () => {
    fixture.detectChanges();

    expect(seansService.filminSeanslari).toHaveBeenCalledWith(3, BUGUN, null);
    const basliklar = Array.from(kok().querySelectorAll('h3')).map((h) => h.textContent?.trim());
    expect(basliklar).toEqual(['Kadıköy', 'Beşiktaş']);
    const kadikoy = kok().querySelectorAll('ul')[0];
    expect(kadikoy.textContent).toContain('13:00');
    expect(kadikoy.textContent).toContain('Satış kapandı');
    expect(kadikoy.querySelector('a')?.getAttribute('href')).toBe('/seans/12');
  });

  it('should read day and cinema from address and ignore days outside the week', () => {
    parametreler.next(convertToParamMap({ gun: '2026-11-02', sube: '1' }));
    fixture.detectChanges();
    expect(seansService.filminSeanslari).toHaveBeenCalledWith(3, '2026-11-02', 1);

    parametreler.next(convertToParamMap({ gun: '2027-01-01' }));
    fixture.detectChanges();
    expect(seansService.filminSeanslari).toHaveBeenCalledWith(3, BUGUN, null);
  });

  it('should write day and cinema choices to address without new history entries', () => {
    fixture.detectChanges();

    fixture.componentInstance.gunSecildi('2026-11-01');
    expect(yonlendir.calls.mostRecent().args[1]).toEqual(jasmine.objectContaining({ queryParams: { gun: '2026-11-01' },
      replaceUrl: true }));

    fixture.componentInstance.gunSecildi(BUGUN);
    expect(yonlendir.calls.mostRecent().args[1].queryParams).toEqual({ gun: null });

    fixture.componentInstance.subeSecildi('1');
    expect(yonlendir.calls.mostRecent().args[1].queryParams).toEqual({ sube: '1' });
  });

  it('should mark selected day as pressed and show empty day message', () => {
    seansService.filminSeanslari.and.returnValue(of([]));
    fixture.detectChanges();

    const secili = kok().querySelector('[aria-pressed="true"]');
    expect(secili?.textContent?.trim()).toBe('Bugün');
    expect(kok().textContent).toContain('Bu gün için seans yok.');
  });
});
