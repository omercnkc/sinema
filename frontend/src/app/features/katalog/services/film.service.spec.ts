import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../../environments/environment';
import { SESSIZ_HATA } from '../../../core/hata/hata.interceptor';
import { FilmService } from './film.service';

describe('FilmService', () => {
  const ADRES = `${environment.apiTabanAdresi}/catalog/movies`;
  let servis: FilmService;
  let sunucu: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servis = TestBed.inject(FilmService);
    sunucu = TestBed.inject(HttpTestingController);
  });

  afterEach(() => sunucu.verify());

  it('should send only filled filters with page size and silence global errors', () => {
    servis.ara({ arama: '', tur: 'Dram', sayfa: 2 }).subscribe();

    const istek = sunucu.expectOne((i) => i.url === ADRES);
    expect(istek.request.params.keys().sort()).toEqual(['genre', 'page', 'size']);
    expect(istek.request.params.get('genre')).toBe('Dram');
    expect(istek.request.params.get('page')).toBe('2');
    expect(istek.request.context.get(SESSIZ_HATA)).toBeTrue();
  });

  it('should fall back to empty genre list when genres cannot be loaded', () => {
    let turler: readonly string[] | undefined;
    servis.turler().subscribe((liste) => (turler = liste));

    sunucu.expectOne(`${ADRES}/genres`).flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(turler).toEqual([]);
  });
});
