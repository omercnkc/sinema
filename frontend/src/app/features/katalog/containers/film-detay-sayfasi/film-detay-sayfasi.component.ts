import { ChangeDetectionStrategy, Component } from '@angular/core';
import { HttpStatusCode } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, throwError } from 'rxjs';
import { distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { AKILLI_ISARET_ADLARI, AkilliIsaret, FilmDetayi } from '../../models/film.models';
import { FilmService } from '../../services/film.service';

const FILM_YOK = new UygulamaHatasi(HttpStatusCode.NotFound, 'MOVIE_NOT_FOUND', 'Film bulunamadı.');

/**
 * /filmler/:id (smart): filmin tüm bilgileri (K16). Puan, film TMDB'den geldiyse "TMDB puanı" olarak etiketlenir
 * (atıf şartı, §9.3-2). Bulunamayan ya da gizlenmiş film için "tekrar dene" gösterilmez. Altında seans seçimi.
 */
@Component({
  selector: 'app-film-detay-sayfasi',
  templateUrl: './film-detay-sayfasi.component.html',
  styleUrls: ['./film-detay-sayfasi.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilmDetaySayfasiComponent {
  readonly isaretAdlari = AKILLI_ISARET_ADLARI;
  readonly durum$: Observable<YuklemeDurumu<FilmDetayi>>;

  private readonly tekrarDene$ = new BehaviorSubject<void>(undefined);

  constructor(filmService: FilmService, rota: ActivatedRoute) {
    const filmId$ = rota.paramMap.pipe(map((parametreler) => Number(parametreler.get('id'))), distinctUntilChanged());
    this.durum$ = combineLatest([filmId$, this.tekrarDene$]).pipe(
      // Sayı olmayan adres (/filmler/abc) sunucuya gitmeden "bulunamadı"
      switchMap(([filmId]) => yuklemeDurumu(
        Number.isInteger(filmId) && filmId > 0 ? filmService.getir(filmId) : throwError(() => FILM_YOK),
      )),
    );
  }

  tekrarDene(): void {
    this.tekrarDene$.next();
  }

  isaretIzle(_: number, isaret: AkilliIsaret): AkilliIsaret {
    return isaret;
  }

  /** 404 (yok/gizli) ve 400 (geçersiz kimlik) tekrar denemekle düzelmez. */
  tekrarDenenebilir(hata: UygulamaHatasi): boolean {
    return hata.durum !== HttpStatusCode.NotFound && hata.durum !== HttpStatusCode.BadRequest;
  }
}
