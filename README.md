# Sinema – Koltuk Seçimi ve Rezervasyon Sistemi

İstanbul'daki 3 şube (10 salon) için film, seans ve koltuk rezervasyonu yapan, microservice mimarili bir SPA.
Misafirler film, seans ve koltuk doluluğunu görür; üyeler en fazla 5 koltuk tutup iyzico (sandbox) ile satın alır;
adminler film (TMDB), salon düzeni, seans ve fiyatları yönetir.

> **Durum:** M3 (rezervasyon) tamamlandı. Kimlik/gateway (M1) ve katalog (M2) üzerine koltuk doluluğu, koltuk tutma
> (çift satış yok), rezervasyonlarım, hold bırakma, terk koruması ve ödeme servisinin kullanacağı onay uçları
> çalışıyor; ödeme (iyzico) ve bilet doğrulama M4'te.
> Ayrıntılı plan: [planlama.md](planlama.md), veritabanı: [db-planlama.md](db-planlama.md).

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
| Test | JUnit 5, AssertJ, Mockito, Testcontainers (gerçek MySQL ve Redis) |

## Repo yapısı

```
sinema/
 ├── pom.xml                 # parent POM: tüm sürümler tek yerde
 ├── sinema-common/          # servlet servislerin ortak altyapısı: hata biçimi, korelasyon kimliği, JWT güvenlik zinciri
 ├── sinema-test-destegi/    # testler için ortak kurulum (Testcontainers MySQL, test JWT anahtarları ve token'ları)
 ├── services/
 │    ├── api-gateway/       # reaktif, tek giriş noktası
 │    └── auth-service/  catalog-service/  booking-service/  payment-service/
 ├── docker/mysql/init/      # ilk açılışta şema + kullanıcı oluşturan betik
 ├── scripts/                # JWT anahtar üretimi, duman testi
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
| Git Bash (Windows) | Betikler için (`openssl`, `curl` içerir) |
| Maven | **Gerekmez**, proje Maven Wrapper ile gelir |
| nvm + Node 16 | Yalnızca frontend için (M5'ten itibaren) |

## Hızlı başlangıç (Docker)

```powershell
git clone https://github.com/omercnkc/sinema.git
cd sinema
copy .env.example .env               # .env içindeki boş değerleri doldurun (openssl rand -base64 24)
bash ./scripts/jwt-anahtar-uret.sh   # keys/ altında JWT anahtar çifti
docker compose up -d --build --wait  # ilk build birkaç dakika sürer
docker compose ps                    # hepsi (healthy) olmalı
bash ./scripts/duman-testi.sh        # gateway üzerinden uçtan uca kontrol
```

İlk açılışta catalog `dev` profiliyle örnek veri yükler: 3 şube (Kadıköy 3, Beşiktaş 4, Bakırköy 3 salon), her salonda
farklı bir düzen, 5 uydurma film ve bugünden itibaren 7 günlük seans. Örnek seanslar biterse veya temiz bir katalogla
başlamak isterseniz `docker compose down -v` (boş katalog için `.env`'de `CATALOG_PROFILES=default`).

Durdurmak: `docker compose down`. Veritabanını da silip sıfırdan başlamak: `docker compose down -v`.

**Ağ ve portlar**

| Bileşen | Bilgisayardan erişim | Not |
|---|---|---|
| api-gateway | `http://localhost:8080` | Tek API giriş noktası, yalnızca bu bilgisayardan |
| MySQL | `127.0.0.1:3306` | IDE'de çalışan servisler ve DB araçları için |
| auth / catalog / booking / payment | yok | Yalnızca iç ağda (`sinema-ag`), Docker DNS adıyla |
| Redis | yok | Yalnızca gateway kullanır, kalıcı veri tutmaz |

- İlk açılışta `docker/mysql/init` betiği 4 şemayı ve her biri yalnızca kendi şemasına yetkili 4 kullanıcıyı oluşturur.
- Her servis açılışta kendi Flyway migration'larını uygular.
- `ADMIN_EMAIL` / `ADMIN_PASSWORD` doluysa ve sistemde hiç admin yoksa, auth-service açılışta ilk admini oluşturur.

## Güvenlik modeli

**Kimlik:** auth-service, RS256 ile imzalı kısa ömürlü bir **access token** (15 dk, JSON gövdede) ve opak bir
**refresh token** (14 gün, `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` cookie) verir. Private key yalnızca
auth-service'tedir; diğer servisler ve gateway yalnızca public key ile doğrular.

