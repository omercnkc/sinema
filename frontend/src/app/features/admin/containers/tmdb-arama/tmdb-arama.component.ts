import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { YuklemeDurumu, yuklemeDurumu } from '../../../../shared/yukleme/yukleme-durumu';
import { TmdbAdayi, TmdbAramaSonucu } from '../../models/film-formu.models';
import { TmdbService } from '../../services/tmdb.service';

/** TMDB kotası ve hız sınırı için her tuşta değil, yazma durunca aranır. */
const ARAMA_BEKLEMESI_MS = 400;
const EN_AZ_KARAKTER = 2;
/** Sunucunun sorgu sınırı (TmdbYonetimKontrolcusu @Size(max = 100)). */
const SORGU_EN_FAZLA = 100;

/**
 * TMDB'de film arama (yeni film sayfasında). Yalnızca ilk sonuç sayfası gösterilir; aranan film yoksa sorgu
 * daraltılır. Seçilen adayın TMDB kimliği `secildi` ile verilir, önizlemeyi kapsayıcı yükler. Zaten ekli filmde
 * seçim yerine düzenleme bağlantısı çıkar (aynı film iki kez eklenmesin).
 */
@Component({
  selector: 'app-tmdb-arama',
  templateUrl: './tmdb-arama.component.html',
  styleUrls: ['./tmdb-arama.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TmdbAramaComponent {
  /** Önizleme yüklenirken seçim düğmeleri kapalıdır. */
  @Input() kilitli = false;
  @Output() readonly secildi = new EventEmitter<number>();

  readonly enFazlaKarakter = SORGU_EN_FAZLA;
  readonly aramaKontrolu = new FormControl('');
  /** Sorgu kısaysa null (liste ve mesaj çizilmez). */
  readonly sonuc$: Observable<YuklemeDurumu<TmdbAramaSonucu> | null>;

  constructor(tmdbService: TmdbService) {
    this.sonuc$ = this.aramaKontrolu.valueChanges.pipe(
      map((deger: unknown) => String(deger ?? '').trim()),
      debounceTime(ARAMA_BEKLEMESI_MS),
      distinctUntilChanged(),
      switchMap((sorgu) => sorgu.length < EN_AZ_KARAKTER ? of(null) : yuklemeDurumu(tmdbService.ara(sorgu))),
    );
  }

  adayIzle(_: number, aday: TmdbAdayi): number {
    return aday.tmdbId;
  }
}
