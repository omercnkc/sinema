import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Kullanici } from '../../../../core/oturum/oturum.models';
import { OturumService } from '../../../../core/oturum/oturum.service';
import { SekmeGorunurlugu } from '../../../../core/zaman/sekme-gorunurlugu';
import { SharedModule } from '../../../../shared/shared.module';
import { DolulukCevabi, KoltukHaritasiGorunumu } from '../../models/koltuk-haritasi.models';
import { ornekDuzen } from '../../models/koltuk-haritasi.models.spec';
import { KoltukHaritasiService } from '../../services/koltuk-haritasi.service';
import { KoltukSecimiComponent } from './koltuk-secimi.component';

describe('KoltukSecimiComponent', () => {
  const ARALIK = environment.dolulukYenilemeMs;
  let fixture: ComponentFixture<KoltukSecimiComponent>;
  let servis: jasmine.SpyObj<KoltukHaritasiService>;
  let gorunur: BehaviorSubject<boolean>;
  let kullanici: BehaviorSubject<Kullanici | null>;

  function doluluk(...tutulular: number[]): DolulukCevabi {
    return { serverTime: '', occupied: tutulular.map((cellId) => ({ cellId, status: 'HELD' as const })) };
  }

  beforeEach(() => {
    servis = jasmine.createSpyObj<KoltukHaritasiService>('KoltukHaritasiService', ['duzen', 'doluluk']);
    servis.duzen.and.returnValue(of(ornekDuzen()));
    servis.doluluk.and.returnValue(of(doluluk()));
    gorunur = new BehaviorSubject<boolean>(true);
    kullanici = new BehaviorSubject<Kullanici | null>(null);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [KoltukSecimiComponent],
      providers: [
        { provide: KoltukHaritasiService, useValue: servis },
        { provide: SekmeGorunurlugu, useValue: { gorunur$: gorunur } },
        { provide: OturumService, useValue: { kullanici$: kullanici } },
      ],
      // Haritanın çizimi kendi testinde; burada ona giden görünüm denetlenir
      schemas: [NO_ERRORS_SCHEMA],
    });
    fixture = TestBed.createComponent(KoltukSecimiComponent);
    fixture.componentInstance.seansId = 101;
  });

  /** Sayfanın haritaya verdiği görünüm (NO_ERRORS_SCHEMA'da bağlama DOM özelliğine yazılır). */
  function haritaGorunumu(): KoltukHaritasiGorunumu {
    const harita = fixture.nativeElement.querySelector('app-koltuk-haritasi') as { gorunum: KoltukHaritasiGorunumu };
    return harita.gorunum;
  }

  function durum(id: number): string | null | undefined {
    return haritaGorunumu().hucreler.find((h) => h.hucre.cellId === id)?.durum;
  }

  it('should poll occupancy immediately and then every interval, loading layout once', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    expect(servis.doluluk).toHaveBeenCalledTimes(1);

    servis.doluluk.and.returnValue(of(doluluk(10)));
    tick(ARALIK);
    fixture.detectChanges();

    expect(servis.doluluk).toHaveBeenCalledTimes(2);
    expect(servis.duzen).toHaveBeenCalledTimes(1);
    expect(durum(10)).toBe('TUTULU');
    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('should stop polling while tab is hidden and refresh as soon as it is visible again', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    gorunur.next(false);
    tick(ARALIK * 3);
    expect(servis.doluluk).toHaveBeenCalledTimes(1);

    gorunur.next(true);
    tick();
    expect(servis.doluluk).toHaveBeenCalledTimes(2);
    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('should keep last occupancy and warn when refresh fails', fakeAsync(() => {
    servis.doluluk.and.returnValue(of(doluluk(10)));
    fixture.detectChanges();
    tick();

    servis.doluluk.and.returnValue(throwError(() => new Error('ağ')));
    tick(ARALIK);
    fixture.detectChanges();

    expect(durum(10)).toBe('TUTULU');
    expect(fixture.nativeElement.textContent).toContain('Koltuk durumu güncellenemedi');

    servis.doluluk.and.returnValue(of(doluluk()));
    tick(ARALIK);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Koltuk durumu güncellenemedi');
    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('should ask guests to log in and come back to this showtime', fakeAsync(() => {
    fixture.detectChanges();

    const baglanti: HTMLAnchorElement = fixture.nativeElement.querySelector('a[href^="/giris"]');
    expect(baglanti.getAttribute('href')).toBe('/giris?donus=%2Fseans%2F101');

    kullanici.next({ id: 1, email: 'a@b.com', role: 'USER', firstName: null, lastName: null, phone: null });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href^="/giris"]')).toBeNull();
    fixture.destroy();
    discardPeriodicTasks();
  }));
});
