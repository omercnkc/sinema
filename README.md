# Sinema – Koltuk Seçimi ve Rezervasyon Sistemi

İstanbul'daki 3 şube (10 salon) için film, seans ve koltuk rezervasyonu yapan, microservice mimarili bir SPA.
Misafirler film, seans ve koltuk doluluğunu görür; üyeler en fazla 5 koltuk tutup iyzico (sandbox) ile satın alır;
adminler film (TMDB), salon düzeni, seans ve fiyatları yönetir.

> **Durum:** Geliştirme aşamasında – M0 (iskelet). Ayrıntılı plan: [planlama.md](planlama.md), veritabanı: [db-planlama.md](db-planlama.md).

## Mimari

```
Angular 13 SPA ──> api-gateway (CORS, JWT ön doğrulama, Redis rate limit)
                     ├─> auth-service     (auth_db)
                     ├─> catalog-service  (catalog_db) ──> TMDB
                     ├─> booking-service  (booking_db) ──Feign──> catalog
                     └─> payment-service  (payment_db) ──Feign──> booking, iyzico
MySQL 8.4: tek instance, servis başına ayrı şema ve kullanıcı
```

- **Aynı koltuk iki kişiye satılamaz:** garanti veritabanında, `UNIQUE(showtime_id, layout_cell_id)` kısıtıyla.
- Koltuk tutma (hold) 3 dakika, sunucu saatine göre; süresi dolan koltuk kendiliğinden serbest kalır.
- Servisler arası çağrılar tek yönlü: `payment → booking → catalog`.

## Teknolojiler

| Katman | Teknoloji |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4 (Gateway, OpenFeign), Spring Security + JWT (RS256), JPA/Hibernate, Flyway |
| Veri | MySQL 8.4, Redis (yalnızca gateway rate limit) |
| Frontend | Angular 13, RxJS, SCSS, Bootstrap + ng-bootstrap |
| Altyapı | Maven (wrapper), Docker / Docker Compose |

## Repo yapısı

```
sinema/
 ├── pom.xml            # parent POM (sürümler tek yerde)
 ├── sinema-common/     # servlet servislerin ortak altyapısı (korelasyon kimliği vb.)
 ├── sinema-test-destegi/ # entegrasyon testleri için ortak kurulum (Testcontainers MySQL)
 ├── services/          # api-gateway, auth-service, catalog-service, booking-service, payment-service
 ├── frontend/          # Angular 13 (M5)
 ├── planlama.md, db-planlama.md, memory-bank/
```

## Gereksinimler

- JDK 21
- Docker Desktop (Compose v2)
- Maven kurulumu **gerekmez**: proje Maven Wrapper ile gelir.
- Frontend için (M5'ten itibaren): nvm + Node 16

## Derleme ve test

```powershell
.\mvnw.cmd verify      # Windows
./mvnw verify          # Linux / macOS
```

İlk çalıştırmada wrapper Maven 3.9.16'yı ve bağımlılıkları indirir.
Entegrasyon testleri Testcontainers ile gerçek bir MySQL başlatır, bu yüzden **Docker'ın çalışıyor olması gerekir**.

Veritabanı şeması yalnızca Flyway ile değişir: her servisin `src/main/resources/db/migration` klasöründeki
`V<sürüm>__<açıklama>.sql` dosyaları açılışta sırayla uygulanır. Uygulanmış bir migration dosyası değiştirilmez,
değişiklik için yeni sürüm eklenir.

## Çalıştırma

Tek bir servisi yerelde çalıştırmak (örnek):

```powershell
.\mvnw.cmd -pl services/catalog-service -am spring-boot:run
```

| Servis | Port | Sağlık kontrolü |
|---|---|---|
| api-gateway | 8080 | http://localhost:8080/actuator/health |
| auth-service | 8081 | http://localhost:8081/actuator/health |
| catalog-service | 8082 | http://localhost:8082/actuator/health |
| booking-service | 8083 | http://localhost:8083/actuator/health |
| payment-service | 8084 | http://localhost:8084/actuator/health |

### Docker ile tüm yığın

```powershell
copy .env.example .env      # sonra .env içindeki parolaları doldurun
bash ./scripts/jwt-anahtar-uret.sh   # JWT anahtar çifti (Windows'ta Git Bash'in bash'i)
docker compose up -d --build --wait
docker compose ps           # hepsi (healthy) olmalı
```

- Dışarıya açık tek API portu gateway'dir: `http://localhost:8080` (yalnızca bu bilgisayardan).
- Servis portları dışarı açık değildir; container'lar yalnızca iç ağda (`sinema-ag`) birbirini görür.
- Redis yalnızca gateway rate limit sayaçlarını tutar, kalıcı veri yoktur. Redis düşerse gateway çalışmaya devam eder:
  `/actuator/health` DOWN olur ama container sağlığına bakılan `/actuator/health/liveness` UP kalır.
- MySQL yalnızca `127.0.0.1:${MYSQL_HOST_PORT}` üzerinden erişilebilir (IDE'de çalışan servisler için).
- İlk açılışta `docker/mysql/init` betiği 4 şemayı ve her servis için yalnızca kendi şemasına yetkili kullanıcıyı oluşturur.
  Betik yalnızca veri klasörü boşken çalışır; sıfırlamak için `docker compose down -v`.

### Ortam değişkenleri

| Değişken | Açıklama |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root parolası |
| `AUTH_DB_PASSWORD`, `CATALOG_DB_PASSWORD`, `BOOKING_DB_PASSWORD`, `PAYMENT_DB_PASSWORD` | Servis başına DB kullanıcısı parolası (tek tırnak içermemeli) |
| `MYSQL_HOST_PORT` | MySQL'in bilgisayarda açılacağı port (varsayılan 3306) |
| `REDIS_PASSWORD` | Redis parolası (gateway rate limit) |
| `GATEWAY_HOST_PORT` | Gateway'in bilgisayarda açılacağı port (varsayılan 8080) |
| `JWT_KEYS_DIR` | JWT anahtar klasörü (varsayılan `./keys`, betikle üretilir) — M1 |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Sistemde admin yokken açılışta oluşturulan ilk admin — M1 |
| `BOOKING_SERVICE_CLIENT_SECRET`, `PAYMENT_SERVICE_CLIENT_SECRET` | Servisler arası çağrılar için servis JWT'si alma sırrı — M1 |
| `TMDB_API_TOKEN` | TMDB API Read Access Token — M2 |
| `IYZICO_API_KEY`, `IYZICO_SECRET_KEY`, `IYZICO_BASE_URL` | iyzico sandbox bilgileri — M4 |

Milestone etiketli değişkenler o adımın kodu gelince kullanılmaya başlar; şimdilik boş bırakılabilir.

**JWT anahtarları:** `keys/jwt-private.pem` token imzalar ve yalnızca auth-service'e verilir; `keys/jwt-public.pem`
token doğrular ve diğer servislere verilir. `keys/` repoya girmez. Anahtarı `--zorla` ile yeniden üretmek, verilmiş
tüm token'ları geçersiz kılar (herkes yeniden giriş yapar).

## Lisans / atıf

Ticari olmayan bir öğrenme projesidir. Film verileri [TMDB](https://www.themoviedb.org/) API'sinden alınır;
bu ürün TMDB API'sini kullanır ancak TMDB tarafından onaylanmamış veya sertifikalandırılmamıştır.
