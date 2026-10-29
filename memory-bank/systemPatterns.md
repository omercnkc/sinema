# System Patterns

Ayrıntı ve gerekçeler: [../planlama.md](../planlama.md) §3 (TASLAK).

## Architecture
Monorepo, Angular SPA (ayrı origin, gateway'de CORS) → API Gateway (Redis rate limit) → 4 servis (auth, catalog, booking, payment). Tek MySQL instance, servis başına ayrı şema/kullanıcı. Servisler arası çağrılar Bearer servis JWT ile (K36). Servisler arası senkron iletişim Feign (tek yönlü: payment → booking → catalog).

## Key technical decisions (taslak)
- Koltuk benzersizliği DB kısıtı ile (UNIQUE showtime+seat), kod kontrolüne güvenilmez
- Bilet görüntü verisi Reservation'da snapshot
- K29 cooldown booking-service içinde iş kuralı
- Servis keşfi yok (Docker DNS), ortak kod: sinema-common (iş mantığı yok)

## Design patterns
Strategy/Adapter: PaymentProvider (iyzico + test mock). Repository, Mapper (MapStruct). Angular: Observable store, interceptor, guard.
