import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { boya, bosDuzen, EditorDuzeni } from '../../models/duzen-editoru.models';
import { DuzenCevabi, DuzenOzeti } from '../../models/duzen-yonetimi.models';
import { YonetimSube } from '../../models/sube-yonetimi.models';
import { DuzenYonetimiService } from '../../services/duzen-yonetimi.service';
import { KaydedilmemisDegisiklikGuard } from '../../services/kaydedilmemis-degisiklik.guard';
import { SubeYonetimiService } from '../../services/sube-yonetimi.service';
import { DuzenSayfasiComponent } from './duzen-sayfasi.component';

describe('DuzenSayfasiComponent', () => {
  const SUBE: YonetimSube = {
    id: 1, name: 'Kadıköy', city: 'İstanbul', address: 'Bahariye',
    halls: [{ id: 11, cinemaId: 1, name: 'Salon 1', defaultTicketPrice: '160.00', activeLayoutId: 5 }],
  };
  const AKTIF: DuzenOzeti = {
    id: 5, version: 2, status: 'PUBLISHED', active: true, gridRows: 3, gridCols: 3, seatCount: 1,
    createdAt: '2026-10-30T10:00:00Z', publishedAt: '2026-10-30T10:05:00Z',
  };
  const TASLAK: DuzenOzeti = { ...AKTIF, id: 6, version: null, status: 'DRAFT', active: false, publishedAt: null };
  const CEVAP: DuzenCevabi = {
    id: 5, hallId: 11, version: 2, status: 'PUBLISHED', active: true, gridRows: 3, gridCols: 3, cells: [
      { cellId: 1, row: 0, col: 1, type: 'SCREEN', seatRowLabel: null, seatNumber: null, seatType: null },
      { cellId: 2, row: 2, col: 1, type: 'SEAT', seatRowLabel: 'A', seatNumber: 1, seatType: 'STANDARD' },
    ],
  };
  /** Geçerli (perde + koltuk) çizim. */
  const CIZIM: EditorDuzeni = boya(boya(bosDuzen(3, 3), 0, 0, 'PERDE'), 2, 2, 'KOLTUK');

  let fixture: ComponentFixture<DuzenSayfasiComponent>;
  let bilesen: DuzenSayfasiComponent;
  let duzenService: jasmine.SpyObj<DuzenYonetimiService>;
  let bildirim: jasmine.SpyObj<BildirimService>;

  function olustur(salonId: string, surumler: DuzenOzeti[]): void {
    duzenService = jasmine.createSpyObj<DuzenYonetimiService>('DuzenYonetimiService',
      ['listele', 'getir', 'taslakOlustur', 'taslakGuncelle', 'yayimla']);
    duzenService.listele.and.returnValue(of(surumler));
    duzenService.getir.and.callFake((_, duzenId) => of({ ...CEVAP, id: duzenId }));
    const subeService = jasmine.createSpyObj<SubeYonetimiService>('SubeYonetimiService', ['listele']);
    subeService.listele.and.returnValue(of([SUBE]));
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['basari']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [DuzenSayfasiComponent],
      providers: [
        { provide: DuzenYonetimiService, useValue: duzenService },
        { provide: SubeYonetimiService, useValue: subeService },
        { provide: BildirimService, useValue: bildirim },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ salonId }) } } },
      ],
      // Editör kendi testinde; burada düzen değişikliği doğrudan tetiklenir
      schemas: [NO_ERRORS_SCHEMA],
    });
    fixture = TestBed.createComponent(DuzenSayfasiComponent);
    bilesen = fixture.componentInstance;
    fixture.detectChanges();
  }

  function yenile(): void {
    fixture.debugElement.injector.get(ChangeDetectorRef).markForCheck();
    fixture.detectChanges();
  }

  function dugme(metin: string): HTMLButtonElement {
    const dugmeler = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    return dugmeler.find((d) => d.textContent?.trim().startsWith(metin)) as HTMLButtonElement;
  }

  function metin(secici: string): string {
    return fixture.nativeElement.querySelector(secici)?.textContent?.trim() ?? '';
  }

  it('should open existing draft and save it with PUT', () => {
    olustur('11', [TASLAK, AKTIF]);
    duzenService.taslakGuncelle.and.returnValue(of({ ...CEVAP, id: 6 }));

    expect(duzenService.getir).toHaveBeenCalledWith(11, 6);
    expect(metin('h2')).toBe('Kadıköy · Salon 1');
    expect(metin('[data-test="duzen-kaynagi"]')).toContain('Kayıtlı taslak');
    expect(dugme('Taslağı kaydet').disabled).toBeTrue();
    expect(dugme('Yayımla').disabled).toBeFalse();

    bilesen.duzenDegisti(CIZIM);
    yenile();
    expect(dugme('Yayımla').disabled).toBeTrue();
    expect(metin('[data-test="kayit-durumu"]')).toContain('Kaydedilmemiş');
    dugme('Taslağı kaydet').click();
    yenile();

    expect(duzenService.taslakGuncelle).toHaveBeenCalledWith(11, 6, jasmine.objectContaining({ gridRows: 3, gridCols: 3 }));
    expect(bilesen.kaydedilmemisDegisiklikVar).toBeFalse();
    expect(metin('[data-test="kayit-durumu"]')).toBe('Taslak kayıtlı.');
  });

  it('should start from active version copy, create draft on save, then publish after confirmation', () => {
    olustur('11', [AKTIF]);
    duzenService.taslakOlustur.and.returnValue(of({ ...CEVAP, id: 7, status: 'DRAFT' }));
    duzenService.yayimla.and.returnValue(of({ ...CEVAP, id: 7, version: 3 }));

    expect(metin('[data-test="duzen-kaynagi"]')).toContain('sürüm 2');
    expect(dugme('Taslağı kaydet').disabled).toBeFalse();
    expect(dugme('Yayımla').disabled).toBeTrue();

    dugme('Taslağı kaydet').click();
    yenile();
    expect(duzenService.taslakOlustur).toHaveBeenCalledWith(11, jasmine.objectContaining({ gridRows: 3 }));
    expect(bilesen.taslakId).toBe(7);

    dugme('Yayımla').click();
    yenile();
    expect(fixture.nativeElement.querySelector('[role="alertdialog"]')).not.toBeNull();
    duzenService.listele.and.returnValue(of([{ ...AKTIF, id: 7, version: 3 }, { ...AKTIF, active: false }]));
    dugme('Evet, yayımla').click();
    yenile();

    expect(duzenService.yayimla).toHaveBeenCalledWith(11, 7);
    expect(bildirim.basari).toHaveBeenCalledWith('Sürüm 3 yayımlandı. Bundan sonra eklenen seanslar bu düzeni kullanır.');
    expect(fixture.nativeElement.querySelector('[role="alertdialog"]')).toBeNull();
    expect(metin('[data-test="duzen-kaynagi"]')).toContain('sürüm 3');
  });

  it('should start with empty grid and keep save disabled until layout is valid', () => {
    olustur('11', []);

    expect(metin('[data-test="duzen-kaynagi"]')).toContain('henüz düzeni yok');
    expect(bilesen.duzen?.satirSayisi).toBe(10);
    expect(dugme('Taslağı kaydet').disabled).toBeTrue();

    bilesen.duzenDegisti(CIZIM);
    yenile();
    expect(dugme('Taslağı kaydet').disabled).toBeFalse();
  });

  it('should mark changes drawn during save as still unsaved', () => {
    olustur('11', [TASLAK]);
    const cevap$ = new Subject<DuzenCevabi>();
    duzenService.taslakGuncelle.and.returnValue(cevap$);
    bilesen.duzenDegisti(CIZIM);
    bilesen.kaydetIstendi();

    bilesen.duzenDegisti(boya(CIZIM, 1, 1, 'KOLTUK'));
    cevap$.next({ ...CEVAP, id: 6 });
    cevap$.complete();

    expect(bilesen.kaydedilmemisDegisiklikVar).toBeTrue();
  });

  it('should show server violations and showtime in progress error', () => {
    olustur('11', [TASLAK]);
    bilesen.duzenDegisti(CIZIM);
    duzenService.taslakGuncelle.and.returnValue(throwError(() => new UygulamaHatasi(422, 'LAYOUT_INVALID',
      'Düzen kurallara uymuyor.', [], null, null, { violations: ['(0,0): hücre ızgaranın dışında'] })));

    bilesen.kaydetIstendi();
    yenile();
    expect(metin('[data-test="sunucu-ihlalleri"]')).toBe('(0,0): hücre ızgaranın dışında');

    duzenService.taslakGuncelle.and.returnValue(of({ ...CEVAP, id: 6 }));
    bilesen.kaydetIstendi();
    duzenService.yayimla.and.returnValue(throwError(() => new UygulamaHatasi(409, 'HALL_SHOWTIME_IN_PROGRESS',
      'Salonda süren bir seans var; düzen seanssız aralıkta yayımlanabilir (K13).')));
    bilesen.yayimlaIstendi();
    yenile();

    expect(fixture.nativeElement.querySelector('app-hata-mesaji').textContent).toContain('süren bir seans');
    expect(fixture.nativeElement.querySelector('[data-test="sunucu-ihlalleri"]')).toBeNull();
  });

  it('should toggle customer preview with current drawing', () => {
    olustur('11', [AKTIF]);

    dugme('Müşteri gözüyle önizle').click();
    yenile();
    expect(fixture.nativeElement.querySelectorAll('app-koltuk-haritasi button.koltuk').length).toBe(1);

    bilesen.duzenDegisti(boya(bilesen.duzen as EditorDuzeni, 2, 0, 'KOLTUK'));
    yenile();
    expect(fixture.nativeElement.querySelectorAll('app-koltuk-haritasi button.koltuk').length).toBe(2);

    dugme('Önizlemeyi kapat').click();
    yenile();
    expect(fixture.nativeElement.querySelector('app-koltuk-haritasi')).toBeNull();
  });

  it('should show not found for unknown hall', () => {
    olustur('99', []);

    expect(fixture.nativeElement.textContent).toContain('Salon bulunamadı.');
    expect(duzenService.getir).not.toHaveBeenCalled();
  });

  it('should ask before leaving with unsaved changes', () => {
    const guard = new KaydedilmemisDegisiklikGuard();
    const onay = spyOn(window, 'confirm').and.returnValue(false);

    expect(guard.canDeactivate({ kaydedilmemisDegisiklikVar: false })).toBeTrue();
    expect(onay).not.toHaveBeenCalled();
    expect(guard.canDeactivate({ kaydedilmemisDegisiklikVar: true })).toBeFalse();
    expect(onay).toHaveBeenCalled();
  });
});
