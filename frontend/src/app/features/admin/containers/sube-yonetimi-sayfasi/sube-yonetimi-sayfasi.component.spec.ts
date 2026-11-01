import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { SalonFormuComponent } from '../../components/salon-formu/salon-formu.component';
import { SubeFormuComponent } from '../../components/sube-formu/sube-formu.component';
import { YonetimSalon, YonetimSube } from '../../models/sube-yonetimi.models';
import { SubeYonetimiService } from '../../services/sube-yonetimi.service';
import { SubeYonetimiSayfasiComponent } from './sube-yonetimi-sayfasi.component';

describe('SubeYonetimiSayfasiComponent', () => {
  const SALON: YonetimSalon = { id: 11, cinemaId: 1, name: 'Salon 1', defaultTicketPrice: '160.00', activeLayoutId: 5 };
  const DUZENSIZ: YonetimSalon = { ...SALON, id: 12, name: 'Salon 2', activeLayoutId: null };
  const SUBE: YonetimSube = { id: 1, name: 'Kadıköy', city: 'İstanbul', address: 'Bahariye Cd.', halls: [SALON, DUZENSIZ] };
  let fixture: ComponentFixture<SubeYonetimiSayfasiComponent>;
  let subeService: jasmine.SpyObj<SubeYonetimiService>;
  let bildirim: jasmine.SpyObj<BildirimService>;

  beforeEach(() => {
    subeService = jasmine.createSpyObj<SubeYonetimiService>('SubeYonetimiService',
      ['listele', 'subeOlustur', 'subeGuncelle', 'salonOlustur', 'salonGuncelle']);
    subeService.listele.and.returnValue(of([SUBE]));
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['basari']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [SubeYonetimiSayfasiComponent, SubeFormuComponent, SalonFormuComponent],
      providers: [
        { provide: SubeYonetimiService, useValue: subeService },
        { provide: BildirimService, useValue: bildirim },
      ],
    });
    fixture = TestBed.createComponent(SubeYonetimiSayfasiComponent);
    fixture.detectChanges();
  });

  function kok(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function dugme(etiket: string): HTMLButtonElement {
    return kok().querySelector(`button[aria-label="${etiket}"]`) as HTMLButtonElement;
  }

  function yaz(secici: string, deger: string): void {
    const kutu = kok().querySelector(secici) as HTMLInputElement;
    kutu.value = deger;
    kutu.dispatchEvent(new Event('input'));
  }

  function formuGonder(): void {
    kok().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('should list cinemas with halls, prices and layout status', () => {
    const salonlar = Array.from(kok().querySelectorAll('[data-test="salon-satiri"]'));

    expect(kok().querySelector('[data-test="sube-karti"] h3')?.textContent).toContain('Kadıköy');
    expect(salonlar[0].textContent).toContain('₺160');
    expect(salonlar.map((satir) => satir.querySelector('.badge')?.textContent?.trim())).toEqual(['Düzen yayımlı', 'Düzen yok']);
    expect(salonlar[1].querySelector('a')?.getAttribute('href')).toBe('/admin/subeler/salonlar/12/duzen');
  });

  it('should update cinema, notify and refresh list silently', () => {
    subeService.subeGuncelle.and.returnValue(of({ ...SUBE, name: 'Kadıköy Merkez' }));
    dugme('Kadıköy şubesini düzenle').click();
    fixture.detectChanges();
    expect((kok().querySelector('#sube-ad') as HTMLInputElement).value).toBe('Kadıköy');

    yaz('#sube-ad', ' Kadıköy Merkez ');
    formuGonder();

    expect(subeService.subeGuncelle).toHaveBeenCalledWith(1, { name: 'Kadıköy Merkez', city: 'İstanbul', address: 'Bahariye Cd.' });
    expect(bildirim.basari).toHaveBeenCalledWith('Kadıköy Merkez güncellendi.');
    expect(subeService.listele).toHaveBeenCalledTimes(2);
    expect(kok().querySelector('form')).toBeNull();
    expect(kok().querySelector('app-yukleniyor')).toBeNull();
  });

  it('should add hall with comma price sent as dot', () => {
    subeService.salonOlustur.and.returnValue(of({ ...SALON, id: 13 }));
    dugme('Kadıköy şubesine salon ekle').click();
    fixture.detectChanges();

    yaz('#salon-ad', 'Salon 3');
    yaz('#salon-fiyat', '175,5');
    formuGonder();

    expect(subeService.salonOlustur).toHaveBeenCalledWith(1, { name: 'Salon 3', defaultTicketPrice: '175.5' });
  });

  it('should validate price on client and show taken hall name under the field', () => {
    dugme('Salon 2 salonunu düzenle').click();
    fixture.detectChanges();

    yaz('#salon-fiyat', '12,345');
    formuGonder();
    expect(subeService.salonGuncelle).not.toHaveBeenCalled();
    expect(kok().querySelector('#salon-fiyat-hata')?.textContent).toContain('Geçerli bir tutar');

    subeService.salonGuncelle.and.returnValue(throwError(() =>
      new UygulamaHatasi(409, 'HALL_NAME_TAKEN', 'Bu şubede aynı adlı bir salon var.')));
    yaz('#salon-fiyat', '150');
    yaz('#salon-ad', 'Salon 1');
    formuGonder();

    expect(subeService.salonGuncelle).toHaveBeenCalledWith(12, { name: 'Salon 1', defaultTicketPrice: '150' });
    expect(kok().querySelector('#salon-ad-hata')?.textContent?.trim()).toBe('Bu şubede aynı adlı bir salon var.');
    expect(kok().querySelector('form')).not.toBeNull();
  });

  it('should keep a single editor open and lock buttons while saving', () => {
    subeService.subeOlustur.and.returnValue(new Subject<YonetimSube>());
    dugme('Salon 1 salonunu düzenle').click();
    fixture.detectChanges();
    (kok().querySelector('.btn.btn-primary.btn-sm') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(kok().querySelectorAll('form').length).toBe(1);
    expect(kok().querySelector('#sube-ad')).not.toBeNull();

    yaz('#sube-ad', 'Beşiktaş');
    yaz('#sube-adres', 'Barbaros Blv.');
    formuGonder();

    expect(subeService.subeOlustur).toHaveBeenCalledWith({ name: 'Beşiktaş', city: 'İstanbul', address: 'Barbaros Blv.' });
    expect(dugme('Kadıköy şubesini düzenle').disabled).toBeTrue();
  });
});
