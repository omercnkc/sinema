import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { sessizBaglam } from '../../../core/hata/hata.interceptor';
import { SeansOzeti, Sube } from '../models/seans.models';

const KATALOG_ADRESI = `${environment.apiTabanAdresi}/catalog`;

/** Film detayındaki seans seçimi (halka açık). Hata sayfada "tekrar dene" ile gösterilir; istekler sessiz. */
@Injectable({ providedIn: 'root' })
export class SeansService {
  constructor(private readonly http: HttpClient) {}

  /** `gun`: İstanbul günü (`YYYY-MM-DD`); `subeId` boşsa tüm şubeler. */
  filminSeanslari(filmId: number, gun: string, subeId: number | null): Observable<SeansOzeti[]> {
    let parametreler = new HttpParams().set('date', gun);
    if (subeId !== null) {
      parametreler = parametreler.set('cinemaId', subeId);
    }
    return this.http.get<SeansOzeti[]>(`${KATALOG_ADRESI}/movies/${filmId}/showtimes`,
      { params: parametreler, context: sessizBaglam() });
  }

  /** Şube filtresi seçenekleri; alınamazsa filtre gizlenir, seanslar yine listelenir. */
  subeler(): Observable<readonly Sube[]> {
    return this.http.get<Sube[]>(`${KATALOG_ADRESI}/cinemas`, { context: sessizBaglam() }).pipe(catchError(() => of([])));
  }
}
