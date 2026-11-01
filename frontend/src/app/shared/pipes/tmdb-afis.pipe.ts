import { Pipe, PipeTransform } from '@angular/core';
import { environment } from '../../../environments/environment';

/** TMDB'nin sunduğu görsel genişlikleri (kullandıklarımız): admin arama listesi w92, kart w342, detay w500. */
export type TmdbAfisBoyutu = 'w92' | 'w342' | 'w500';

/**
 * Saklanan TMDB görsel yolundan ("/abc.jpg") tam afiş adresi kurar (K41 hotlink; taban adres environment'ta).
 * Afiş yoksa null döner; şablon yer tutucu gösterir.
 */
@Pipe({ name: 'tmdbAfis' })
export class TmdbAfisPipe implements PipeTransform {
  transform(afisYolu: string | null, boyut: TmdbAfisBoyutu): string | null {
    return afisYolu ? `${environment.tmdbGorselTabanAdresi}/${boyut}${afisYolu}` : null;
  }
}
