# sinema-frontend

Angular 13 SPA (NgModule, strict, SCSS, Bootstrap 5.1 + ng-bootstrap 12). Kurulum, çalıştırma ve mimari için kök
[README](../README.md) ve [planlama](../planlama.md) §8.

```bash
nvm use            # .nvmrc: Node 14.18.1 (Angular 13: ^14.15.0)
npm ci
npm start          # http://localhost:4200, API: http://localhost:8080 (gateway)
npm run test:ci    # Karma, ChromeHeadless, tek seferlik
npm run build      # üretim derlemesi: dist/sinema-frontend
```
