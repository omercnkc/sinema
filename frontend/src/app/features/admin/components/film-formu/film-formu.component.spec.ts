import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { FilmFormuBaslangici, FilmIstegi } from '../../models/film-formu.models';
import { FilmFormuComponent } from './film-formu.component';

describe('FilmFormuComponent', () => {
  const ONIZLEME: FilmFormuBaslangici = {
    tmdbId: 872585, title: 'Oppenheimer', overview: 'Atom bombası.', posterPath: '/abc.jpg', runtimeMinutes: 181,
    releaseDate: '2023-07-21', certification: '13+', voteAverage: 8.1, director: 'Christopher Nolan',
    genres: ['Dram', 'Tarih'], cast: ['Cillian Murphy', 'Emily Blunt'], contentDescriptors: ['CINSELLIK'],
  };
  let fixture: ComponentFixture<FilmFormuComponent>;
  let bilesen: FilmFormuComponent;
  let gonderilen: FilmIstegi | null;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [SharedModule], declarations: [FilmFormuComponent] });
    fixture = TestBed.createComponent(FilmFormuComponent);
    bilesen = fixture.componentInstance;
    gonderilen = null;
    bilesen.kaydet.subscribe((istek: FilmIstegi) => (gonderilen = istek));
    fixture.detectChanges();
  });

  /** Bileşen OnPush: girişler testte doğrudan atanınca görünüm ancak işaretlenince yenilenir. */
  function yenile(): void {
    fixture.debugElement.injector.get(ChangeDetectorRef).markForCheck();
    fixture.detectChanges();
  }

  function deger(secici: string): string {
    return (fixture.nativeElement.querySelector(secici) as HTMLInputElement).value;
  }

  function hataMetni(alan: string): string {
    return fixture.nativeElement.querySelector(`#film-${alan}-hata`).textContent.trim();
  }

  function gonder(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    yenile();
  }

  it('should fill form from TMDB preview and send it back unchanged', () => {
    bilesen.baslangic = ONIZLEME;
    yenile();

    expect(deger('#film-turler')).toBe('Dram, Tarih');
    expect(deger('#film-oyuncular')).toBe('Cillian Murphy\nEmily Blunt');
    expect((fixture.nativeElement.querySelector('#film-isaret-CINSELLIK') as HTMLInputElement).checked).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-test="tmdb-kaynagi"]').textContent).toContain('#872585');
    expect(fixture.nativeElement.querySelector('app-film-afisi img').getAttribute('src')).toContain('/w342/abc.jpg');

    gonder();

    expect(gonderilen).toEqual({ ...ONIZLEME, runtimeMinutes: 181 });
  });

  it('should convert edited values: comma score, lists, empty fields as null', () => {
    bilesen.baslangic = ONIZLEME;
    bilesen.form.patchValue({
      overview: '  ', posterPath: '', releaseDate: '', certification: '', voteAverage: '7,5', director: '',
      genres: 'Dram, , Dram, Gerilim ', cast: ' Ayşe \n\n Mehmet ',
      contentDescriptors: { CINSELLIK: false, SIDDET_KORKU: true },
    });

    gonder();

    expect(gonderilen).toEqual({
      tmdbId: 872585, title: 'Oppenheimer', overview: null, posterPath: null, runtimeMinutes: 181, releaseDate: null,
      certification: null, voteAverage: 7.5, director: null, genres: ['Dram', 'Gerilim'], cast: ['Ayşe', 'Mehmet'],
      contentDescriptors: ['SIDDET_KORKU'],
    });
  });

  it('should validate on client before sending', () => {
    bilesen.form.patchValue({ title: '', runtimeMinutes: 601, posterPath: 'abc.jpg', voteAverage: '10,5',
      genres: Array.from({ length: 11 }, (_, i) => `Tür ${i}`).join(',') });

    gonder();

    expect(gonderilen).toBeNull();
    expect(hataMetni('baslik')).toBe('Başlık zorunludur.');
    expect(hataMetni('sure')).toBe('Süre en çok 600 olabilir.');
    expect(hataMetni('afis')).toBe('Afiş yolu biçimi geçersiz.');
    expect(hataMetni('puan')).toBe('Puan biçimi geçersiz.');
    expect(hataMetni('turler')).toContain('en çok 10 öğe');
    expect(fixture.nativeElement.querySelector('app-film-afisi img')).toBeNull();
  });

  it('should send manual film without TMDB id and show server field errors', () => {
    bilesen.form.patchValue({ title: 'Kısa Film', runtimeMinutes: 15 });
    gonder();
    expect(gonderilen?.tmdbId).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-test="tmdb-kaynagi"]')).toBeNull();

    bilesen.hata = new UygulamaHatasi(400, 'VALIDATION_FAILED', 'Formu düzeltin.', [
      { field: 'certification', message: 'Geçerli bir TR yaş sınırı olmalı' },
      { field: 'title', message: 'Başlık çok uzun.' },
    ]);
    yenile();

    expect(hataMetni('baslik')).toBe('Başlık çok uzun.');
  });
});