**İki katmanlı doğrulama:** gateway token'ı ve rota rolünü kontrol eder; her servis aynı token'ı ayrıca kendisi doğrular
(gateway atlanıp iç ağdan gelinse de servis korunur). Kurallar varsayılan olarak kapalıdır:

| Yol | Kim erişir |
|---|---|
| Servisin açık yolları (ör. `GET /api/v1/catalog/**`, giriş/kayıt) ve sağlık kontrolü | Herkes |
| `/api/v1/admin/**` | ADMIN |
| Diğer `/api/v1/**` | USER veya ADMIN |
| `/internal/**` | Yalnızca o servise kesilmiş SERVICE token'ı; gateway'den hiç erişilemez |

**Diğer korumalar:**
- Hesap kilidi: 5 hatalı giriş → 5 dk (429 + `Retry-After`).
- Refresh token rotasyonu: her yenilemede değişir; eski token tekrar kullanılırsa tüm oturum ailesi iptal edilir.
- Cookie'li uçlar (refresh, logout) yalnızca izinli `Origin`'den kabul edilir (CSRF savunması).
- IP başına istek sınırı (Redis): giriş 10/dk, kayıt 5/dk, bilet doğrulama 30/dk, genel 20/sn. Redis erişilemezse giriş ve
  kayıt reddedilir (503), diğer trafik geçer.
- Servisler birbirini `POST /internal/v1/auth/service-token` ile alınan 5 dk'lık token'larla çağırır; hangi servisin hangi
  servise token alabileceği sınırlıdır (booking → catalog, payment → booking).
- Tüm hata cevapları RFC 7807 `application/problem+json`: `code` (makine okunur), `detail`, `correlationId`, `timestamp`.

## API

Gateway üzerinden (`http://localhost:8080`). Para tutarları string (`"120.00"`), zamanlar UTC ISO-8601.

**Kimlik (M1)**

| Uç | Erişim | Açıklama |
|---|---|---|
| `POST /api/v1/auth/register` | Herkes | `{email, password}` → 201. Rol her zaman USER |
| `POST /api/v1/auth/login` | Herkes | `{email, password}` → `{accessToken, expiresInSeconds}` + refresh cookie |
| `POST /api/v1/auth/refresh` | Cookie + izinli Origin | Yeni access token + yeni refresh cookie |
| `POST /api/v1/auth/logout` | Cookie + izinli Origin | 204, oturum ailesi iptal, cookie silinir |
| `GET /api/v1/auth/me` | USER/ADMIN | `{id, email, role}` |
| `POST /api/v1/admin/auth/admins` | ADMIN | `{email, password}` → 201, yeni admin |

Parola kuralı: en az 8 karakter, en çok 72 bayt (BCrypt sınırı; Türkçe harfler 2 bayt sayılır).

**Katalog (M2), herkese açık**

| Uç | Açıklama |
|---|---|
| `GET /api/v1/catalog/cinemas` | Şubeler ve salonları (fiyat yok, fiyat seansta) |
| `GET /api/v1/catalog/movies?q&genre&page&size` | Görünür filmler, sayfalı (`size` ≤ 50); başlık araması harf/aksan duyarsız |
| `GET /api/v1/catalog/movies/{id}` | Film detayı (oyuncular, yönetmen, TR yaş sınırı, akıllı işaretler, TMDB puanı) |
| `GET /api/v1/catalog/movies/{id}/showtimes?date&cinemaId` | O günün seansları (`date` Europe/Istanbul günü, boşsa bugün) |
| `GET /api/v1/catalog/showtimes/{id}` | Seans: fiyat, `salesCloseAt`, `salesOpen`, `serverTime` |
| `GET /api/v1/catalog/showtimes/{id}/layout` | Seansın mühürlü koltuk haritası (`ETag` + 5 dk önbellek) |

**Katalog (M2), ADMIN** — hepsi `/api/v1/admin/catalog` altında

