import { APP_INITIALIZER, NgModule, Optional, SkipSelf } from '@angular/core';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HataInterceptor } from './hata/hata.interceptor';
import { Kullanici } from './oturum/oturum.models';
import { OturumInterceptor } from './oturum/oturum.interceptor';
import { OturumService } from './oturum/oturum.service';

/** Uygulama açılmadan önce önceki oturum (refresh cookie'si) geri kurulur; rotalar ve guard'lar hazır durumu görür. */
export function oturumBaslaticisi(oturumService: OturumService): () => Observable<Kullanici | null> {
  return () => oturumService.oturumuGeriYukle();
}

/**
 * Uygulama genelinde tek örnek olan servisler, interceptor'lar ve guard'lar (rules §6.3). Yalnızca AppModule içe
 * aktarır: ikinci kez içe aktarılırsa (ör. lazy bir modülde) servislerin ikinci örneği oluşur ve oturum durumu
 * bölünür; bunu açılışta hata vererek engelleriz.
 */
@NgModule({
  imports: [HttpClientModule],
  providers: [
    // Sıra önemli: cevap interceptor'lardan ters sırayla geçer. HataInterceptor ilk kayıtlı olduğu için cevabı en
    // son görür; OturumInterceptor 401'i ondan önce ham haliyle yakalayıp oturumu yenileyebilir.
    { provide: HTTP_INTERCEPTORS, useClass: HataInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: OturumInterceptor, multi: true },
    { provide: APP_INITIALIZER, useFactory: oturumBaslaticisi, deps: [OturumService], multi: true },
  ],
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() mevcut: CoreModule | null) {
    if (mevcut) {
      throw new Error('CoreModule yalnızca AppModule tarafından bir kez içe aktarılmalıdır.');
    }
  }
}
