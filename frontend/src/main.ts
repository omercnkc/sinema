import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(AppModule)
  // Uygulama hiç açılamadıysa hatayı bildirecek başka bir yer yok (bildirim servisi de henüz yok)
  // eslint-disable-next-line no-console
  .catch((hata: unknown) => console.error(hata));