| Uç | Açıklama |
|---|---|
| `GET/POST /cinemas`, `PUT /cinemas/{id}` | Şube |
| `POST /halls`, `PUT /halls/{id}` | Salon ve varsayılan bilet fiyatı |
| `GET/POST /halls/{id}/layouts`, `GET/PUT /halls/{id}/layouts/{layoutId}` | Düzen taslağı (salon başına bir taslak; hatalarda 422 `violations`) |
| `POST /halls/{id}/layouts/{layoutId}/publish` | Taslağı sürüm yapar, salonun aktif düzeni olur; salonda süren seans varken 409 |
| `GET/POST /movies`, `GET/PUT /movies/{id}`, `PATCH /movies/{id}/visibility` | Film (silinmez, gizlenir) |
| `GET /tmdb/search?query`, `GET /tmdb/movies/{tmdbId}` | TMDB araması ve form ön doldurma (kaydetmez); TMDB yoksa 503 |
| `POST /showtimes`, `PUT /showtimes/{id}` | Seans (çakışma 409, fiyat boşsa salondan); silme/iptal yok |

**Kurallar:** seans bitişi = başlangıç + film süresi + 15 dk ara; satış başlangıçtan 10 dk sonra kapanır (seans bazında
değiştirilebilir); her seans oluşturulduğu andaki düzen sürümüne bağlı kalır, yeni düzen yalnızca sonraki seanslara
uygulanır. TMDB'den gelen film verisi 3 ayda bir (ayın 1'i) yenilenir; TMDB'de boş gelen alan admin değerini ezmez.

## Yerel geliştirme (tek servis IDE'de veya Maven ile)

MySQL'i Docker'dan, geliştirdiğiniz servisi doğrudan çalıştırın:

```powershell
docker compose up -d mysql
.\mvnw.cmd install -DskipTests          # ortak modülleri yerel Maven deposuna kurar (bir kez, değişince tekrar)
$env:DB_PASSWORD = "<.env içindeki CATALOG_DB_PASSWORD>"
.\mvnw.cmd -pl services/catalog-service spring-boot:run
```

booking-service ayrıca `BOOKING_SERVICE_CLIENT_SECRET` ister (auth'tan servis token'ı alıp catalog'u çağırır; auth ve
catalog varsayılan olarak `localhost:8081` / `localhost:8082`, değiştirmek için `AUTH_SERVICE_URL` / `CATALOG_SERVICE_URL`).

Servisler JWT anahtarlarını `./keys` altında arar (servis klasöründen çalıştırılıyorsa `JWT_KEYS_DIR` ile mutlak yol verin).
IDE'de çalıştırırken aynı değişkenleri çalıştırma yapılandırmasına ekleyin.

| Servis | Yerel port | Ana sınıf | DB kullanıcısı / parola değişkeni |
|---|---|---|---|
| api-gateway | 8080 | `ApiGatewayUygulamasi` | — (Redis: `REDIS_PASSWORD`) |
| auth-service | 8081 | `KimlikServisiUygulamasi` | `auth_user` / `AUTH_DB_PASSWORD` |
| catalog-service | 8082 | `KatalogServisiUygulamasi` | `catalog_user` / `CATALOG_DB_PASSWORD` |
| booking-service | 8083 | `RezervasyonServisiUygulamasi` | `booking_user` / `BOOKING_DB_PASSWORD` |
| payment-service | 8084 | `OdemeServisiUygulamasi` | `payment_user` / `PAYMENT_DB_PASSWORD` |

Port `SERVER_PORT`, DB adresi `DB_HOST` / `DB_PORT` ile değiştirilebilir.

**Rezervasyon (M3)** — giriş zorunlu, doluluk hariç

| Uç | Erişim | Açıklama |
|---|---|---|
| `GET /api/v1/bookings/showtimes/{id}/seat-status` | Herkes | Dolu koltuklar `{serverTime, occupied[{cellId, status}]}`; kimin tuttuğu yok, `no-store` |
| `POST /api/v1/bookings/reservations` | USER/ADMIN | `{showtimeId, seatCellIds[1..5]}` → 201, ödeme bekleyen rezervasyon + `expiresAt` |
| `GET /api/v1/bookings/reservations/{id}` | Sahibi | Durum, koltuklar, tutar, bilet bilgileri; onaylıysa `ticketCode` |
| `GET /api/v1/bookings/reservations?page&size` | USER/ADMIN | Kendi rezervasyonları, en yeni önce (`size` ≤ 50) |
| `DELETE /api/v1/bookings/reservations/{id}` | Sahibi | Ödeme başlamadan hold'u bırakır (204), koltuklar hemen boşalır |

