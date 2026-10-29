# Sinema – Koltuk Seçimi ve Rezervasyon Sistemi

İstanbul'daki 3 şube (10 salon) için film, seans ve koltuk rezervasyonu yapan, microservice mimarili bir SPA.
Misafirler film, seans ve koltuk doluluğunu görür; üyeler en fazla 5 koltuk tutup iyzico (sandbox) ile satın alır;
adminler film (TMDB), salon düzeni, seans ve fiyatları yönetir.

> **Durum:** M0 (iskelet) tamamlandı. Servisler ayağa kalkıyor, veritabanına bağlanıyor ve sağlık kontrolünden geçiyor;
> henüz iş özelliği yok. Ayrıntılı plan: [planlama.md](planlama.md), veritabanı: [db-planlama.md](db-planlama.md).

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
| Test | JUnit 5, AssertJ, Testcontainers (gerçek MySQL) |

## Repo yapısı

```
sinema/
 ├── pom.xml                 # parent POM: tüm sürümler tek yerde
 ├── sinema-common/          # servlet servislerin ortak altyapısı (korelasyon kimliği filtresi)
 ├── sinema-test-destegi/    # entegrasyon testleri için ortak kurulum (Testcontainers MySQL)
 ├── services/
 │    ├── api-gateway/       # reaktif, tek giriş noktası
 │    └── auth-service/  catalog-service/  booking-service/  payment-service/
 ├── docker/mysql/init/      # ilk açılışta şema + kullanıcı oluşturan betik
 ├── scripts/                # yardımcı betikler (JWT anahtar üretimi)
 ├── Dockerfile              # tüm Java servisleri için tek, multi-stage
 ├── docker-compose.yml
 ├── .env.example            # gerekli ortam değişkenleri (değersiz)
 └── planlama.md, db-planlama.md, memory-bank/
```

## Gereksinimler

