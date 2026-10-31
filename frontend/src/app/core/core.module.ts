import { NgModule, Optional, SkipSelf } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

/**
 * Uygulama genelinde tek örnek olan servisler, interceptor'lar ve guard'lar (rules §6.3). Yalnızca AppModule içe
 * aktarır: ikinci kez içe aktarılırsa (ör. lazy bir modülde) servislerin ikinci örneği oluşur ve oturum durumu
 * bölünür; bunu açılışta hata vererek engelleriz.
 */
@NgModule({
  imports: [HttpClientModule],
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() mevcut: CoreModule | null) {
    if (mevcut) {
      throw new Error('CoreModule yalnızca AppModule tarafından bir kez içe aktarılmalıdır.');
    }
  }
}