Koltuk tutma hataları: `409 SEATS_UNAVAILABLE` (+`unavailableCellIds`), `409 ACTIVE_RESERVATION_EXISTS` (+`reservationId`),
`429 HOLD_COOLDOWN` (+`Retry-After`), `422 SALES_CLOSED`, `400 INVALID_SEATS` / `TOO_MANY_SEATS` / `DUPLICATE_SEATS`,
`503 CATALOG_UNAVAILABLE`. payment-service'in iç uçları (`/internal/v1/bookings/reservations/{id}/payment-info`,
`extend-payment-window`, `confirm`, `fail`) gateway'den erişilemez.

**Kurallar:** hold 3 dk (satış kapanışını aşmaz); bir işlemde en çok 5 koltuk; kullanıcının seans başına tek aktif işlemi
olur; ödeme başlayınca hold bir kez 2 dk uzar. Son 30 dk'da ödemeden bırakılan ya da süresi dolan 3 işlem varsa son terkten
10 dk sonrasına kadar yeni hold açılamaz. Satın alınan bilet iptal edilemez. Doluluk cevabı sunucuda en çok 2 sn
önbelleklenir; kendi tutma/bırakma işleminiz hemen görünür.

## Derleme ve test

```powershell
.\mvnw.cmd verify      # Windows
./mvnw verify          # Linux / macOS
```

- Entegrasyon testleri Testcontainers ile gerçek MySQL ve Redis başlatır: **Docker çalışıyor olmalı**.
- Testler JWT anahtarlarını çalışma anında kendileri üretir; `keys/` gerekmez.
- Her servlet servisi `ServisAltyapiTesti`'ni miras alır: sağlık kontrolü, Flyway ve 6 güvenlik senaryosu.
- Çalışan yığın için: `bash ./scripts/duman-testi.sh [gateway-adresi]`. `ADMIN_EMAIL`/`ADMIN_PASSWORD` verilirse admin
  katalog adımları da çalışır (veri oluşturmaz).

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
| `CORS_ALLOWED_ORIGINS` | Gateway'e tarayıcıdan istek atabilecek frontend adresleri, virgülle (varsayılan `http://localhost:4200`) |
| `JWT_KEYS_DIR` | JWT anahtar klasörü (varsayılan `./keys`, betikle üretilir) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Sistemde admin yokken açılışta oluşturulan ilk admin; boşsa oluşturulmaz |
| `BOOKING_SERVICE_CLIENT_SECRET`, `PAYMENT_SERVICE_CLIENT_SECRET` | Servis token'ı almak için sırlar; booking bu değer olmadan açılmaz |
| `CATALOG_PROFILES` | `dev` (varsayılan): katalog boşken örnek veri yüklenir; `default`: yüklenmez |
| `TMDB_API_TOKEN` | TMDB "API Read Access Token" (v4). Boşsa TMDB araması 503 verir, filmler elle girilir |
| `IYZICO_API_KEY`, `IYZICO_SECRET_KEY`, `IYZICO_BASE_URL` | iyzico sandbox bilgileri — M4 |

Eksik zorunlu bir değişkende `docker compose` hangi değişkenin eksik olduğunu söyleyerek başlamaz.

**JWT anahtarları:** `keys/jwt-private.pem` token imzalar ve yalnızca auth-service'e mount edilir; diğer servislere ve
gateway'e yalnızca `keys/jwt-public.pem` verilir. `keys/` repoya girmez. Anahtarı `--zorla` ile yeniden üretmek, verilmiş
tüm token'ları geçersiz kılar (herkes yeniden giriş yapar).

## Sık karşılaşılan sorunlar