| Araç | Not |
|---|---|
| JDK 21 | `java -version` ile kontrol edin |
| Docker Desktop (Compose v2) | Çalışıyor olmalı; testler de Docker kullanır |
| Git Bash (Windows) | JWT anahtar betiği için (`openssl` içerir) |
| Maven | **Gerekmez**, proje Maven Wrapper ile gelir |
| nvm + Node 16 | Yalnızca frontend için (M5'ten itibaren) |

## Hızlı başlangıç (Docker)

```powershell
git clone https://github.com/omercnkc/sinema.git
cd sinema
copy .env.example .env               # .env içindeki boş parolaları doldurun (openssl rand -base64 24)
bash ./scripts/jwt-anahtar-uret.sh   # keys/ altında JWT anahtar çifti
docker compose up -d --build --wait  # ilk build birkaç dakika sürer
docker compose ps                    # hepsi (healthy) olmalı
curl http://localhost:8080/actuator/health
```

Durdurmak: `docker compose down`. Veritabanını da silip sıfırdan başlamak: `docker compose down -v`.

**Ağ ve portlar**

| Bileşen | Bilgisayardan erişim | Not |
|---|---|---|
| api-gateway | `http://localhost:8080` | Tek API giriş noktası, yalnızca bu bilgisayardan |
| MySQL | `127.0.0.1:3306` | IDE'de çalışan servisler ve DB araçları için |
| auth / catalog / booking / payment | yok | Yalnızca iç ağda (`sinema-ag`), Docker DNS adıyla |
| Redis | yok | Yalnızca gateway kullanır, kalıcı veri tutmaz |

- İlk açılışta `docker/mysql/init` betiği 4 şemayı (`auth_db`, `catalog_db`, `booking_db`, `payment_db`) ve her biri
  yalnızca kendi şemasına yetkili 4 kullanıcıyı oluşturur.
- Her servis açılışta kendi Flyway migration'larını uygular.
- Redis düşerse gateway çalışmaya devam eder: `/actuator/health` DOWN olur, container sağlığına bakılan
  `/actuator/health/liveness` UP kalır.

## Yerel geliştirme (tek servis IDE'de veya Maven ile)

MySQL'i Docker'dan, geliştirdiğiniz servisi doğrudan çalıştırın:

```powershell
docker compose up -d mysql
.\mvnw.cmd install -DskipTests          # sinema-common'ı yerel Maven deposuna kurar (bir kez, değişince tekrar)
$env:DB_PASSWORD = "<.env içindeki CATALOG_DB_PASSWORD>"
.\mvnw.cmd -pl services/catalog-service spring-boot:run
```

IDE'de çalıştırırken aynı `DB_PASSWORD` değişkenini çalıştırma yapılandırmasına ekleyin.
Docker'daki aynı servis de çalışıyorsa çakışma olmaz (container portları dışarı açık değildir).

| Servis | Yerel port | Ana sınıf | DB kullanıcısı / parola değişkeni |
|---|---|---|---|
| api-gateway | 8080 | `ApiGatewayUygulamasi` | — (Redis: `REDIS_PASSWORD`) |
| auth-service | 8081 | `KimlikServisiUygulamasi` | `auth_user` / `AUTH_DB_PASSWORD` |
| catalog-service | 8082 | `KatalogServisiUygulamasi` | `catalog_user` / `CATALOG_DB_PASSWORD` |
| booking-service | 8083 | `RezervasyonServisiUygulamasi` | `booking_user` / `BOOKING_DB_PASSWORD` |
| payment-service | 8084 | `OdemeServisiUygulamasi` | `payment_user` / `PAYMENT_DB_PASSWORD` |

Port `SERVER_PORT`, DB adresi `DB_HOST` / `DB_PORT` ile değiştirilebilir. Docker'daki gateway 8080'i kullanıyorsa
yerel gateway'i `SERVER_PORT` ile başka porta alın.

## Derleme ve test

```powershell
.\mvnw.cmd verify      # Windows
./mvnw verify          # Linux / macOS
```

- İlk çalıştırmada wrapper Maven 3.9.16'yı ve bağımlılıkları indirir.
- Entegrasyon testleri Testcontainers ile gerçek bir MySQL başlatır: **Docker çalışıyor olmalı**.
- Her servlet servisi `ServisAltyapiTesti`'ni miras alır: sağlık kontrolü, korelasyon kimliği ve Flyway testi.

**Veritabanı değişiklikleri** yalnızca Flyway ile yapılır: servisin `src/main/resources/db/migration` klasörüne
`V<sürüm>__<açıklama>.sql` eklenir. Uygulanmış bir migration dosyası değiştirilmez; değişiklik için yeni sürüm eklenir.
Hibernate şemayı değiştirmez (`ddl-auto=validate`), yalnızca entity'lerle uyumunu kontrol eder.

## Ortam değişkenleri

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
Eksik zorunlu bir değişkende `docker compose` hangi değişkenin eksik olduğunu söyleyerek başlamaz.

**JWT anahtarları:** `keys/jwt-private.pem` token imzalar ve yalnızca auth-service'e verilir; `keys/jwt-public.pem`
token doğrular ve diğer servislere verilir. `keys/` repoya girmez. Anahtarı `--zorla` ile yeniden üretmek, verilmiş
tüm token'ları geçersiz kılar (herkes yeniden giriş yapar).

## Sık karşılaşılan sorunlar

| Belirti | Sebep / çözüm |
|---|---|
| `failed to connect to the docker API` | Docker Desktop açık değil. Açın, `docker info` çalışana kadar bekleyin. |
| `MYSQL_ROOT_PASSWORD .env içinde tanımlı olmalı` | `.env` yok veya değer boş. `.env.example`'dan kopyalayıp doldurun. |
| `port is already allocated` (3306 / 8080) | Bilgisayarda başka MySQL veya uygulama o portu kullanıyor. `.env`'de `MYSQL_HOST_PORT` / `GATEWAY_HOST_PORT` değiştirin. |
| `Access denied for user 'xxx_user'` | `.env`'deki parola değişti ama MySQL eski parolayla kurulmuştu. Init betiği yalnızca ilk açılışta çalışır: `docker compose down -v` ile sıfırlayın. |
| Servis `unhealthy`, logda `Validate failed: Migrations have failed validation` | Uygulanmış bir migration dosyası değiştirilmiş. Dosyayı geri alın, değişikliği yeni `V<n>__` dosyasıyla yapın. |
| Yerel çalıştırmada `Could not resolve placeholder 'DB_PASSWORD'` | `DB_PASSWORD` ortam değişkeni verilmemiş (bkz. Yerel geliştirme). |
| Yerel çalıştırmada `sinema-common` bulunamıyor | Önce kökte `.\mvnw.cmd install -DskipTests` çalıştırın. |
| `mvnw verify` testlerde `Could not find a valid Docker environment` | Testcontainers Docker'a ulaşamıyor; Docker Desktop'ı başlatın. |
| `./mvnw: bad interpreter` (Linux container / WSL) | Satır sonları CRLF olmuş. `.gitattributes` bunu önler; `git add --renormalize .` ile düzeltin. |

## Yol haritası

| Milestone | İçerik | Durum |
|---|---|---|
| M0 | İskelet: servisler, Docker, MySQL, Redis, Flyway, testler | Tamamlandı |
| M1 | Auth + gateway güvenliği (JWT, refresh, rate limit, CORS) | Sırada |
| M2 | Catalog: şube, salon, düzen, film (TMDB), seans | |
| M3 | Booking: koltuk tutma (çift satış yok), rezervasyon | |
| M4 | Payment (iyzico sandbox) + bilet doğrulama | |
| M5–M7 | Angular frontend, koltuk haritası, admin paneli | |
| M8 | Salon düzeni değişikliğinde bilet göçü | |
| M9 | Sertleştirme, güvenlik taraması, demo | |

## Lisans / atıf

Ticari olmayan bir öğrenme projesidir. Film verileri [TMDB](https://www.themoviedb.org/) API'sinden alınır;
bu ürün TMDB API'sini kullanır ancak TMDB tarafından onaylanmamış veya sertifikalandırılmamıştır.
