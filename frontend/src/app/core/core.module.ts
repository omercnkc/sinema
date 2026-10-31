import { NgModule, Optional, SkipSelf } from '@angular/core';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { HataInterceptor } from './hata/hata.interceptor';

/**
 * Uygulama genelinde tek örnek olan servisler, interceptor'lar ve guard'lar (rules §6.3). Yalnızca AppModule içe
 * aktarır: ikinci kez içe aktarılırsa (ör. lazy bir modülde) servislerin ikinci örneği oluşur ve oturum durumu
 * bölünür; bunu açılışta hata vererek engelleriz.
 */
@NgModule({
  imports: [HttpClientModule],
  // Sıra önemli: cevap interceptor'lardan ters sırayla geçer. HataInterceptor ilk kayıtlı olduğu için cevabı en son
  // görür; oturum yenileme (alt görev 5) 401'i ondan önce ham haliyle yakalayabilir.
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: HataInterceptor, multi: true }],
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() mevcut: CoreModule | null) {
    if (mevcut) {
      throw new Error('CoreModule yalnızca AppModule tarafından bir kez içe aktarılmalıdır.');
    }
  }
}