| Belirti | Sebep / çözüm |
|---|---|
| `failed to connect to the docker API` | Docker Desktop açık değil. Açın, `docker info` çalışana kadar bekleyin. |
| `MYSQL_ROOT_PASSWORD .env içinde tanımlı olmalı` | `.env` yok veya değer boş. `.env.example`'dan kopyalayıp doldurun. |
| `port is already allocated` (3306 / 8080) | Başka bir uygulama o portu kullanıyor. `.env`'de `MYSQL_HOST_PORT` / `GATEWAY_HOST_PORT` değiştirin. |
| Servis açılmıyor, logda `JWT ... key okunamadı` | Anahtar çifti üretilmemiş: `bash ./scripts/jwt-anahtar-uret.sh`. |
| auth açılmıyor, `ADMIN_EMAIL / ADMIN_PASSWORD geçersiz` | İlk admin parolası kurala uymuyor (en az 8 karakter). `.env`'i düzeltin. |
| `Access denied for user 'xxx_user'` | `.env`'deki DB parolası değişti ama MySQL eski parolayla kurulmuştu. Init betiği yalnızca ilk açılışta çalışır: `docker compose down -v`. |
| Refresh/logout `403 ORIGIN_NOT_ALLOWED` | İstek izinli bir frontend'den gelmiyor; `CORS_ALLOWED_ORIGINS`'e frontend adresini ekleyin. |
| Giriş/kayıt `503 RATE_LIMIT_UNAVAILABLE` | Redis'e ulaşılamıyor (`docker compose ps redis`); bu iki uç Redis olmadan bilerek kapalıdır. |
| `429 RATE_LIMITED` | İstek sınırı aşıldı; `Retry-After` saniye bekleyin. Docker'da tüm yerel istekler aynı IP'den görünür. |
| Servis `unhealthy`, logda `Validate failed: Migrations have failed validation` | Uygulanmış bir migration dosyası değiştirilmiş. Dosyayı geri alın, değişikliği yeni `V<n>__` dosyasıyla yapın. |
| Yerel çalıştırmada `Could not resolve placeholder 'DB_PASSWORD'` | `DB_PASSWORD` ortam değişkeni verilmemiş (bkz. Yerel geliştirme). |
| `mvnw verify` testlerde `Could not find a valid Docker environment` | Testcontainers Docker'a ulaşamıyor; Docker Desktop'ı başlatın. |
| Admin TMDB araması `503 TMDB_UNAVAILABLE` | `TMDB_API_TOKEN` boş/yanlış veya TMDB'ye ulaşılamıyor. Film formu elle doldurulabilir. |
| Film sayfasında hiç seans yok | Örnek seanslar yüklendiği günden itibaren 7 gün içindir. `docker compose down -v` ile yeniden yükleyin. |
| Seans eklerken `409 SHOWTIME_OVERLAP` | Salonda bu aralıkta (15 dk ara dahil) başka seans var; cevaptaki `conflictingShowtimeId`'ye bakın. |
| Koltuk tutarken `503 CATALOG_UNAVAILABLE` | booking catalog'a ulaşamıyor ya da servis token'ı alamıyor: `docker compose ps catalog-service auth-service`, `.env`'de `BOOKING_SERVICE_CLIENT_SECRET` dolu mu, booking loglarına bakın. |
| booking açılmıyor, `sinema.servis-istemcisi.sir` boş | `.env`'de `BOOKING_SERVICE_CLIENT_SECRET` yok. Değeri verip `docker compose up -d booking-service`. |
| Koltuk tutarken `429 HOLD_COOLDOWN` | Kısa sürede 3 işlem ödemeden bırakıldı/süresi doldu (terk koruması); `Retry-After` saniye bekleyin. |
| Koltuk tutarken `409 ACTIVE_RESERVATION_EXISTS` | Bu seansta ödemesi bekleyen işleminiz var; cevaptaki `reservationId`'yi bırakın ya da süresinin dolmasını bekleyin. |
| `./mvnw: bad interpreter` (Linux container / WSL) | Satır sonları CRLF olmuş. `.gitattributes` bunu önler; `git add --renormalize .` ile düzeltin. |

## Yol haritası

| Milestone | İçerik | Durum |
|---|---|---|
| M0 | İskelet: servisler, Docker, MySQL, Redis, Flyway, testler | Tamamlandı |
| M1 | Auth + gateway güvenliği (JWT, refresh, hesap kilidi, admin, servis token'ı, rate limit, CORS) | Tamamlandı |
| M2 | Catalog: şube, salon, sürümlü düzen, film (TMDB), seans, örnek veri | Tamamlandı |
| M3 | Booking: koltuk tutma (çift satış yok), doluluk, rezervasyon, terk koruması, ödeme onay uçları | Tamamlandı |
| M4 | Payment (iyzico sandbox) + bilet doğrulama | Sırada |
| M5–M7 | Angular frontend, koltuk haritası, admin paneli | |
| M8 | Salon düzeni değişikliğinde bilet göçü | |
| M9 | Sertleştirme, güvenlik taraması, demo | |

## Lisans / atıf

Ticari olmayan bir öğrenme projesidir. Film verileri [TMDB](https://www.themoviedb.org/) API'sinden alınır;
bu ürün TMDB API'sini kullanır ancak TMDB tarafından onaylanmamış veya sertifikalandırılmamıştır.
