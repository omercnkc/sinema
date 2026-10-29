# Planlama – Sinema Koltuk Seçimi & Rezervasyon Sistemi

> Durum: PLANLAMA (kod yok). Kararlar bu dosyada toplanır; özet `memory-bank/`'e yansıtılır.

## Plan Adımları
1. [x] Kapsam ve gereksinimler
2. [x] Domain modeli
3. [x] Servis sınırları
4. [x] Concurrency stratejisi (K32–K35)
5. [x] Auth/JWT akışı (K36)
6. [x] Monorepo + Docker topolojisi (K37, K38)
7. [x] API sözleşmeleri (K40)
8. [x] Angular 13 planı (K41)
9. [x] Geliştirme sırası / milestone'lar (K42)

**Planlama tamam.** Kodlama, kullanıcı "kodlamaya geçelim" diyene kadar başlamaz.

---

## 1. Kapsam ve Gereksinimler

### 1.1 Kesinleşen kararlar
| # | Karar |
|---|---|
| K1 | Rezervasyon **film + saat (seans) + salon** üçlüsüne bağlıdır. |
| K2 | Örnek veri: **İstanbul'da 3 salon**. |
| K3 | **Misafir:** sadece görüntüleme. **Üye:** giriş yapınca satın alabilir. **Admin:** panel. |
| K4 | Admin: salona film/seans atama, blog yazısı, fiyat ve benzeri özellikleri değiştirme (yetkiler rol/authority bazlı tanımlanacak). |
| K5 | **Salon düzeni sabit değil, özelleştirilebilir** (her salonun giriş, çıkış, boşluk vb. farklıdır). |
| K6 | **Alınan koltuk iptal edilemez.** |
| K7 | Koltuk seçilince **geçici tutma (hold) süresi zorunlu**, süre dolunca koltuk kesin serbest kalır (saldırgan sabotajı önlemi). |
| K8 | Kullanıcı başına **en fazla 5 koltuk**. |
| K9 | **Aynı koltuk aynı anda iki kişiye satılamaz** (en kritik gereksinim, bkz. Adım 4). |
| K10 | **Satış kapanışı (GÜNCEL): seans başladıktan 10 dk sonra** (örn. 20:00 seansı için 20:10'a kadar satış). Bu önceki "Seçenek A"yı değil Seçenek C'yi (başlangıçtan sonra) karşılar. Değer `salesCloseOffsetMinutes` olarak işaretli tutulur (negatif = önce, pozitif = sonra), varsayılan +10, yapılandırılabilir. |
| K11 | **Hold süresi 3 dakika** (sunucu tarafı `expiresAt`; konfigürasyondan okunur, hard-code değil). |
| K12 | **Ödeme: iyzico Sandbox kullanılacak** (onaylandı, yeni bağımlılık izni verildi). `PaymentProvider` arayüzü korunur; otomatik testler için küçük bir test mock'u olur. Güncel iyzico akışı (checkout form/3DS/callback) kodlamadan önce resmi dokümandan doğrulanacak; yerel Docker'da callback erişimi Adım 6'da planlanır (bkz. 1.7). |
| K13 | **Salon düzeni değişikliği yalnızca seanssız aralıkta** yapılabilir (bkz. 1.8). |
| K14 | **"Blog" = film/içerik açıklaması:** film eklenirken açıklama, görsel vb. içerik girilir. Ayrı blog servisi yok; film/katalog içeriğinin parçası (servis sınırı Adım 3'te). |
| K15 | Harita güncelleme yöntemi (polling vs anlık) bir sonraki adımda detaylıca açıklanıp karara bağlanacak. |
| K16 | **Film alanları:** başlık, açıklama (içerik/blog), afiş URL'i, süre, tür, vizyon tarihi, yaş sınırı, akıllı işaretler, oyuncular, yönetmen, yıldızlandırma (puan). Yıldızlandırmanın kaynağı açık soru (bkz. 1.9). |
| K17 | **Düzen sürümleme onaylandı:** her seans oluşturulduğu andaki salon düzeni sürümüne bağlı kalır; yeni düzen sadece sonradan oluşturulan seanslara uygulanır (bkz. 1.8). |
| K18 | **Film bilgisi TMDB API'den çekilir (ONAYLANDI, proje ticari değil):** admin film eklerken TMDB'de arar, bilgiler forma dolar, admin düzenleyip kaydeder; veri kendi DB'mize kopyalanır (bkz. 1.10). |
| K19 | **Yıldız = yalnızca TMDB puanı** (`vote_average`); üye puanlama yok (YAGNI). |
| K20 | **Yaş sınırı:** TMDB `release_dates` (film) içinden **TR bölgesi** sertifikasyonu okunur; yoksa admin elle girer. Dizi kapsam dışı (`content_ratings` kullanılmayacak, sadece film). |
| K21 | **Akıllı işaretler:** önce TMDB `release_dates` içindeki `descriptors` alanından denenir; boş/eksik gelirse admin sabit listeden (enum) elle seçer. Oyuncu/yönetmen TMDB `credits` verisinden düz metin liste olarak saklanır. |
| K22 | Film detay + yaş sınırı + oyuncular **tek istekte** alınır (`append_to_response=release_dates,credits`, doğrulanacak). |
| K23 | **Fiyat modeli:** salon bazlı tek fiyat; koltuk tipine göre fiyat farkı yok. Seans fiyatı salon varsayılanından kopyalanır, admin değiştirebilir. |
| K24 | **1 işlemde en fazla 5 koltuk;** daha fazlası için işlem tamamlandıktan sonra yeni işlem. Aynı seansta aynı anda tek aktif bekleyen işlem. |
| K25 | **İndirim/kampanya kapsam dışı** (YAGNI). |
| K26 | **Seans arası payı:** 15 dk varsayılan, ayarlanabilir (reklam/temizlik). |
| K27 | **Örnek veri:** İstanbul'da 3 şube; salon sayıları 3, 4, 3 (toplam 10 salon, 10 ayrı düzen). |
| K28 | **Koltuk tipleri:** sadece `STANDARD` ve `ACCESSIBLE` (VIP kaldırıldı). |
| K29 | **Terk edilmiş hold koruması:** kullanıcının 3 hold'u ödeme yapılmadan süresi dolarsa 10 dk boyunca yeni hold açamaz (rate limiting). Zaman penceresi ve uygulama yeri (gateway mi servis mi) Adım 4'te. |
| K30 | **Bilet:** bilet üzerinde film, şube, salon, tarih/saat, koltuklar, rezervasyon kodu ve tutar yazar; **QR doğrulama içindir** ve doğrulama adresini taşır (`/bilet/{kod}`). Doğrulama sayfası bileti geçerli/geçersiz olarak ve aynı bilgilerle gösterir, kişisel veri göstermez. QR çizimi için kütüphane onaya bağlı (bkz. 2.7). "Kullanıcı bileti kullanıldı" işaretleme ve gişe okutma ekranı kapsam dışı. |
| K31 | **5 deployable servis onaylandı:** api-gateway, auth, catalog, booking, payment (bkz. §3). §3.6'daki diğer maddeler (Spring Cloud Gateway, Docker DNS, tek MySQL/4 şema, bellek içi rate limit, iki uçlu harita) çalışma varsayımıdır; Adım 6'da kesinleşir. |
| K32 | **Geç gelen ödeme: P2 (ödeme penceresi).** Ödeme başlatılınca hold bir kez sabit kısa süre uzar (varsayılan +2 dk, yapılandırılabilir), satış kapanışını aşmaz; terk edilen ödemeler K29'a sayılır (§4.12). |
| K33 | **K29 penceresi W = 30 dk.** Polling aralığı **5 sn** (yapılandırılabilir). |
| K34 | **Seans iptali / toplu iade kapsam dışı** (varsayılan; kullanıcı cevabı düzen göçüne odaklandı, bkz. K35). |
| K44 | **Sürümler (2026-10-29):** Spring Boot 3.x'in ücretsiz desteği 30.06.2026'da bittiği için **Spring Boot 4.1.2 + Spring Cloud 2025.1.4** (Maven Central'dan doğrulandı). Build: Maven Wrapper (Maven 3.9.16). Kodlamaya geçildi (M0). |
| K43 | **DB planı onaylandı** ([db-planlama.md](db-planlama.md) §9): film listeleri ayrı tablolar; `active_pending_key` üretilmiş kolon + UNIQUE; süresi dolan `HELD` satırlar silinir, `SOLD` kalır; ham callback logu 90 gün (yapılandırılabilir). |
| K42 | **Milestone sırası M0→M9 onaylandı** (M5 paralel olabilir, M8 düzen göçü sonda); örnek veri `dev` seed; her milestone başlarken alt görevlere bölünür (§9.5). |
| K41 | **Frontend kararları:** Bootstrap CSS + SCSS **ve `ng-bootstrap` eklenecek** (2026-10-28 güncellemesi; Angular 13 uyumlu sürüm 12.x, doğrulanacak); `angularx-qrcode` onaylandı (Angular 13 uyumlu sürüm kodlamadan önce doğrulanır); ödeme sayfasına tam yönlendirme (iframe yok); düzen editöründe undo yok; arayüz yalnızca Türkçe; TMDB afişi hotlink. |
| K40 | **API kararları:** kullanıcı kendi hold'unu bırakabilir (`DELETE /reservations/{id}`, K29 sayacına dahil – varsayım); koltuk durumu misafire açık; film silinmez, gizlenir (`visibility`); kayıt yalnızca e-posta + parola, bilette ad/soyad/e-posta yok; düzen göçü orkestrasyonu booking→catalog `activate` (§7.8); para string, id `Long`, geçersiz bilet 404. |
| K39 | **iyzico yalnızca yerelde, sandbox ile kullanılır; `localhost` yeterli (tünel/webhook yok).** Callback tarayıcı yönlendirmesidir (token POST), sonuç `retrieve` ile doğrulanır (§7.1). |
| K38 | **Bağımlılıklar ve Redis kesintisi:** JWT doğrulama/imzalama için `spring-boot-starter-oauth2-resource-server` onaylandı (servlet servislerde ve reaktif gateway'de; elle JWT filtresi yazılmaz, rules §5.6'daki `OncePerRequestFilter` ifadesi bu starter'ın filtre zinciriyle karşılanır). Redis: `spring-boot-starter-data-redis-reactive` (gateway rate limiter). Redis erişilemezse: genel sınırda istek geçer (fail-open), `/auth/login` ve `/auth/register` reddedilir (fail-closed); varsayılan davranış kodlamadan önce dokümandan doğrulanır. Spring Cloud Gateway, Flyway, MapStruct, Lombok, springdoc, Testcontainers zaten rules.md/K31/K37 kapsamında; ek onay gerekmiyor sayıldı (itiraz varsa söylenir). |
| K37 | **Monorepo/Docker:** repo yapısı §6.1 onaylı; **frontend ve gateway ayrı origin** (tek origin yok → gateway'de CORS); Flyway; **gateway rate limit Redis ile** (yeni bileşen, onaylandı); `sinema-common` servlet tabanlı kalır, gateway kendi reaktif JWT doğrulamasını yapar; `git init` yapılır; iyzico callback/tünel kararı önce resmi dokümandan doğrulanır (Adım 7). |
| K36 | **Auth:** RS256; access 15 dk, refresh 14 gün (cookie, rotasyonlu); hesap kilidi (5 hata → 5 dk) + IP rate limit; e-posta doğrulama ve şifre sıfırlama yok; servisler arası iletişimde Bearer servis JWT (`X-Internal-Key` yok); ilk ADMIN `.env` ile (bkz. §5). |
| K35 | **Düzen değişikliğinde bilet göçü (kararlar §4.13'te: upgrade = boş ve eski koltuğa/perdeye göre yakın, boş koltuk yoksa değişiklik reddedilir, bildirim uygulama içi):** eski düzen silinmez, V2 açılır; satılmış biletler eski sürümde saklanır; koltuk yeni düzende aynı konumdaysa bilet sessizce güncellenir; fiziksel olarak kaldırılmışsa sistem otomatik daha iyi boş koltuk atar (upgrade) ve kullanıcıya bildirim gönderir. **K17 ve §2.4 değişmezleriyle çelişir, bkz. §4.13.** |

### 1.2 Roller
- **GUEST:** film, seans, koltuk doluluk durumunu görür.
- **USER:** GUEST yetkileri + koltuk tutma ve satın alma + kendi rezervasyonlarını görme.
- **ADMIN:** salon/düzen, film, seans, fiyat, blog içeriği yönetimi. İleride ayrıntılı yetki (ör. içerik admini / salon admini) gerekirse authority bazlı bölünür.

### 1.3 Salon düzeni (öneri – onay bekliyor)
Izgara (grid) tabanlı ama hücre tipli bir düzen. Her hücre bir tiptir: `SEAT`, `EMPTY` (boşluk/koridor), `ENTRANCE`, `EXIT`, `SCREEN`, `WALL`.
- Admin düzeni bir editörle çizer, böylece her salon farklı olabilir (L şekilli, ortada koridor, yan giriş vb.).
- Koltuk tipleri: `STANDARD`, `VIP`, `ACCESSIBLE` (engelli).
- Yeni teknoloji gerekmez: düzen backend'de saklanır, Angular'da SCSS grid ile çizilir.
- Seansı olan salonun düzeni değiştirilirse mevcut rezervasyonları bozabilir → "seansı olan salonun düzeni kilitlenir/sürümlenir" kuralı Adım 2'de karara bağlanacak.

### 1.4 Satış kapanış zamanı (kullanıcının sorusu: "film saatine kadar mı?")
Sorun: Kiosklarda geç alım yapılıp filme geç girilebiliyor. Bizim sistem online olduğu için bunu **kural olarak** belirleyebiliriz.

Öneri: Satış kapanışı **seans başına yapılandırılabilir** bir değer olsun (`salesCloseOffsetMinutes`). Sistemde tek bir yerde (service katmanı) kontrol edilir:
- **Seçenek A (önerim):** Seans başlangıcından **N dakika önce** satış kapanır (ör. N = 10). Gerekçe: geç girişi engeller, hold süresi ile birlikte tutarlı kalır.
- **Seçenek B:** Seans başlangıç saatine kadar satılır (basit, kioskta gördüğün sorun burada da olur).
- **Seçenek C:** Başlangıçtan sonra reklam süresi kadar (ör. +15 dk) satılır.

Ek kural: Hold süresi, satış kapanışını **aşamaz** (hold bitişi = min(hold süresi, satış kapanışı)). Kapanıştan sonra tutulan koltuklar da serbest kalır.

### 1.5 Hold (geçici tutma) kuralları (öneri)
- Süre: **sunucu tarafında** `expiresAt` ile tutulur (istemci saati güvenilmez). Önerilen: **5 dakika**.
- Süre dolunca koltuk **kendiliğinden** serbest sayılır (sorgularda `expiresAt > now` kontrolü) ve ayrıca bir temizlik job'ı kayıtları siler. Böylece job gecikse bile koltuk kilitli kalmaz.
- Kötüye kullanım önlemleri: kullanıcı başına toplam aktif hold ≤ 5; yalnızca giriş yapmış kullanıcı hold açabilir; gateway'de rate limit; hold uzatma yok (süre dolunca baştan seçim gerekir); aynı kullanıcı sürekli hold-bırak yaparak sabote etmesin diye kısa süreli cooldown/limit düşünülebilir.

### 1.6 Hold davranışı (güncel)
Hold süresi 3 dk (K11). Hold bitişi = min(hold süresi, satış kapanışı). Süre dolunca koltuk sorgularda `expiresAt > now` kontrolüyle serbest sayılır; temizlik job'ı sadece kayıtları siler.

### 1.7 Ödeme (test amaçlı, anlaşmasız)
**HTTP mi webhook mu?**
- **HTTP (senkron):** Biz ödeme sağlayıcısına istek atarız, cevabı hemen alırız ("başarılı/başarısız").
- **Webhook (asenkron):** Sağlayıcı sonucu sonradan **bize** bir HTTP isteği atarak bildirir (3D Secure, banka onayı gibi sonradan netleşen durumlar için). Webhook'ta imza doğrulaması ve tekrar gelen bildirimi güvenle işleme (idempotency) şarttır.

**Seçenekler:**
1. **Kendi mock ödeme sağlayıcımız (önerim, başlangıç):** `payment-service` içinde `PaymentProvider` arayüzü (Strategy/Adapter) + `MockPaymentProvider`. Test kartı numarasına göre başarılı/başarısız/zaman aşımı simüle eder. Yeni bağımlılık yok.
2. **iyzico Sandbox:** Sözleşme gerektirmez, ücretsiz sandbox hesabı ve test kartları vardır; gerçek para hareketi olmaz. Ama yeni dış bağımlılık/SDK olduğu için **onayına bağlı**. Mock'tan sonra `IyzicoPaymentProvider` olarak aynı arayüze eklenir, mevcut kod değişmez (Open/Closed).

Ödeme akışı koltuk hold'una bağlıdır: hold süresi (3 dk) içinde ödeme tamamlanmazsa koltuklar serbest kalır; ödeme başarılı olursa koltuklar atomik olarak SATILDI yapılır. Ayrıntı Adım 4'te.

### 1.8 Salon düzeni kilitleme (güncel)
Kural: Salonun **sürmekte olan bir seansı veya tutulmuş/satılmış koltuğu varken** düzen değiştirilemez; düzen yalnızca seanssız boşlukta (örn. bugünkü son seans bitişi ile yarınki ilk seans başlangıcı arası) güncellenebilir.

Açık nokta: Gelecekteki seanslarda zaten satılmış koltuk varsa düzen değişince ne olacak? Önerim **düzen sürümleme (versioning)**: her seans oluşturulduğu andaki düzen sürümüne bağlı kalır, yeni düzen sadece sonradan oluşturulan seanslara uygulanır. Satılmış koltuklar asla bozulmaz.

### 1.9 Satış kapanışı notu (K10 güncel)
Seans başladıktan 10 dk sonrasına kadar satış açık kalır. Hold bitişi = min(hold süresi, satış kapanışı) kuralı aynen geçerli. Bilinen trade-off: kapanış başlangıçtan sonra olduğu için filme geç giren olabilir; bu bilinçli iş kararıdır (reklam süresi payı). Süre admin/ayardan değiştirilebilir.

### 1.10 TMDB entegrasyonu (öneri)
- **Ne çekilebilir:** başlık, özet (tr-TR dilinde), afiş yolu, süre, türler, vizyon tarihi, oyuncular, yönetmen, TMDB puanı, yaş sınıflandırması (ülkeye göre, eksik olabilir).
- **Çekilemeyen:** Türkiye "akıllı işaretleri" (şiddet, korku vb.) TMDB'de yok → admin elle işaretler.
- **Akış:** Admin "TMDB'den ara" → sonuç seç → form dolar → admin düzenler → kaydet. Veri **kendi DB'mize kopyalanır**; kullanıcı sayfaları TMDB'yi çağırmaz (hız, kota, bağımlılık riski yok). TMDB erişilemezse elle giriş her zaman çalışır.
- **Teknik:** catalog tarafında `TmdbClient` (Feign, dış API için de uygun), API key env/secret, timeout/fallback. Yeni kütüphane yok ama **yeni dış servis bağımlılığı → onay gerekir**.
- **Yıldız:** TMDB puanı ile bizim üyelerin puanı ayrı tutulur; ekranda "TMDB puanı" ve "Üye puanı" ayrı gösterilebilir.
- **Dikkat (doğrulanacak):** TMDB ücretsiz API'si ticari olmayan kullanım içindir, atıf (logo + "bu ürün TMDB API'sini kullanır ama TMDB tarafından onaylanmamıştır" notu) şarttır ve ticari kullanımda lisans gerekir. Güncel şartlar kodlamadan önce resmi dokümandan kontrol edilecek.
- **Afiş:** TMDB görsel adresi saklanır (hotlink) veya kopyalanır; şartlara göre karar verilir.

### 1.11 Adım 1 sonucu
Kapsam kararları tamam (K1–K22). Kodlamadan önce doğrulanacaklar: TMDB `descriptors` alanının TR için dolu gelip gelmediği, `append_to_response` kullanımı, TMDB atıf (attribution) şartı (ticari olmasa da gerekir), iyzico güncel akış ayrıntıları.

## 2. Domain Modeli

> Durum: TASLAK (onay bekliyor). Kod yok; bu bölüm veri ve kural modelidir. Servislere dağılımı Adım 3'te.

### 2.1 Varlıklar (özet)
```
Cinema(3 şube) 1─* Hall 1─* HallLayout(sürümlü) 1─* LayoutCell (SEAT/ENTRANCE/EXIT/SCREEN/WALL)
Movie  1─* Showtime *─1 Hall            Showtime *─1 HallLayout (mühürlü sürüm)
Showtime 1─* SeatBooking *─1 LayoutCell        (koltuk durumu: tek doğruluk kaynağı)
User 1─* Reservation 1─* ReservationSeat ─> SeatBooking
Reservation 1─* Payment
```
Servisler arası referanslar yalnızca **id** ile tutulur (başka servisin tablosuna FK yok).

### 2.2 Varlık ayrıntıları
| Varlık | Önemli alanlar | Not |
|---|---|---|
| **Cinema** | id, name, city, address | İstanbul örneği. (Soru 1) |
| **Hall** | id, cinemaId, name, activeLayoutId, defaultTicketPrice | Salon. Aktif düzen sürümüne işaret eder. Salondaki tüm koltuklar aynı fiyatlıdır (K25). |
| **HallLayout** | id, hallId, version, gridRows, gridCols, createdAt | **Değişmez (immutable)**; düzen değişince yeni sürüm oluşur (K17). |
| **LayoutCell** | id, layoutId, rowIndex, colIndex, cellType, seatRowLabel, seatNumber, seatType (STANDARD/ACCESSIBLE – fiyat farkı yok, sadece görsel/erişilebilirlik) | Sadece boş olmayan hücreler saklanır. `cellType = SEAT` olanlar koltuktur. Koltuk kimliği = LayoutCell.id. |
| **Movie** | id, tmdbId (unique), title, overview, posterUrl, runtimeMinutes, genres, releaseDate, certification (TR), contentDescriptors, cast, director, voteAverage | TMDB'den kopyalanır, admin düzenleyebilir (K18–K22). |
| **Showtime** | id, movieId, hallId, layoutId, startsAt, endsAt, ticketPrice, salesCloseOffsetMinutes, status | `endsAt = startsAt + süre + ara payı (15 dk, ayarlanabilir – K26)`. `ticketPrice` seans oluşturulurken salonun `defaultTicketPrice` değerinden kopyalanır, admin değiştirebilir. Aynı salonda seanslar çakışamaz. |
| **SeatBooking** | id, showtimeId, layoutCellId, status (HELD/SOLD), holdExpiresAt, userId, reservationId, version | **Kritik tablo.** `UNIQUE(showtimeId, layoutCellId)` (K9). Satır yoksa koltuk boştur. |
| **Reservation** | id, userId, showtimeId, status, totalAmount, expiresAt, createdAt | Bir işlem = en fazla 5 koltuk (K24). |
| **ReservationSeat** | reservationId, seatBookingId, unitPrice (anlık fiyat kopyası) | Fiyat sonradan değişse bile bilet fiyatı sabit kalır. |
| **Payment** | id, reservationId, provider (IYZICO), providerPaymentId, status, amount, idempotencyKey, createdAt | Ham callback logu ayrı tutulur (denetim). |
| **User / Role** | id, email, passwordHash, role (USER/ADMIN) | Auth servisi. Misafir hesap değildir. |

### 2.3 Durum makineleri
**Koltuk (SeatBooking), seans bazında**
```
BOŞ (satır yok / süresi dolmuş) ──hold──> HELD ──ödeme başarılı──> SOLD (terminal, K6: iptal yok)
                                            └─ süre doldu / ödeme başarısız / satış kapandı ──> BOŞ
```
**Rezervasyon**
```
PENDING_PAYMENT ──ödeme başarılı──> CONFIRMED
      ├─ hold süresi doldu ──> EXPIRED
      └─ ödeme reddedildi ───> FAILED
```
**Ödeme:** `INITIATED → PENDING → SUCCEEDED | FAILED`. Geç gelen başarılı callback + süresi dolmuş hold = ayrı ele alınır (Adım 4).

### 2.4 Değişmezler (invariant)
1. Bir seansta bir koltuk için en fazla **bir** geçerli SeatBooking vardır (DB kısıtı ile garanti, kod kontrolüne güvenilmez).
2. HELD koltuğun `holdExpiresAt = min(şimdi + 3 dk, satış kapanışı)` (K11, K10).
3. Satış kapanışı: `now ≤ startsAt + salesCloseOffsetMinutes` (K10, varsayılan +10 dk).
4. **Bir işlemde (rezervasyon) en fazla 5 koltuk** seçilebilir (K24). Aynı seans için daha fazla koltuk gerekirse ilk işlem tamamlandıktan sonra yeni işlem açılır. Bir kullanıcının aynı seansta aynı anda yalnızca **1 aktif (PENDING_PAYMENT) rezervasyonu** olabilir.
5. SOLD satır silinmez ve değişmez; iptal/iade akışı yok (K6).
6. Düzen sürümü (HallLayout) yayınlandıktan sonra değişmez; salon düzeni değişikliği yalnızca seanssız aralıkta yeni sürüm olarak yapılır (K13, K17).
7. Seans oluşturulurken o anki aktif düzen sürümü seansa bağlanır; seans düzeni sonradan değişmez.
8. Aynı salonda iki seansın `[startsAt, endsAt)` aralığı çakışamaz.
9. Para: decimal (BigDecimal), para birimi TRY; zaman: UTC saklanır, ekranda Europe/Istanbul.
10. Sunucu saati tek referanstır (istemci saati yok).

### 2.5 Düzen kilidi kuralı (K13) – veriyle ifadesi
Düzen yeni sürüm olarak yalnızca şu koşulda yayınlanabilir: salonun `now` anında sürmekte olan seansı yok **ve** değişiklik anı ile bir sonraki seans başlangıcı arasında yapılıyor. Eski seanslar eski sürümde kalır (K17), bu yüzden gelecek seans doluluğunu bozmaz.

### 2.6 Adım 2 sonucu
Domain modeli tamam (K23–K30). `VIP` kaldırıldı, 3 şube (3/4/3 salon) örnek verisi netleşti.

### 2.7 Bilet ve QR doğrulama (K30)
- **Bilet sayfası (giriş yapmış kullanıcı):** film + afiş, şube, salon, tarih/saat, koltuklar (ör. F-7, F-8), rezervasyon kodu, tutar, QR.
- **QR içeriği:** doğrulama adresi `/bilet/{rezervasyonKodu}`. Kod rastgele ve tahmin edilemez olmalı.
- **Doğrulama sayfası (herkese açık, giriş gerekmez):** kodun geçerliliği (CONFIRMED mi) + film, şube, salon, tarih/saat, koltuklar. Kullanıcı adı/e-posta gösterilmez. Kodlara toplu deneme (brute force) yapılmasına karşı gateway'de rate limit.
- **Tahmini efor ~1 gün:** bilet sayfası + QR çizimi + doğrulama endpoint'i/sayfası. QR kütüphanesi tarayıcıda çalışan Angular 13 uyumlu bir paket olur (örn. `angularx-qrcode`; uyumluluk kodlamadan önce doğrulanacak, onay gerekir). Backend'e kütüphane eklenmez.
- **Kapsam dışı:** "bilet kullanıldı" işaretleme, gişe görevlisi ekranı.

## 3. Servis Sınırları

> Durum: TASLAK (onay bekliyor). Kod yok. Bounded context başına tek servis, her servis kendi şemasına sahip (rules.md §5.5, §5.7).

### 3.1 Servis listesi
```
 Angular SPA ──> API Gateway ──┬─> auth-service     (auth_db)
                               ├─> catalog-service  (catalog_db) ──> TMDB (dış API)
                               ├─> booking-service  (booking_db) ──Feign──> catalog-service
                               └─> payment-service  (payment_db) ──Feign──> booking-service
                                                                  └──────> iyzico (dış API)
 MySQL: tek instance, 4 ayrı şema + servis başına ayrı DB kullanıcısı
```
| Servis | Sorumluluk (tek cümle) | Sahip olduğu veri |
|---|---|---|
| **api-gateway** | Tek giriş: routing, JWT ön doğrulama, genel rate limit, CORS, correlation-id | Yok (stateless) |
| **auth-service** | Kayıt, giriş, JWT/refresh token üretimi, roller | User, Role, RefreshToken |
| **catalog-service** | "Ne, nerede, ne zaman" – şube/salon/düzen/film/seans yönetimi + TMDB entegrasyonu | Cinema, Hall, HallLayout, LayoutCell, Movie, Showtime |
| **booking-service** | Koltuk tutma, rezervasyon, satış, bilet doğrulama; **K9'un garantörü** | SeatBooking, Reservation, ReservationSeat, terk edilmiş hold sayaçları |
| **payment-service** | Ödeme başlatma, iyzico adapter'ı, callback, idempotency | Payment, callback logu |

### 3.2 Neden bu sınırlar?
- **catalog tek servis:** Düzen kilidi kuralı (K13) salonun seanslarını ve düzenini birlikte görmeyi gerektirir; ayrı servislerde dağıtık kontrol gerekirdi. Film ve seans da aynı yönetim akışının parçası (admin panel). YAGNI: şimdilik bölmüyoruz.
- **booking ayrı servis:** En kritik ve en yoğun yük (hold/satış) burada; bağımsız ölçeklenir, DB kısıtı (K9) tek yerde yaşar.
- **payment ayrı servis:** Dış sağlayıcıya bağımlılık izole kalır (`PaymentProvider` değişebilir), callback güvenliği ve ödeme verisi ayrı tutulur. *Trade-off:* servis sayısı artar; geliştirme eforu için booking'e katmak mümkün ama sağlayıcı değişimi ve test izolasyonu zorlaşır. Önerim ayrı.
- **auth ayrı servis:** Kimlik tek yerde; diğer servisler kullanıcıyı yalnızca JWT içindeki `userId`/`role` ile tanır.
- **Blog/içerik servisi yok** (K14): film açıklaması Movie içinde.

### 3.3 Servisler arası çağrılar (Feign – senkron, tek yönlü)
| Çağıran → Çağrılan | Ne için | Not |
|---|---|---|
| booking → catalog | Seans bilgisi (başlangıç, kapanış ofseti, fiyat, düzen id), seçilen hücrelerin gerçekten `SEAT` olduğunun doğrulanması, bilet için görüntü bilgisi (film/şube/salon adı, koltuk etiketleri) | Resilience4j timeout/retry/circuit breaker; fallbackFactory. Başarısızsa hold açılmaz (fail fast). |
| payment → booking | Rezervasyonun tutarı ve durumu (**tutar istemciden değil sunucudan alınır**); ödeme sonucunda "rezervasyonu onayla" | Booking çağrıyı idempotent işler. |
| auth ↔ diğerleri | Çağrı yok | Diğer servisler JWT'yi kendileri doğrular. |
| catalog → TMDB | Film arama/detay | Admin akışında; hata olursa elle giriş (K18). |
| payment → iyzico | Ödeme başlat/doğrula | Adapter arkasında. |

Döngü yok: `payment → booking → catalog` tek yönlü. Başka bir yönde bağımlılık eklemek için ADR gerekir.

### 3.4 Önemli tasarım kararları
1. **Bilet görüntü verisi snapshot'tır:** Booking, hold anında catalog'dan aldığı film/şube/salon adı, seans saati ve koltuk etiketlerini Reservation'a kopyalar. Sonuç: doğrulama sayfası (K30) catalog'a bağımlı olmadan çalışır; film adı sonradan değişse bile bilet değişmez.
2. **Koltuk haritası iki kaynaktan birleşir:** Düzen (catalog) ve doluluk durumu (booking) ayrı uç noktalar; Angular iki akışı RxJS ile birleştirir (`combineLatest`). Ayrı bir BFF/aggregator servisi yok (YAGNI).
3. **K29 (3 terk edilmiş hold → 10 dk bekleme) iş kuralıdır:** booking-service içinde tutulur (kullanıcı bazlı sayaç). Gateway'deki genel rate limit IP/istek sıklığı içindir, bu kural için kullanılmaz.
4. **Rezervasyon onayı atomik ve tek yerde:** `HELD → SOLD` geçişi yalnızca booking-service yapar; payment sadece "onayla" der. Detay ve geç gelen ödeme (hold bitmiş, ödeme başarılı) senaryosu Adım 4'te.
5. **Servis keşfi yok:** Feign adresleri env ile verilir, Docker Compose ağ adı (DNS) yeterli (KISS).
6. **Ortak kod:** küçük bir Maven kütüphanesi `sinema-common` (hata response formatı, correlation-id filtresi, JWT doğrulama yardımcıları). İş mantığı içermez; sürümü parent POM'dan yönetilir.
7. **Veritabanı:** Tek MySQL instance, 4 şema, her servisin kendi kullanıcısı yalnızca kendi şemasına yetkili. Şema yönetimi Flyway/Liquibase (karar Adım 6'da).

### 3.5 Gateway rotaları (özet)
| Yol | Hedef | Erişim |
|---|---|---|
| `/api/v1/auth/**` | auth-service | Herkese açık (login/register), refresh için token |
| `/api/v1/catalog/**` (GET) | catalog-service | GUEST |
| `/api/v1/admin/catalog/**` | catalog-service | ADMIN |
| `/api/v1/bookings/**` | booking-service | USER |
| `/api/v1/tickets/{kod}` | booking-service | Herkese açık (QR doğrulama), rate limit |
| `/api/v1/payments/**` | payment-service | USER |
| `/api/v1/payments/callback/**` | payment-service | Sağlayıcıdan gelir (imza doğrulamalı) |

### 3.6 Açık sorular / onay gerekenler
1. **5 backend servis (gateway, auth, catalog, booking, payment)** ayrımı uygun mu? Özellikle payment'in ayrı olması.
2. **Yeni bağımlılık onayı:** Spring Cloud Gateway (API Gateway). (OpenFeign ve Resilience4j zaten kural setinde var.)
3. **Servis keşfi (Eureka) yok, Docker DNS + env ile Feign adresi** uygun mu?
4. **Tek MySQL instance, 4 şema** uygun mu (geliştirme/demo için)?
5. **Genel rate limit** (gateway) için mekanizma: Spring Cloud Gateway'in hazır rate limiter'ı Redis ister (yeni bileşen). Alternatif tek instance için bellek içi basit sınırlayıcı. Karar Adım 5/6'da; şimdilik önerim Redis'siz, bellek içi.
6. **Koltuk haritası için Angular'ın iki uç noktayı birleştirmesi** (BFF yok) uygun mu?

---
## 4. Concurrency Stratejisi (K9'un çözümü)

> Durum: TASLAK (onay bekliyor). Kod yok. Hedefler: K9 (çift satış yok), K7/K11 (3 dk hold), K8/K24 (≤5 koltuk/işlem), K10 (satış kapanışı), K29 (terk edilmiş hold koruması), K15 (harita güncelleme).

### 4.1 Temel ilke
Asıl güvence **veritabanıdır**, uygulama kodundaki "boş mu diye bak, sonra kaydet" kontrolü değil. İki istek aynı anda "boş" görüp ikisi de kaydedebilir; bunu yalnızca DB'nin kendi kısıtı kesin olarak engeller.

### 4.2 Yaklaşım seçenekleri
| Seçenek | Nasıl | Artı | Eksi |
|---|---|---|---|
| **A. Seans satırına kilit** (`SELECT … FOR UPDATE`) | Seansa ait tüm işlemler sıraya girer | Basit | Aynı seansta herkes birbirini bekler; ölçeklenmez |
| **B. Benzersizlik kısıtı + koşullu UPDATE (önerim)** | `UNIQUE(showtime, koltuk)`; hold = INSERT; süresi dolmuş hold'u devralma = koşullu UPDATE | Kesin garanti, ek bileşen yok, yalnızca çakışan koltuklar birbirini etkiler | SQL'i dikkatli yazmak gerekir |
| **C. Redis dağıtık kilit** | Kilit Redis'te | Hızlı | Yeni bileşen, gerçeğin kaynağı ikiye bölünür, kilit kaybolursa çift satış riski |

**Öneri: B.**

### 4.3 Hold akışı (B ile)
1. **Ön kontroller (fail fast):** kullanıcı doğrulandı mı; ≤5 koltuk (K24); aynı seansta aktif `PENDING_PAYMENT` rezervasyonu yok mu (varsa 409 + mevcut rezervasyon id); K29 bekleme süresinde mi (429 + kalan süre); satış açık mı (K10, catalog'dan seans bilgisi); hücreler gerçekten `SEAT` mi (catalog).
2. **Tek transaction içinde** (hepsi ya da hiçbiri): koltuklar **sıralı** (cell id artan) işlenir, böylece iki işlem ters sırada kilitleyip deadlock yaratmaz. Her koltuk için:
   - `INSERT` dene (HELD, `holdExpiresAt`, rezervasyon id). Başarılıysa koltuk senindir.
   - Benzersizlik hatası alırsan satır var demektir: **koşullu UPDATE** ile devral → `WHERE showtime=? AND koltuk=? AND status='HELD' AND holdExpiresAt <= şimdi`. Etkilenen satır 1 ise süresi dolmuş hold'u sen aldın; 0 ise koltuk gerçekten dolu (SOLD veya geçerli HELD).
   - Bir koltuk bile alınamazsa tüm transaction geri alınır, istemciye **409 + hangi koltukların dolu olduğu** döner.
3. Rezervasyon `PENDING_PAYMENT` oluşur, `expiresAt = min(şimdi + 3 dk, satış kapanışı)` (K10/K11).
4. İstemciye `reservationId` ve **sunucunun** `expiresAt` değeri döner; ekrandaki geri sayım bu değerden hesaplanır (istemci saatine güvenilmez).

**Neden güvenli:** Aynı koltuğa iki istek aynı anda gelirse InnoDB satır/index kilidi yüzünden biri INSERT'i kazanır, diğeri benzersizlik hatası alır. Devralma UPDATE'i tek atomik ifadedir; iki kişi aynı süresi dolmuş hold'u devralamaz (ikincinin koşulu artık sağlanmaz).

### 4.4 Zaman kaynağı
Süre karşılaştırmaları **DB sunucu zamanıyla** yapılır (UTC); uygulama örneklerinin saat farkı sonucu etkilemez. Test için zaman soyutlaması (Clock) kullanılır.

### 4.5 Onay (HELD → SOLD)
Payment "ödeme başarılı" dediğinde booking tek transaction'da koşullu UPDATE yapar:
`status='SOLD' WHERE rezervasyon=? AND status='HELD' AND holdExpiresAt > şimdi`, etkilenen satır sayısı = koltuk sayısı değilse tümü geri alınır. Rezervasyon `CONFIRMED` olur. İşlem idempotent: aynı onay iki kez gelirse ikincisi zararsızdır.

### 4.6 Süre dolumu ve temizlik
- Doğruluk **job'a bağlı değildir:** doluluk sorguları `HELD ve holdExpiresAt > şimdi` filtreler, devralma UPDATE'i süresi dolmuşu zaten alabilir.
- Zamanlanmış job yalnızca düzenlemedir: süresi dolmuş `HELD` satırları siler, `PENDING_PAYMENT` rezervasyonları `EXPIRED` yapar. Seans kapandıktan sonra kalan tüm hold'lar da temizlenir.

### 4.7 K29 – terk edilmiş hold koruması (türetilmiş, ek tablo yok)
Yeni hold isteği geldiğinde kullanıcının son W dakikadaki rezervasyonlarına bakılır: ödenmeden süresi dolan (`EXPIRED` veya süresi geçmiş `PENDING_PAYMENT`) sayısı ≥ 3 ise, **son terk edilmenin süresi dolduğu andan 10 dk** sonrasına kadar yeni hold reddedilir (429 + `Retry-After`). Başarılı satın alma sayacı etkilemez. Avantajı: job gecikse bile doğru çalışır, ek veri/senkronizasyon yok. (W değeri: soru 2.)
Ek olarak gateway'de genel istek sıklığı sınırı (IP/kullanıcı) bulunur; bu K29'dan ayrıdır.

### 4.8 Ödeme ile tutarlılık (booking ↔ payment, eventual consistency)
- Payment servisi tutarı ve durumu **booking'den sorar**; istemci tutar gönderemez.
- Ödeme kaydında sağlayıcı ödeme id'si benzersizdir; tekrar gelen callback aynı sonucu üretir (idempotency).
- **Başarılı ödeme ama onay başarısız** (ağ hatası): payment durumu `SUCCEEDED`, onay beklemede kalır; yeniden deneme + periyodik mutabakat (reconciliation) job'ı onayı tekrar dener.
- **Geç gelen ödeme (hold dolmuş):** iki politika var (soru 1). Her iki durumda da koltuk başkasına geçtiyse ödeme iade edilir (`REFUND_REQUIRED` → iyzico iade/iptal). Bu, kullanıcı iptali (K6) değil, sistemin hata düzeltmesidir; K6 ile çelişmez.

### 4.9 Harita güncelleme (K15) – polling mi, anlık mı?
- **Polling (önerim):** Angular belirli aralıkla (ör. 5 sn) `GET /bookings/showtimes/{id}/seat-status` çağırır (`timer` + `switchMap`; sekme gizliyken durur). Cevap yalnızca dolu/tutulu koltuk id'lerini içerir (kimin tuttuğu gösterilmez); düzen zaten catalog'dan bir kez gelir. Sorgu `showtime` indeksiyle küçük ve ucuz.
- **Anlık (SSE/WebSocket):** Değişimde sunucu istemciye iter. Daha akıcı ama bağlantı yönetimi, gateway ayarları ve çok örnekte olay dağıtımı (broker) gerektirir; ilk sürüm için fazla.
- **Neden polling yeterli:** Doğruluk haritanın tazeliğine bağlı değil. Kullanıcı eski bir haritada dolmuş koltuğa tıklarsa hold isteği **409** döner, harita hemen yenilenir ve "bu koltuk az önce alındı" denir. Yani en kötü ihtimal küçük bir UX gecikmesi.
- API, ileride SSE'ye geçilebilecek biçimde tasarlanır; polling aralığı konfigürasyondan okunur.

### 4.10 Test planı (Testcontainers + gerçek MySQL)
1. N paralel iş parçacığı aynı koltuğu tutmaya çalışır → **tam 1 başarı**.
2. Aynı süresi dolmuş hold'u iki işlem devralmaya çalışır → tam 1 başarı.
3. Çok koltuklu istek, bir koltuk doluysa **hiçbiri** alınmaz (hepsi ya da hiçbiri).
4. Ters sırada koltuk seçen iki işlem deadlock olmadan biter.
5. Süre dolduktan sonra onay → reddedilir; süre içinde çift onay → idempotent.
6. K29: 3 terk edilmiş hold sonrası 4. istek 429; 10 dk sonra serbest.
7. Satış kapanışı sonrası hold/onay reddedilir.

### 4.11 Kapsam dışı / not
Admin'in satılmış koltuğu olan seansı iptal etmesi (toplu iade) kapsam dışı. Seanslar silinmez, durumu `CANCELLED` yapılabilir; bunun rezervasyonlara etkisi için sonraki bir karar gerekir. booking→catalog yönü tek yönlü kalsın diye catalog booking'e çağrı yapmaz.

### 4.12 Sorular ve cevaplar (2026-10-27)
Cevaplar: 1 → **P2** (K32), 2 → **30 dk** (K33), 3 → **5 sn** (K33), 4 → seans iptali kapsam dışı (K34); kullanıcı ayrıca düzen göçü isteğini bildirdi (§4.13, K35).
1. **Geç gelen ödeme politikası:**
   - **P1 (katı):** Hold bittiyse onay yok; ödeme başarılıysa iade edilir. Kural net, ama 3DS gecikirse kullanıcının ödemesi iade edilip baştan başlaması gerekir.
   - **P2 (ödeme penceresi, önerim):** Ödeme başlatıldığında hold bir kez, sabit kısa süre (ör. +2 dk) uzar (satış kapanışını aşmaz). Ödeme başlatmak zaten kart/3DS adımı gerektirdiği için sabotajda kullanması zordur; terk edilen ödemeler K29'a sayılır. Geç başarı ihtimali ciddi azalır.
2. **K29 penceresi W:** "3 terk edilmiş hold" son kaç dakika içinde sayılsın? (Önerim 30 dk.)
3. **Polling aralığı** 5 sn uygun mu? (Ayarlanabilir.)
4. **Seans iptali** (§4.11) kapsam dışı kalsın mı?

### 4.13 Düzen değişikliğinde bilet göçü (K35)
**Kararlar (2026-10-27):**
- **Yedek koltuk seçimi:** aynı seansta **boş** ve **eski koltuğa yakın** koltuk; yakınlık eski konuma ve perdeye (`SCREEN` hücresi) göre olan konumun benzerliğiyle ölçülür (kullanıcı salon/perde konumlandırmasını kastetti; tam formül aşağıdaki varsayımda).
- **Boş koltuk yoksa** göç yapılamaz: düzen değişikliği **reddedilir** (önce önizleme/dry-run, çözülemeyen biletler admin'e raporlanır).
- **Bildirim:** uygulama içi (booking içinde tablo, yeni bağımlılık yok); bilet sayfasında "koltuğunuz değişti" bandı. E-posta bu sürümde yok.

**Varsayımlar (itiraz yoksa geçerli):** "aynı yer" = aynı (rowIndex, colIndex) ve hücre hâlâ `SEAT`; yedek koltuk = eski konuma ve perdeye olan mesafesi en küçük boş koltuk (`ACCESSIBLE` ise `ACCESSIBLE`); aynı işlemdeki koltuklar mümkünse bitişik kalır, bitişik yer yoksa değişiklik reddedilir; göç booking'in catalog'dan düzen farkını çekmesiyle yapılır (booking→catalog yönü korunur); göç sırasında salonun gelecek seanslarında kısa bakım kilidi ve tek transaction + sıralı koltuk işleme uygulanır; K17 "başlamamış seanslar V2'ye göçer" olarak, SOLD değişmezliği "yalnızca göç + denetim kaydı" istisnasıyla güncellenir.

**Aşağıdaki liste ilk taslaktır (çözülenler yukarıda):**
**İstenen davranış (kullanıcı):** eski salon düzeni silinmez, V2 açılır; satılmış biletler eski sürümde güvenle saklanır; koltuk yeni düzende aynı yerdeyse bilet arka planda sessizce güncellenir; koltuk fiziksel olarak kaldırılmışsa sistem otomatik daha iyi boş koltuk (upgrade) atar ve kişiye bildirim gönderir.

**Mevcut kurallarla çatışmalar (karar gerekir):**
1. **K17 / değişmez 7:** "Seans düzeni sonradan değişmez, yeni düzen sadece yeni seanslara uygulanır." K35 ise gelecekteki seansları V2'ye taşır. Önerim: K17 yeniden yazılsın: *gelecekteki* (başlamamış) seanslar V2'ye göç eder, başlamış/geçmiş seanslar eski sürümde kalır.
2. **Değişmez 5 / K6:** SOLD satır değişmez. Önerim: SOLD satırın `layoutCellId` alanı yalnızca göç işlemiyle güncellenebilir, her güncelleme denetim kaydına yazılır (istisna açıkça belgelenir). Bu kullanıcı iptali değil, sistem düzeltmesidir (§4.8'deki gibi).
3. **K13:** "Seanssız aralıkta değişiklik" kuralı. Göç varsa gelecekteki seanslar etkilendiği için kural gevşetilebilir; yine de sürmekte olan seans varken göç yapılmamalı.

**Eksik tanımlar (cevap gerekir):**
- **"Aynı yerde" nasıl eşleşir?** Önerim: `(seatRowLabel, seatNumber)` etiketi değil **(rowIndex, colIndex) konumu** + hücre hâlâ `SEAT` ise eşleşir.
- **"Daha iyi koltuk" ne demek?** Fiyat farkı yok (K23), VIP yok (K28). Öneri: aynı seansta boş, `ACCESSIBLE` ise yine `ACCESSIBLE`; aksi halde eski konuma en yakın, ekrana göre merkeze yakın boş koltuk. Gerçek "yükseltme" tanımı için sizin kriteriniz gerekir.
- **Aynı işlemdeki koltuklar** (örn. 3 bitişik koltuk) bitişik kalmalı mı? Bitişik boş koltuk yoksa ne olacak?
- **Boş koltuk kalmadıysa** (seans dolu) ne olur? K6 iptale izin vermez; seçenekler: seans salonu değişikliğinden hariç tutulur (göç yapılmaz), ya da düzen değişikliği reddedilir. Önerim: göç önizlemesi (dry-run) yapılır, çözülemeyen biletler varsa admin'e raporlanıp değişiklik reddedilir.
- **Bildirim kanalı:** projede bildirim servisi/e-posta altyapısı yok. Seçenekler: (a) uygulama içi bildirim (booking içinde tablo, yeni bağımlılık yok), (b) e-posta (SMTP + `spring-boot-starter-mail`, **onay gerekir**). Önerim ilk sürümde (a), bilet sayfasında "koltuğunuz değişti" bandı.
- **Eşzamanlılık:** göç, HELD/devam eden ödemeleri de etkiler; göç sırasında salona ait gelecek seanslarda yeni hold durdurulmalı (kısa bakım kilidi) ve göç tek transaction'da koltuk sırasıyla (deadlock önleme, §4.3) yapılmalı.
- **Servis sınırı:** düzen catalog'da, koltuklar booking'de. Göçü catalog tetikler ama catalog→booking çağrısı §3.3'teki tek yönlü kuralı bozar. Seçenekler: booking, catalog'dan düzen farkını **kendisi çeker** (booking→catalog yönü korunur; admin booking'de "göçü başlat" der) ya da ADR ile yön istisnası. Önerim: booking çeker.
- **Tahmini efor:** orta-büyük (eşleştirme, upgrade algoritması, bildirim, denetim, test). Eğer isterseniz ilk sürümde sadece "aynı konum eşlemesi + çözülemezse değişikliği reddet" ile başlayıp upgrade/bildirimi sonraya bırakabiliriz (YAGNI).

---
## 5. Auth / JWT Akışı

> Durum: TASLAK (onay bekliyor). Kod yok. Dayanak: rules.md §5.6, §6.6, §7; K3 (roller), §3.3 (servisler JWT'yi kendileri doğrular), §3.5 (gateway rotaları).

### 5.1 Token modeli
| | Access token | Refresh token |
|---|---|---|
| Biçim | JWT (imzalı) | Opak rastgele değer (256 bit), JWT değil |
| Ömür (K36) | 15 dk | 14 gün |
| İçerik | `sub`=userId, `role`, `iss`, `iat`, `exp`, `jti`. **E-posta/ad gibi kişisel veri yok** | Sunucuda yalnızca **hash'i** (SHA-256) saklanır |
| Saklama (istemci) | **Bellekte** (Angular servisi, `BehaviorSubject`); localStorage'a yazılmaz | **HttpOnly + Secure + SameSite=Strict cookie**, `Path=/api/v1/auth` |
| Üretici/doğrulayıcı | auth üretir; gateway ve servisler doğrular | Yalnızca auth-service |

**İmza algoritması (öneri): asimetrik (RS256).** auth-service private key ile imzalar; gateway ve diğer servisler yalnızca **public key** ile doğrular. Böylece bir servis ele geçirilse bile token üretilemez. Anahtarlar repoya yazılmaz; env/secret olarak mount edilir (rules §5.6). Alternatif HS256 (tek paylaşılan secret): daha basit ama her servis token üretebilir; öneri değil. Anahtar rotasyonu şimdilik kapsam dışı (`kid` alanı JWT başlığına konur, ileride kolaylaşır).

### 5.2 Refresh token rotasyonu (çalınmaya karşı)
- `RefreshToken` (auth_db): id, userId, tokenHash (unique), familyId, expiresAt, revokedAt, replacedById, createdAt.
- Her `refresh` çağrısında eski token iptal edilir, **yeni token verilir** (rotasyon), aynı `familyId` korunur.
- **Yeniden kullanım tespiti:** iptal edilmiş bir refresh token tekrar gelirse aile (family) tümüyle iptal edilir, kullanıcı yeniden giriş yapar. Çalınan token'ın sessizce kullanılmasını engeller.
- Aynı anda iki sekmenin refresh yapması yanlış alarm üretebilir: 10 sn'lik tolerans (grace) penceresi veya istemcide tek refresh (`shareReplay`) ile çözülür; öneri: ikisi birden (istemci tek refresh, sunucuda kısa tolerans).
- Süresi dolmuş refresh kayıtları zamanlanmış job ile silinir (doğruluk job'a bağlı değil, `expiresAt` her zaman kontrol edilir).

### 5.3 Akışlar
| Uç nokta | Erişim | Davranış |
|---|---|---|
| `POST /api/v1/auth/register` | Herkes | USER oluşturur (rol istemciden alınmaz). Başarıda otomatik giriş yapmaz (öneri: login'e yönlendirir). |
| `POST /api/v1/auth/login` | Herkes | Doğrular → access token (gövde) + refresh cookie. Hata mesajı **genel** ("e-posta veya şifre hatalı"). |
| `POST /api/v1/auth/refresh` | Cookie | Rotasyon, yeni access token + yeni cookie. |
| `POST /api/v1/auth/logout` | Cookie | Refresh ailesini iptal eder, cookie'yi siler. Access token süre dolana kadar geçerli kalır (≤15 dk, kabul edilen risk; kara liste yok, YAGNI). |
| `GET /api/v1/auth/me` | USER | userId + role + (e-posta) döner; Angular oturum durumunu buradan kurar. |

**Sayfa yenilenince:** access token bellekte olduğu için kaybolur; Angular açılışta (`APP_INITIALIZER`) `refresh` çağırıp oturumu sessizce geri kurar. Başarısızsa misafir olarak devam eder (K3).

### 5.4 Gateway ve servislerde doğrulama (defense in depth)
1. **Gateway:** JWT imza/süre ön doğrulaması, rota bazlı rol kontrolü (§3.5: `/admin/**` → ADMIN, `/bookings/**` → USER), rate limit, CORS (izinli origin listesi, `*` yok), correlation-id. `Authorization` başlığını **değiştirmeden** iletir.
2. **Her servis:** JWT'yi `sinema-common`'daki ortak doğrulayıcı ile (`OncePerRequestFilter`, STATELESS) yeniden doğrular ve yetkiyi `@PreAuthorize` ile uygular. Gateway atlanırsa (iç ağdan doğrudan çağrı) bile servis korumalıdır.
3. Servisler kullanıcıyı yalnızca `userId`/`role` ile tanır; auth-service'e çağrı yok (§3.3).
4. **Rol değişikliği** en geç access token süresi (15 dk) sonra yansır; kabul edilir.

### 5.5 Servisler arası (sistem) kimlik doğrulama
Kullanıcı JWT'si olmayan çağrılar var: iyzico callback'i ve `payment → booking` "onayla" çağrısı, booking'in catalog'dan iç veri çekmesi (K35 göçü dahil).
- **Karar (K36): `X-Internal-Key` yeterli değil; servisler arası çağrılar da Bearer JWT taşır (servis JWT'si).**
- **Mekanizma (client-credentials benzeri, varsayım):** her servisin env/secret ile verilen `clientId` + `clientSecret` değeri vardır. Servis, auth-service'in `POST /internal/v1/auth/service-token` ucundan kısa ömürlü (5 dk) RS256 JWT alır: `sub`=servis adı, `role`=`SERVICE`, `aud`=çağrılacak servis. Token süre dolana kadar önbellekte tutulur; Feign `RequestInterceptor` `Authorization: Bearer` olarak ekler. Alıcı servis aynı ortak doğrulayıcıyla imza, süre ve `aud`/`SERVICE` rolünü kontrol eder; `/internal/v1/**` uçları yalnızca `SERVICE` rolüne açıktır.
- **Etkisi:** §3.3'teki "auth ↔ diğerleri çağrı yok" kuralına tek istisna eklenir: servisler auth'tan yalnızca servis token'ı alır (auth kimseyi çağırmaz, döngü oluşmaz). Auth-service ayrıca servis başına yetkiyi sınırlar (örn. yalnızca payment, booking'in "onayla" ucunu çağırabilir: `aud` + izinli servis eşlemesi). Bu istisna ADR ile kayda geçer.
- `/internal/v1/**` yolları yine **gateway'den hiç yönlendirilmez** (ek katman).
- Kullanıcı bağlamı gereken Feign çağrılarında (örn. booking → catalog seans bilgisi) kullanıcı JWT'si iletilmez; catalog'un bu verisi halka açıktır, iç uçlar servis JWT'siyle çağrılır.
- iyzico callback'i `/api/v1/payments/callback/**` (§3.5) kullanıcı JWT'si taşımaz; güvence iyzico imza/token doğrulaması + payment-service'in sağlayıcıdan sonucu **tekrar sorgulaması**dır (Adım 7'de ayrıntı).

### 5.6 Parola, kayıt ve kötüye kullanım
- Hash: **BCrypt** (maliyet 12, konfigürasyonda). Parola politikası: en az 8 karakter, en çok 72 (BCrypt sınırı), sızmış parola listesi kontrolü yok (YAGNI).
- E-posta benzersiz, küçük harfe normalize edilir.
- **Brute force:** gateway'de `/auth/login` ve `/auth/register` için sıkı rate limit (IP) + hesap bazlı geçici kilit (öneri: 5 hatalı denemede 5 dk). Kayıt sırasında "e-posta kayıtlı" hatası hesap varlığını sızdırır; ilk sürümde kabul edilir (e-posta doğrulama altyapısı yok).
- **ADMIN** hesabı herkese açık kayıtla **oluşturulamaz**: ilk admin `.env` değişkenleriyle (e-posta + parola; yalnızca ADMIN yoksa açılışta bir kez oluşturulur, `.env` repoya girmez, K36) tanımlanır, sonraki adminleri mevcut admin ekler (ADMIN yetkili uç nokta).
- Hassas veri (parola, token, refresh) loglanmaz; hata cevaplarında stack trace yok.

### 5.7 Angular 13 tarafı (Adım 8'e girdi)
`AuthService` (BehaviorSubject: `accessToken$`, `currentUser$`), class-based `AuthInterceptor` (Bearer ekler; 401'de **tek seferlik** refresh dener, `exhaustMap`/paylaşılan refresh ile, başarısızsa logout), `AuthGuard`, `RoleGuard`. Token bellekte olduğu için XSS ile çalınma yüzeyi küçülür; refresh cookie JS'den okunamaz.

### 5.8 Test planı
1. Geçerli/süresi dolmuş/imzası bozuk/yanlış `iss` token → doğru 200/401.
2. Rol yetkisi: USER admin rotasında 403; misafir 401.
3. Refresh rotasyonu: eski token ikinci kez kullanılınca tüm aile iptal.
4. Aynı anda iki refresh isteği → tolerans penceresi sınırları.
5. Logout sonrası refresh reddedilir.
6. Brute force: 5 hatalı giriş → kilit; kilit süresi sonunda açılır.
7. `/internal/**` dış rotadan erişilemez; servis JWT'si olmayan, süresi dolmuş, yanlış `aud` veya `USER` rollü token'la iç çağrı reddedilir; yanlış `clientSecret` ile servis token'ı alınamaz.
8. Register ile rol yükseltme (`role=ADMIN` gönderimi) yok sayılır.

### 5.9 Cevaplar (2026-10-27) → K36
RS256; access 15 dk, refresh 14 gün; refresh HttpOnly cookie, access bellekte; hesap kilidi + IP rate limit yeterli; e-posta doğrulama ve "şifremi unuttum" **devre dışı**; servisler arası Bearer (servis) JWT, `X-Internal-Key` yok; ilk ADMIN `.env` ile oluşturulur. Kalan varsayım: servis JWT'sinin auth-service'ten client-credentials ile alınması (§5.5).

Eski sorular (referans):
1. **RS256 (asimetrik) mi, HS256 mı?** (Önerim RS256.)
2. **Süreler:** access 15 dk, refresh 7 gün uygun mu?
3. **Refresh token cookie'de, access bellekte** (rules önerisiyle uyumlu) uygun mu? Yerel geliştirmede HTTP olduğu için `Secure` bayrağı dev profilinde kapalı olur; Angular dev sunucusu ile gateway aynı origin olmadığından dev'de proxy gerekir (Adım 6).
4. **Hesap kilidi** (5 hata → 5 dk) ve IP rate limit yeterli mi?
5. **E-posta doğrulama ve "şifremi unuttum"** kapsam dışı kalsın mı? (E-posta altyapısı yok; K35'te de e-posta kullanılmadı.)
6. **Servisler arası kimlik:** paylaşılan `X-Internal-Key` + gateway'de `/internal/**` yok yeterli mi?
7. **İlk ADMIN** env ile tohumlansın mı?

---
## 6. Monorepo + Docker Topolojisi

> Durum: TASLAK (onay bekliyor). Kod yok. Dayanak: rules.md §5.8 (Maven), §5.9 (Docker), §6.2 (nvm); K31 çalışma varsayımları burada kesinleşir; §5.9 dev notu (cookie/proxy).

### 6.1 Repo yapısı
```
sinema/
 ├── pom.xml                 # parent POM: Java 21, BOM'lar (Spring Boot, Spring Cloud), merkezi sürümler
 ├── sinema-common/          # hata formatı, correlation-id, JWT doğrulama yardımcıları (iş mantığı yok)
 ├── services/
 │    ├── api-gateway/  auth-service/  catalog-service/  booking-service/  payment-service/
 ├── frontend/               # Angular 13, .nvmrc (Node 16.x)
 ├── docker/mysql/init/      # şema + kullanıcı oluşturma SQL'i
 ├── docker-compose.yml
 ├── .env.example            # değer içermez, .env ve anahtar dosyaları .gitignore'da
 ├── docs/adr/               # kısa karar kayıtları (servis token istisnası vb.)
 └── planlama.md, memory-bank/
```
- Maven multi-module: parent `pom.xml` `dependencyManagement` ile tüm sürümleri tutar; servisler parent'ı miras alır, kullanılmayan bağımlılık yok (rules §5.8). Spring Boot / Spring Cloud sürüm eşleşmesi **kodlamadan önce resmi uyumluluk tablosundan doğrulanır**.
- Her servis feature-based paket yapısı (rules §5.2); `frontend/` Maven dışında, kendi npm yaşam döngüsünde.
- **Uyarı (gateway ≠ servlet):** Spring Cloud Gateway'in klasik sürümü reaktif (WebFlux) çalışır; `sinema-common`'daki servlet tabanlı filtre (`OncePerRequestFilter`) gateway'de kullanılamaz. Öneri: `sinema-common` servlet tabanlı kalır (4 servis kullanır), gateway JWT'yi kendi küçük reaktif filtresiyle doğrular; gateway `sinema-common`'ın yalnızca servlet-bağımsız kısmına (sabitler, hata formatı DTO'su) bağımlı olur ya da hiç bağımlı olmaz. Alternatif: Spring Cloud Gateway Server WebMVC (servlet tabanlı) ama daha yeni/az olgun; öneri değil. **Karar (K37): `sinema-common` servlet tabanlı kalır, gateway ona bağımlı olmaz.**

### 6.2 docker-compose topolojisi
```
 tarayıcı ──> frontend (nginx, yalnızca statik Angular)            [origin 1]
 tarayıcı ──> api-gateway (CORS, Redis rate limit) ──> auth / catalog / booking / payment   [origin 2]
                                                                                          └──> mysql (4 şema, 4 kullanıcı)
```
| Container | Dışarı açık port | Not |
|---|---|---|
| frontend (nginx) | 80 | Yalnızca Angular build'ini servis eder; API'ye proxy yapmaz. |
| api-gateway | 8080 (tek API giriş noktası) | Tarayıcı doğrudan buraya gelir (ayrı origin): CORS, JWT ön doğrulama, rate limit. |
| auth / catalog / booking / payment | **yok** | Yalnızca iç ağ; `/internal/**` zaten gateway'den yönlendirilmez (§5.5). |
| mysql | yok (dev'de `127.0.0.1:3306` isteğe bağlı) | Tek instance. |
| redis | yok | Yalnızca gateway rate limit sayaçları (K37). Sabit tag, healthcheck, parola `.env`'den. Hold kilidi veya koltuk durumu için **kullanılmaz** (gerçek kaynak MySQL, §4.2-C reddedildi). |
- Tek özel bridge ağı; servisler birbirini **Docker DNS adıyla** çağırır (`http://booking-service:8080`), Feign adresleri env ile (K31, Eureka yok).
- `depends_on` + `condition: service_healthy`; her container için healthcheck (Actuator `/actuator/health`, MySQL için `mysqladmin ping`).
- Image'lar sabit tag (`latest` yok); servis Dockerfile'ları **multi-stage** (Maven build → küçük JRE 21 imajı), **non-root** kullanıcı, `.dockerignore`, secret imaja gömülmez. Tam tag'ler kodlama sırasında doğrulanır.
- JVM `-Duser.timezone=UTC`; MySQL bağlantısı UTC (§2.4-9).

### 6.3 Veritabanı
- Tek MySQL (8.4 LTS, sabit tag), şemalar: `auth_db`, `catalog_db`, `booking_db`, `payment_db`. `docker/mysql/init/*.sql` ilk açılışta şemaları ve **servis başına kullanıcıyı** oluşturur; her kullanıcı yalnızca kendi şemasında yetkili (K31, rules §5.5). Şifreler `.env`'den gelir, SQL dosyasında düz yazılmaz (init script env'den üretir).
- Şema yönetimi: **Flyway önerisi** (sade SQL migration, `db/migration` her servis içinde; MySQL için `flyway-mysql` modülü gerekir). `ddl-auto=validate`; prod'da `update/create` yasak (rules §5.5). Alternatif Liquibase: daha esnek (XML/YAML) ama bu proje için gereksiz karmaşık.
- Testcontainers aynı MySQL sürümünü kullanır (§4.10 concurrency testleri gerçek MySQL ister).
- K9'un `UNIQUE(showtimeId, layoutCellId)` kısıtı ve K35 denetim tablosu ilk migration'larda yer alır (Adım 7'den sonra netleşir).

### 6.4 Konfigürasyon ve secret yönetimi
- `application.yml` + `@ConfigurationProperties`; profiller `dev`, `test`, `prod` (rules §5.4). Docker'da `SPRING_PROFILES_ACTIVE` env ile.
- `.env` (repoya girmez) + `.env.example` (anahtar adları, değersiz). Başlıca değişkenler:
  - MySQL: root ve 4 servis kullanıcısı parolaları
  - JWT: private key dosya yolu (**yalnızca auth-service'e mount**), public key dosya yolu (diğer 4 servise mount); anahtar çifti repo dışında bir betikle üretilir
  - Servis kimlik bilgileri: servis başına `SERVICE_CLIENT_ID` / `SERVICE_CLIENT_SECRET` (§5.5)
  - `ADMIN_EMAIL`, `ADMIN_PASSWORD` (ilk admin, K36)
  - `TMDB_API_KEY`, iyzico sandbox `API_KEY` / `SECRET_KEY` / base URL
  - Konfigüre edilebilir iş değerleri: hold süresi, ödeme penceresi (+2 dk), satış kapanış ofseti, polling aralığı, K29 eşikleri (değerler koda gömülmez)
- Dosya mount'ları salt okunur (`:ro`).

### 6.5 Geliştirme akışı (yerel)
- Backend: `docker compose up --build` tüm yığını kaldırır. Tek servis geliştirirken o servis IDE'de çalışır, MySQL compose'dan gelir (`127.0.0.1:3306`).
- Frontend: `nvm use && npm start` (Angular dev sunucusu, `localhost:4200`) gateway'e (`localhost:8080`) doğrudan gider; proxy yok (K37). `Secure` cookie bayrağı yalnızca `dev` profilinde kapalıdır (HTTP olduğu için, §5.9).
- **Cookie ve ayrı origin:** `localhost` üzerinde farklı portlar aynı *site* sayılır, bu yüzden dev'de `SameSite=Strict` refresh cookie çalışır. Prod'da frontend ve gateway **aynı kayıtlı alan adı altında** olmalıdır (örn. `app.ornek.com` ve `api.ornek.com`); tamamen farklı sitelerde `Strict` cookie gitmez ve `SameSite=None; Secure` gerekir (CSRF riski artar). İstemci auth çağrılarında `withCredentials: true` kullanır.
- README'de kurulum/çalıştırma/env tablosu bulunur (rules §9).

### 6.6 Gateway ayrıntıları (K31 varsayımlarının kesinleşmesi)
- **Spring Cloud Gateway** (reaktif) — yeni bağımlılık, onay gerekir.
- **Rate limit (K37): Redis ile.** Spring Cloud Gateway'in hazır `RequestRateLimiter`'ı (Redis tabanlı, çoklu gateway instance'ında da doğru). Genel IP sınırı + `/auth/login`, `/auth/register`, `/tickets/{kod}` için sıkı sınırlar (route bazlı). K29 bu mekanizmayı kullanmaz, booking içinde kalır (§3.4-3). Redis erişilemezse davranışın (isteği geçir / reddet) güvenlik açısından seçilmesi gerekir (§6.11-2).
- **CORS (ayrı origin):** gateway'de izinli origin listesi konfigürasyondan (`*` yok), `Access-Control-Allow-Credentials: true`, izinli başlıklar `Authorization`, `Content-Type`, correlation-id. Cookie'li `refresh`/`logout` uçları ek olarak `Origin` başlığını izinli listeye karşı doğrular (CSRF savunması, `SameSite` ile birlikte).
- Correlation-id gateway'de üretilir, Feign `RequestInterceptor` ile iletilir (rules §5.7).

### 6.7 Gözlemlenebilirlik
Her serviste Actuator: yalnızca `health`, `info`, `metrics` ve yalnızca iç ağdan; log'lar stdout (JSON, correlation-id'li), parola/token loglanmaz. Prometheus/Grafana kapsam dışı (YAGNI).

### 6.8 iyzico callback erişimi (K12'den devreden açık nokta)
Sandbox akışında sonucun nasıl döndüğü (tarayıcı yönlendirmesi mi, sunucudan sunucuya bildirim mi) **resmi dokümandan doğrulanmadan karar verilmez**. Tarayıcı yönlendirmesiyse `localhost` üzerinden de çalışır (frontend → gateway → payment). Sunucudan sunucuya geliyorsa yerelde dışarıdan erişilebilir bir tünel (örn. ngrok/cloudflared) gerekir; bu **yeni dış araç → onay gerektirir**. Karar, Adım 7'de iyzico akışı netleşince verilir.

### 6.9 Test planı
1. `docker compose up` sonrası tüm container'lar `healthy`; gateway üzerinden her servise örnek rota 200/401 doğru.
2. DB izolasyonu: `catalog` kullanıcısı `booking_db`'yi okuyamaz.
3. Dışarıdan yalnızca frontend portuna erişilebilir; servis/DB portları kapalı.
4. Boş veritabanında Flyway migration'ları sorunsuz; `ddl-auto=validate` geçer.
5. `.env` olmadan kalkış anlamlı hata verir (fail fast); secret imaja girmemiştir (`docker history` kontrolü).

### 6.11 Cevaplar (2026-10-27) → K37 ve kalan sorular
Cevaplar: 1 repo yapısı uygun; 2 tek origin olmasın; 4 Flyway; 5 Redis; 6 servlet kalsın; 7 `git init` yapılsın; 8 önce doküman. **3. soru (bağımlılık onayları) cevaplanmadı.**
Kalan:
1. **Bağımlılık onayları (3. soru):** Spring Cloud Gateway; Redis için `spring-boot-starter-data-redis-reactive` (gateway rate limiter için gerekli); JWT için `spring-boot-starter-oauth2-resource-server` (Nimbus dahil, servlet + reaktif destekler; önerim) veya jjwt; Flyway (+`flyway-mysql`, onaylandı); MapStruct; Lombok; springdoc; Testcontainers.
2. **Redis erişilemezse gateway ne yapsın?** Öneri: genel sınırda isteği geçir (fail-open, kullanılabilirlik), `/auth/login` ve `/auth/register` için reddet (fail-closed). Spring'in varsayılan davranışı kodlamadan önce dokümandan doğrulanacak.
3. **Prod alan adı:** frontend ve gateway aynı kayıtlı alan adı altında mı olacak (`SameSite=Strict` için)? Şimdilik yalnızca yerel/demo olduğu için varsayım: evet.

### 6.10 Açık sorular / onay gerekenler (ilk taslak, referans)
1. **Repo yapısı** (§6.1) uygun mu? `docker-compose.yml` ve `frontend/` kökte.
2. **Frontend nginx container'ı** ile tek origin yaklaşımı uygun mu? (nginx yeni bir bileşen ama yalnızca statik dosya + proxy; Angular için standart.)
3. **Yeni bağımlılık onayları (kodlamadan önce):** Spring Cloud Gateway; JWT doğrulama/imzalama için `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE dahil, Spring ekosistemi) **veya** ayrı bir JWT kütüphanesi (jjwt); Flyway (+`flyway-mysql`); MapStruct; Lombok (`@RequiredArgsConstructor` kuralı); springdoc; Testcontainers. Hangileri onaylı?
4. **Flyway mi Liquibase mi?** (Önerim Flyway.)
5. **Gateway rate limit:** Resilience4j ile bellek içi (önerim) uygun mu?
6. **Gateway reaktif** kalıp `sinema-common`'ı servlet tabanlı tutmak (§6.1 uyarısı) uygun mu?
7. **`git init`** ve `.gitignore` (`.env`, anahtarlar, `node_modules`, `target`) kodlamaya geçmeden önce yapılsın mı? Conventional Commits (rules §9) ile.
8. **iyzico tünel aracı** gerekirse onay verilir mi, yoksa önce doküman mı doğrulansın? (Önerim: önce doküman.)

---
## 7. API Sözleşmeleri

> Durum: BAŞLADI. Önce dış API doğrulamaları (K37: "önce doküman"); uç nokta sözleşmeleri sonraki taslakta.

### 7.1 iyzico Checkout Form doğrulama notları (2026-10-27, resmi dokümandan; kısmen doğrulandı)
Kaynaklar: docs.iyzico.com (CF Implementation / Initialize / Webhook).
- **Akış:** `initialize` → yanıtta `token` + `paymentPageUrl`; token saklanır. Kullanıcı ödeme sayfasına yönlendirilir (iframe için `&iframe=true`). Ödeme sonrası iyzico `callbackUrl`'i tetikler; sonuç **token ile `retrieve`** çağrılarak alınır, başarı `paymentStatus = SUCCESS`.
- **İş hatası HTTP 200 ile gelebilir:** gövdede `status: "failure"` kontrol edilmeli; yalnızca HTTP koduna güvenilmez.
- **Sandbox adresi:** `https://sandbox-api.iyzipay.com` (SDK örneğinden; resmi sayfadan teyit edilecek).
- **Webhook:** ayrıca sunucudan sunucuya bildirim var; ilk bildirim ~10–15 sn sonra, 2xx alınana dek 15 dk arayla yeniden denenir (en fazla 3 deneme, kaynak: webhook sayfası özeti). `X-Iyz-Signature-V3` imzası **hesapta özelliğin açılmasını** (iyzico entegrasyon ekibine başvuru) gerektirir; imza formülü resmi sayfadan ayrıca doğrulanmalı (üçüncü taraf kaynakta HMAC-SHA256 olarak geçiyor).
- **Callback (cf-retrieve sayfasından doğrulandı):** iyzico **müşterinin tarayıcısını** `callbackUrl`'e yönlendirir ve `token` değerini **POST** eder. Yani sunucudan sunucuya çağrı değil; `localhost` yeterlidir. Sonuç `POST …/payment/iyzipos/checkoutform/auth/ecom/detail` (`token`, isteğe bağlı `locale`, `conversationId`; `Authorization: IYZWSv2 …` imzalı başlık) ile alınır. Yanıttaki `paymentStatus`, `paidPrice`, `price`, `basketId`, `conversationId`, `paymentId`, `fraudStatus` (yalnızca değer **1** ise teslim edilir) ve `signature` alanları kontrol edilir.
- **Hâlâ doğrulanamayanlar:** token ömrü ve tekrar kullanılıp kullanılamayacağı; yanıt `signature` alanının hesaplanma biçimi. Kodlamadan önce `response-signature-validation` sayfasından kapatılacak.

**Mimariye etkisi (ön değerlendirme):**
1. Güvence, callback'in kendisine değil **`retrieve` sonucuna** dayanmalı: callback gelince payment-service token ile iyzico'dan sonucu sorgular, tutar ve conversationId'yi kendi kaydıyla karşılaştırır. Böylece sahte callback işe yaramaz (§4.8, §5.5).
2. **Karar (K39): yalnızca yerel kullanım; tarayıcı yönlendirmeli callback + `retrieve` kullanılır, webhook ve tünel aracı kullanılmaz.** Webhook'un imzası hesapta özellik açılmasını ve dışarıdan erişilebilir adres ister; ikisi de kapsam dışı. Tarayıcı kapanırsa/callback'e hiç gelmezse ödeme durumu, payment-service'in bekleyen ödemeleri `retrieve` ile periyodik sorgulaması (mutabakat job'ı, §4.8) ile kapatılır.
   - Callback adresi gateway üzerinden `POST /api/v1/payments/callback/iyzico` olur; JWT taşımaz (üçüncü taraf sayfadan gelen form POST'u), kullanıcıyı token→ödeme→rezervasyon eşlemesi tanır. Cevap, frontend'in ödeme sonuç sayfasına **yönlendirme (redirect)** olur. CORS bu istek için geçerli değildir (üst düzey navigasyon).
3. Tekrarlayan callback/webhook idempotent işlenir (token benzersiz, §4.8).

Webhook sandbox'ta çalışıyor mu dokümandan belli değil; K39 gereği kullanılmıyor (isteğe bağlı sonraki iş).

### 7.2 Ortak kurallar (TASLAK)
- **Taban yol / sürüm:** `/api/v1/...`; sözleşme değişiklikleri geriye uyumlu (yalnızca alan ekleme); kırıcı değişiklik `/v2`.
- **Biçim:** JSON, UTF-8. Zamanlar UTC ISO-8601 (`2026-11-01T17:00:00Z`), ekranda Europe/Istanbul (§2.4-9). **Para birimi string** (`"120.00"`, para birimi TRY) → kayan nokta kaybı yok. Enum'lar string.
- **Kimlikler:** varlık id'leri sayısal (`Long`); rezervasyon doğrulama kodu ayrı, rastgele ve tahmin edilemez (K30). Sahibi olunmayan kayıt **404** döner (var olduğu sızdırılmaz).
- **Başlıklar:** `Authorization: Bearer`, `X-Correlation-Id` (gelen/giden). Saat sorunları için kritik cevaplarda sunucu `serverTime` döner (§4.3).
- **Hata formatı** (RFC 7807 `application/problem+json`, `sinema-common`): `type`, `title`, `status`, `detail`, `code` (makine okunur, örn. `SEATS_UNAVAILABLE`), `correlationId`, `timestamp`, doğrulama hatalarında `errors[{field, message}]`, ilgili hatalarda ek alanlar (`unavailableCellIds`, `reservationId`). Stack trace ve iç ayrıntı yok.
- **Durum kodları:** 400 doğrulama, 401 kimlik yok/geçersiz, 403 yetki yok, 404 yok/sahibi değil, 409 çakışma (koltuk dolu, aktif rezervasyon var, seans çakışması), 422 iş kuralı ihlali (satış kapalı), 429 (`Retry-After`), 502/503/504 bağımlı servis (Feign/Resilience4j fallback) — fail fast.
- **Sayfalama:** büyük listelerde zorunlu: `?page=0&size=20` (en çok 50), `sort` yalnızca izin listesindeki alanlar; cevap `{content, page, size, totalElements, totalPages}`.
- **Dokümantasyon:** her serviste springdoc (`/v3/api-docs`), `Idempotency-Key` başlığı **yok** (YAGNI): koltuk tutma "tek aktif rezervasyon" kuralıyla, ödeme başlatma "rezervasyon başına tek ödeme" ile zaten idempotent.
- **Feign DTO'ları** servisin iç modelinden ayrı tutulur (rules §5.7); entity API'ye çıkmaz.

### 7.3 auth-service (`/api/v1/auth`)
| Uç | Erişim | İstek → Cevap |
|---|---|---|
| `POST /register` | Herkes | `{email, password}` → 201 `{id, email}` (rol sunucuda USER; ad/soyad alanı yok, K40) |
| `POST /login` | Herkes | `{email, password}` → 200 `{accessToken, expiresInSeconds}` + `Set-Cookie` refresh. Hata: 401 genel mesaj; kilit: 429 `Retry-After` |
| `POST /refresh` | Cookie | → 200 yeni `accessToken` + yeni cookie; yeniden kullanım tespitinde 401 ve aile iptal |
| `POST /logout` | Cookie | → 204, cookie silinir |
| `GET /me` | USER/ADMIN | → `{id, email, role}` |
| `POST /admin/auth/admins` | ADMIN | `{email, password}` → 201 (yeni admin, K36) |
| `POST /internal/v1/auth/service-token` | Servis | `{clientId, clientSecret, audience}` → `{accessToken, expiresInSeconds}` (gateway'den yönlendirilmez, §5.5) |

### 7.4 catalog-service
**Halka açık (GET, GUEST):** `/api/v1/catalog/...`
| Uç | Cevap |
|---|---|
| `GET /cinemas` | Şubeler ve salonları (ad, adres; salon varsayılan fiyatı **gösterilmez**, fiyat seansta) |
| `GET /movies?page&size&genre&q` | Film özet listesi (afiş, tür, süre, yaş sınırı, TMDB puanı) |
| `GET /movies/{id}` | Tüm film alanları (K16; oyuncu, yönetmen, akıllı işaretler, TMDB atfı için kaynak bilgisi) |
| `GET /movies/{id}/showtimes?date&cinemaId` | Seanslar: `id, startsAt, endsAt, cinemaName, hallName, ticketPrice, salesOpen` |
| `GET /showtimes/{id}` | Seans ayrıntısı + `salesCloseAt` (başlangıç + ofset, K10) |
| `GET /showtimes/{id}/layout` | Mühürlü düzen sürümü: `gridRows, gridCols, cells[{cellId, row, col, type, seatRowLabel, seatNumber, seatType}]`. Sürüm değişmez olduğundan `Cache-Control` uzun + `ETag` |

**Admin (ADMIN)**: `/api/v1/admin/catalog/...`
| Uç | Amaç |
|---|---|
| `GET /tmdb/search?query=` , `GET /tmdb/movies/{tmdbId}` | TMDB arama ve form ön doldurma (kaydetmez, K18). TMDB erişilemezse 503 + elle giriş çalışır |
| `POST /movies`, `PUT /movies/{id}` | Film oluştur/güncelle (`tmdbId` benzersiz) |
| `PATCH /movies/{id}/visibility` | `{visible: true\|false}` — film **silinmez, gizlenir** (K40). Gizli film halka açık listelerde görünmez ve yeni seans eklenemez; mevcut seanslar ve satılmış biletler/doğrulama etkilenmez |
| `POST /cinemas`, `PUT /cinemas/{id}`, `POST /halls`, `PUT /halls/{id}` | Şube/salon; salon `defaultTicketPrice` (K23) |
| `POST /halls/{id}/layouts` | **Taslak** düzen oluşturur (hücreler; doğrulama: ≥1 `SEAT`, `SCREEN` var, ızgara sınırları). Taslak düzenlenebilir (`PUT`), yayımlanınca değişmez (§2.4-6) |
| `GET /halls/{id}/layouts` | Sürüm listesi (aktif işaretli) |
| `POST /showtimes` | `{movieId, hallId, startsAt, ticketPrice?, salesCloseOffsetMinutes?}`; `layoutId` salonun aktif sürümünden, `endsAt` = başlangıç + süre + ara payı (K26), fiyat boşsa salon varsayılanı. Çakışma → 409 |
| `PUT /showtimes/{id}` | Fiyat ve ofset güncelleme. **Silme/iptal yok** (K34) |

### 7.5 booking-service
| Uç | Erişim | Davranış |
|---|---|---|
| `GET /api/v1/bookings/showtimes/{id}/seat-status` | **Herkes** (K3, §4.9) | `{serverTime, occupied:[{cellId, status: HELD\|SOLD}]}`; kimin tuttuğu yok; `Cache-Control: no-store` |
| `POST /api/v1/bookings/reservations` | USER | `{showtimeId, seatCellIds[1..5]}` → 201 `{reservationId, status: PENDING_PAYMENT, expiresAt, serverTime, seats[{cellId, label}], totalAmount}` |
| `GET /api/v1/bookings/reservations/{id}` | Sahibi | Durum, `expiresAt`, koltuklar, tutar; `CONFIRMED` ise `ticketCode` + bilet görüntü verisi (snapshot, §3.4-1). Ödeme sonrası frontend bunu yoklar |
| `GET /api/v1/bookings/reservations?page&size` | USER | Kendi rezervasyonları |
| `DELETE /api/v1/bookings/reservations/{id}` | Sahibi | Yalnızca `PENDING_PAYMENT` hold'u bırakır (K40): koltuklar hemen serbest, rezervasyon `RELEASED`; `CONFIRMED` için 409 (K6). Ödeme başlatılmışsa (iyzico sayfasındayken) 409 `PAYMENT_IN_PROGRESS`. Terk sayacına etkisi: K40 |
| `GET /api/v1/bookings/notifications`, `POST …/{id}/read` | USER | Uygulama içi bildirimler (K35: koltuğunuz değişti) |
| `GET /api/v1/tickets/{kod}` | **Herkes**, rate limit | Geçerli (`CONFIRMED`) ise `{valid:true, movieTitle, cinemaName, hallName, startsAt, seats[labels]}`; aksi halde **404** (bilinmeyen/bekleyen/süresi dolmuş ayırt edilmez). Kişisel veri yok (K30) |

**Hold hataları:** 409 `SEATS_UNAVAILABLE` (+`unavailableCellIds`), 409 `ACTIVE_RESERVATION_EXISTS` (+`reservationId`), 429 `HOLD_COOLDOWN` (+`Retry-After`, K29), 422 `SALES_CLOSED`, 400 geçersiz/`SEAT` olmayan hücre veya 5'ten fazla koltuk, 503 catalog erişilemiyor.

**Admin:** `POST /api/v1/admin/bookings/layout-migrations/preview` ve `POST /api/v1/admin/bookings/layout-migrations` (K35, bkz. §7.8).

**Internal (`/internal/v1/bookings`, yalnızca servis JWT'si):** `GET /reservations/{id}/payment-info` (tutar, durum, sahip `userId`), `POST /reservations/{id}/extend-payment-window` (K32: bir kez +2 dk), `POST /reservations/{id}/confirm` (`{paymentId}`, idempotent, §4.5), `POST /reservations/{id}/fail`.

### 7.6 payment-service
| Uç | Erişim | Davranış |
|---|---|---|
| `POST /api/v1/payments` | USER | `{reservationId}` → booking'den tutar/sahip/durum alınır (istemci tutar gönderemez), hold bir kez uzatılır (K32), iyzico `initialize` → 201 `{paymentId, paymentPageUrl}`. Aynı rezervasyonda geçerli ödeme varsa aynısı döner |
| `POST /api/v1/payments/callback/iyzico` | Herkes (iyzico tarayıcı yönlendirmesi) | Form-urlencoded `token` → `retrieve` ile sonuç doğrulanır (tutar, `conversationId`, `fraudStatus`), booking `confirm`/`fail` çağrılır → **302** frontend `/odeme/sonuc?reservationId=…`; tekrar gelen callback aynı sonucu üretir |
| `GET /api/v1/payments/by-reservation/{reservationId}` | Sahibi | Ödeme durumu (`INITIATED/PENDING/SUCCEEDED/FAILED`) |

### 7.7 Gateway rota düzeltmesi (§3.5'e fark)
- `GET /api/v1/bookings/showtimes/{id}/seat-status` **herkese açık** olmalı (K3: misafir doluluğu görür); §3.5'teki `/api/v1/bookings/**` → USER kuralına istisna.
- `/api/v1/auth/admin/**` değil, `/api/v1/admin/auth/**` ADMIN rotasıdır.
- `/internal/**` hiçbir rotada yok (§5.5).

### 7.8 Düzen yayımlama ve bilet göçü akışı (K35 – önerilen orkestrasyon)
Sorun: düzen ve seans catalog'da, koltuk kayıtları booking'de; catalog→booking çağrısı yasak (§3.3). Göç iki servisi birlikte değiştirir.
1. Admin catalog'da **taslak** düzeni hazırlar (§7.4).
2. Admin booking'de `preview` çağırır: booking, catalog'dan (internal) hedef taslak ile gelecekteki seansların mevcut düzenini okur, eşleştirme/yedek koltuk planını **kaydetmeden** hesaplar; çözülemeyen bilet varsa rapor döner (K35: değişiklik reddedilir).
3. Admin `POST …/layout-migrations` ile uygular: booking, salona ait gelecek seanslarda bakım kilidi açar, koltukları tek transaction'da taşır (sıralı, §4.3) ve bildirimleri yazar.
4. Booking, catalog'un internal ucunu çağırır: `POST /internal/v1/catalog/halls/{id}/layouts/{layoutId}/activate` — catalog düzeni yayımlar, salonun aktif sürümünü ve başlamamış seansların `layoutId`'sini tek transaction'da günceller (K13 kontrolü burada).
5. Adım 4 başarısız olursa booking göçü geri alır (telafi); kilit kalkar. Yön hep booking→catalog kalır.

### 7.9 Cevaplar (2026-10-27) → K40
1 → kullanıcı hold'unu bırakabilir; 2 → seat-status misafire açık (misafirin tıklaması hold oluşturmaz, yalnızca dolu/boş görür); 3 → §7.8 orkestrasyonu uygun; 4 → para string, id Long, geçersiz bilet 404 uygun; 5 → film silinmez, gizlenir; 6 → ad/soyad/e-posta bilette yok, kayıtta `displayName` yok.
**Varsayım (itiraz yoksa geçerli):** bırakılan hold K29 sayacına **dahil** edilir (hold-bırak sabotajı K29'un hedefi). `RELEASED` durumu `EXPIRED` gibi sayılır: 30 dk içinde 3 terk → 10 dk bekleme.

Eski sorular (referans):
1. **Kullanıcı kendi hold'unu bırakabilsin mi?** (`DELETE /reservations/{id}`, yalnızca `PENDING_PAYMENT`). Aksi halde koltuk değiştirmek isteyen 3 dk beklemek zorunda. Önerim: evet ve **K29 sayacına dahil** (hold-bırak sabotajı K29'un tam hedefi).
2. **seat-status misafire açık** (§7.7) uygun mu?
3. **§7.8 orkestrasyonu** (booking yönetir, catalog yalnızca `activate` uygular) uygun mu?
4. **Para string, id Long, geçersiz bilet 404** kararları uygun mu?
5. **Film silme** (seansı yoksa) kalsın mı, yoksa film silinmesin, sadece gizlensin mi?
6. **Kayıt alanları:** `displayName` gerekli mi (bilette gösterilmiyor, K30)? Gerekmiyorsa yalnızca e-posta + parola.

---
## 8. Angular 13 Planı

> Durum: TASLAK (onay bekliyor). Kod yok. Dayanak: rules.md §6 (Angular 13 kısıtları), §5 (auth), §7 (API), K3, K18, K30, K36, K37.

### 8.1 Temel kurulum
- Angular CLI 13, **strict mod açık**, SCSS, `.nvmrc` = Node 16.x (rules §6.2). NgModule + class-based guard/interceptor, `*ngIf/*ngFor`, RxJS (Signals, standalone, `inject()`, typed forms yok).
- Bootstrap 5 (npm, CSS + grid/utility) + **`ng-bootstrap`** (modal, dropdown, toast, pagination, tooltip gibi hazır bileşenler; Bootstrap'in kendi JS'i ve jQuery kullanılmaz). Angular 13 ile uyumlu sürüm (12.x) ve peer bağımlılıkları (`@popperjs/core`, muhtemelen `@angular/localize`) kodlamadan önce doğrulanır; ek peer bağımlılıklar gerekirse ayrıca belirtilir. Kullanım `SharedModule` üzerinden, yalnızca gereken `NgbModule` parçaları import edilir (bundle boyutu).
- Tarih/saat: Türkiye UTC+3 sabit olduğundan `DatePipe` ile `'+0300'` zaman dilimi parametresi ve `tr` locale (`registerLocaleData`); Angular 13'ün `DatePipe`'ı IANA adı değil offset/`UTC` kabul eder (doğrulanacak). API'den UTC gelir (§7.2).
- `environment.ts`: `apiBaseUrl` (gateway, ayrı origin K37), polling aralığı ve limitler API/konfigürasyondan okunur, koda gömülmez.

### 8.2 Klasör yapısı (rules §6.3)
```
src/app
 ├── core/       # AuthService, interceptor'lar, guard'lar, ServerClockService, APP_INITIALIZER (yalnızca AppModule import eder)
 ├── shared/     # SharedModule: countdown, loading/hata bileşenleri, tarih pipe'ları, ortak form hata bileşeni
 ├── layout/     # header, footer (TMDB atfı burada), bildirim zili
 └── features/   # her biri lazy-loaded NgModule
      ├── auth/        (login, register)
      ├── catalog/     (film listesi, film detayı, seans seçimi)
      ├── booking/     (koltuk haritası, ödeme öncesi özet, rezervasyonlarım, bilet, bildirimler)
      ├── payment/     (ödeme sonuç sayfası)
      ├── ticket-verify/ (halka açık /bilet/:kod)
      └── admin/       (lazy, RoleGuard: film+TMDB, şube/salon, düzen editörü, seans, düzen göçü, admin ekleme)
```
Her feature: `components/` (dumb: `@Input/@Output`, servis çağırmaz), `containers/` (smart), `services/`, `models/`, routing modülü.

### 8.3 Rotalar
| Yol | Erişim | Not |
|---|---|---|
| `/` , `/filmler`, `/filmler/:id` | Herkes | Liste (sayfalı, tür/arama), detay + seanslar |
| `/seans/:id` | Herkes | Koltuk haritası; misafir yalnızca görüntüler, "Giriş yap" çağrısı çıkar (K3) |
| `/giris`, `/kayit` | Misafir | Giriş yapmışsa ana sayfaya |
| `/rezervasyonlarim`, `/rezervasyonlarim/:id` | USER | Liste ve bilet sayfası (QR) |
| `/odeme/sonuc` | USER | iyzico dönüşü (`?reservationId=`), durumu yoklar |
| `/bilet/:kod` | Herkes | QR doğrulama sayfası (K30), kişisel veri yok |
| `/admin/**` | ADMIN | Lazy modül, `RoleGuard` |

### 8.4 Core: auth ve HTTP
- **AuthService:** özel `BehaviorSubject<AuthState>` + `asObservable()`; access token **bellekte** (K36), localStorage yok. `APP_INITIALIZER` açılışta `POST /auth/refresh` (cookie, `withCredentials: true`) çağırır; başarısız olursa misafir.
- **AuthInterceptor:** `Authorization: Bearer` ekler. 401'de **tek seferlik paylaşılan refresh** (`shareReplay({bufferSize:1, refCount:true})` ile aynı anda gelen 401'ler tek refresh bekler), sonra isteği tekrarlar; refresh başarısızsa logout ve `/giris`. `withCredentials` yalnızca auth uçlarına.
- **ErrorInterceptor:** RFC 7807 cevabını (`code`) kullanıcı mesajına çevirir (tek yerde sözlük); stack/teknik detay gösterilmez; 429'da `Retry-After` gösterilir.
- **Guard'lar:** `AuthGuard`, `RoleGuard`, giriş yapmışı yönlendiren `GuestGuard` (hepsi class-based `CanActivate`).
- **ServerClockService:** cevaplardaki `serverTime` ile istemci farkını (`offset`) tutar; geri sayım buna göre hesaplanır, karar yine sunucudadır (§4.3).

### 8.5 Koltuk haritası (en kritik ekran)
- **Container** `SeatMapPageComponent` (smart): `combineLatest([layout$, seatStatus$, selection$])`.
  - `layout$`: bir kez alınır (`shareReplay`), değişmez sürüm.
  - `seatStatus$`: `timer(0, pollMs)` + `switchMap` + `catchError` (akış kırılmaz); sekme gizliyken durur (`visibilitychange` ile `switchMap`), yeniden görünür olunca hemen yenilenir. Aralık 5 sn, konfigürasyondan (K33).
  - `selection$`: `BehaviorSubject<ReadonlySet<number>>`, en çok 5 (K24); dolu koltuğa tıklanamaz.
- **Dumb** `SeatMapComponent` + `SeatCellComponent`: SCSS grid (satır/sütun düzenden), hücre tipleri `SEAT/EMPTY/ENTRANCE/EXIT/SCREEN/WALL`, `OnPush`, `trackBy`. Durumlar renge ek olarak simge/desenle de ayırt edilir (erişilebilirlik), `aria-label` ("F sırası 7 numara, boş"), klavyeyle gezinme.
- **Seç ve tut:** "Devam" → `POST /reservations` (`exhaustMap`, çift tık engellenir). 409 `SEATS_UNAVAILABLE` → harita hemen yenilenir, "bu koltuk az önce alındı" denir, çakışan koltuklar seçimden çıkar. 409 `ACTIVE_RESERVATION_EXISTS` → mevcut rezervasyona yönlendirir. 429 `HOLD_COOLDOWN` → kalan süre gösterilir.
- **Hold ekranı:** geri sayım bileşeni (`expiresAt` + sunucu offset'i); süre dolunca durum "süre doldu", harita yenilenir. "Vazgeç" → `DELETE /reservations/{id}` (K40). Aktif rezervasyon `ReservationStore` (BehaviorSubject) ile paylaşılır.
- Küçük ekranda harita yatay kaydırılır/ölçeklenir (mobile-first).

### 8.6 Ödeme akışı (K39)
1. "Öde" → `POST /payments` (`exhaustMap`) → `paymentPageUrl`.
2. Tarayıcı **tam sayfa yönlendirilir** (`window.location.assign`; iframe seçeneği ilk sürümde yok, daha basit ve 3DS ile sorunsuz).
3. iyzico → gateway callback'i → 302 `/odeme/sonuc?reservationId=…`.
4. Sonuç sayfası `GET /reservations/{id}` değerini `timer` + `switchMap` ile yoklar (`PENDING_PAYMENT` iken, sınırlı deneme sayısı, sonra "durum kontrol ediliyor" mesajı ve manuel yenile). `CONFIRMED` → bilet sayfasına yönlendirir; `FAILED/EXPIRED` → anlamlı mesaj.

### 8.7 Bilet ve doğrulama (K30)
- Bilet sayfası: film+afiş, şube, salon, tarih/saat, koltuklar, rezervasyon kodu, tutar, QR; yazdırma için basit print SCSS (PDF üretimi yok, YAGNI).
- QR içeriği `${origin}/bilet/${kod}`. QR çizimi için `angularx-qrcode` (Angular 13 uyumlu sürümü kodlamadan önce doğrulanır) — **yeni bağımlılık, onay gerekir**.
- `/bilet/:kod` sayfası halka açık; 404 → "Geçersiz bilet". Kişisel veri gösterilmez.

### 8.8 Admin
- **Film:** TMDB arama → sonuç seç → form dolar (Reactive Forms, tiplenmemiş ama model arayüzleriyle sarılmış) → düzenle → kaydet; gizle/göster (K40). TMDB 503'te elle giriş açık kalır.
- **Şube/salon, seans:** liste + form (çakışma 409 mesajı); fiyat/ofset güncelleme.
- **Düzen editörü (en büyük admin parçası):** ızgara boyutu seç, fırça (`SEAT/EMPTY/ENTRANCE/EXIT/SCREEN/WALL`, koltuk tipi `STANDARD/ACCESSIBLE`), sürükleyerek boyama, satır harfi/numara otomatik etiketleme (elle düzeltme), istemci tarafı doğrulama (asıl doğrulama backend), taslağı kaydet/önizle. Geri alma (undo) ilk sürümde yok (YAGNI). State: editor servisi + `BehaviorSubject`.
- **Düzen göçü:** taslak seç → "Önizle" (rapor: taşınacak, yedek atanacak, çözülemeyen) → çözülemeyen yoksa "Uygula" (§7.8).
- Admin ekleme formu.

### 8.9 State, performans, a11y, TMDB atfı
- State: servis + `BehaviorSubject` (NgRx yok; karmaşıklaşırsa onay istenir, rules §6.4). Observable değişkenler `$` ile biter, template'te `async` pipe, manuel subscribe `takeUntil(destroy$)`.
- Lazy loading, `OnPush`, `trackBy`, ağır işler template dışında.
- TMDB atfı (K18): footer'da TMDB logosu + "Bu ürün TMDB API'sini kullanır ancak TMDB tarafından onaylanmamıştır." metni; film detayında puanın kaynağı "TMDB puanı". Afiş hotlink mi kopya mı kararı (§1.10) şartlar doğrulanınca.
- SCSS: `_variables.scss`, `_mixins.scss`, Bootstrap değişken override, BEM, `!important` yok, ≤3 seviye nesting, sabit renk yok.
- Güvenlik: Angular şablon kaçışı korunur; `innerHTML`/`bypassSecurityTrust*` kullanılmaz; film açıklaması düz metin.

### 8.10 Test planı (Jasmine/Karma)
1. AuthInterceptor: 401 → tek refresh → istek tekrar; eşzamanlı 401'ler tek refresh; refresh başarısız → logout (`HttpClientTestingModule`).
2. Seat status akışı: polling, sekme gizliyken durma, hata sonrası devam (`fakeAsync`/marble).
3. Seçim: 5 koltuk sınırı, dolu koltuğa tıklanamaz, 409 sonrası seçim temizlenir.
4. Countdown: sunucu offset'iyle doğru süre, sıfırda durum değişimi (`fakeAsync`).
5. Ödeme sonuç yoklaması: `PENDING` → `CONFIRMED`, sınır aşımı.
6. Guard'lar: misafir/USER/ADMIN yönlendirmeleri.
7. Düzen editörü: boyama, otomatik etiketleme, doğrulama kuralları.

### 8.11 Cevaplar (2026-10-28) → K41
1 Bootstrap + SCSS uygun (sonradan güncellendi: `ng-bootstrap` da eklenecek); 2 `angularx-qrcode` onaylandı; 3 tam yönlendirme; 4 undo yok; 5 yalnızca Türkçe; 6 hotlink. Hotlink için TMDB görsel adresi (`image.tmdb.org` taban yolu + boyut) saklanır; TMDB şartları kodlamadan önce yine doğrulanır (§1.10).

Eski sorular (referans):
1. **Bootstrap yalnızca CSS** (ng-bootstrap yok, kendi küçük bileşenlerimiz) uygun mu?
2. **`angularx-qrcode`** bağımlılığı onaylanır mı (bilet QR'ı için)? Alternatif: QR'ı backend'de üretmek (kütüphane ekler, §2.7'ye aykırı).
3. **Ödeme sayfası tam yönlendirme** (iframe yok) uygun mu?
4. **Düzen editöründe undo** ilk sürümde olmasın mı?
5. **Yalnızca Türkçe arayüz** (i18n altyapısı yok) uygun mu?
6. **TMDB afişi:** hotlink (TMDB CDN adresi saklanır) mi, kopya mı? (§1.10, şartlar doğrulanınca; önerim hotlink.)

---
## 9. Geliştirme Sırası / Milestone'lar

> Durum: TASLAK (onay bekliyor). **Kod yazımı, kullanıcı açıkça "kodlamaya geçelim" diyene kadar başlamaz** (rules.md proje kuralı). Bu bölüm yalnızca sıradır.

### 9.1 İlkeler
- **Dikey dilimler:** her milestone çalışan, test edilmiş bir parça bırakır; bir sonrakine geçmeden önce Teslim Öncesi Kontrol Listesi (rules §10) geçilir.
- **Önce kritik risk:** K9 (çift satış yok) ve ödeme akışı UI'dan önce backend'de kanıtlanır (Testcontainers + gerçek MySQL).
- **Küçük commit'ler:** Conventional Commits, tek amaçlı; her milestone sonunda `docker compose up` ile duman testi.
- Boyut etiketleri göreli (S/M/L); takvim tahmini yok.

### 9.2 Milestone'lar
| # | Milestone | İçerik | Boyut | Bağımlılık |
|---|---|---|---|---|
| M0 | **İskelet** | Parent POM + `sinema-common`; 5 servis iskeleti (Actuator health); docker-compose (MySQL, Redis, servisler, healthcheck'ler); MySQL init (4 şema/kullanıcı); `.env.example`; JWT anahtar çifti üretim betiği; Flyway altyapısı; README | M | — |
| M1 | **Auth + gateway güvenliği** | register/login/refresh/logout/me, hesap kilidi, ilk ADMIN `.env`, admin ekleme, servis token ucu; oauth2-resource-server ile servis ve gateway doğrulaması; gateway rotaları, CORS, Redis rate limit (fail-open/closed); §5.8 testleri | L | M0 |
| M2 | **Catalog çekirdeği** | Şube/salon, düzen taslak+yayım (K13 kilidi), film (+gizle), seans (çakışma, fiyat, ara payı); halka açık GET'ler; TMDB client + arama; **örnek veri** (3 şube, 3/4/3 salon, 10 düzen, birkaç film/seans); internal uçlar (booking için) | L | M1 |
| M3 | **Booking çekirdeği (K9)** | SeatBooking + UNIQUE kısıtı, hold (sıralı INSERT/devralma), seat-status, hold bırakma, süre dolumu job'ı, K29, satış kapanışı, snapshot; Feign + Resilience4j (catalog); **§4.10 concurrency testleri** | L | M2 |
| M4 | **Payment + bilet** | `PaymentProvider` + mock; iyzico adapter (initialize/callback/retrieve); confirm/fail/ödeme penceresi; mutabakat job'ı; bilet doğrulama ucu (`/tickets/{kod}`); iyzico açık noktalarının (token ömrü, response signature) doğrulanması | L | M3 |
| M5 | **Frontend iskeleti + katalog** | Angular 13 projesi (`.nvmrc`, strict, Bootstrap/SCSS), core (auth, interceptor, guard, APP_INITIALIZER), layout (TMDB atfı), giriş/kayıt, film listesi/detay/seanslar | M | M1, M2 (M3-M4 ile paralel yürüyebilir) |
| M6 | **Koltuk haritası → ödeme → bilet** | Harita (polling, seçim, 409 yönetimi), hold ekranı (geri sayım, vazgeç), ödeme yönlendirmesi + sonuç sayfası, rezervasyonlarım, bilet + QR, `/bilet/:kod` | L | M3, M4, M5 |
| M7 | **Admin panel** | Film + TMDB formu, gizle/göster, şube/salon, seans, **düzen editörü**, admin ekleme | L | M2, M5 |
| M8 | **Düzen göçü (K35)** | Preview/apply/activate orkestrasyonu (§7.8), eşleştirme + yedek koltuk, bakım kilidi, denetim kaydı, uygulama içi bildirim + zil UI | L | M3, M7 |
| M9 | **Sertleştirme** | `mvn dependency-check`, `npm audit`, rate limit/Redis kesinti testleri, uçtan uca duman testi, ADR'ler (servis token istisnası vb.), README son hali, demo senaryosu | M | M8 |

**Önerilen sıra:** M0 → M1 → M2 → M3 → M4 → (M5 paralel) → M6 → M7 → M8 → M9. M8, en büyük ve en riskli özellik olduğu için son sıraya konur; çekirdek ürün (görüntüle, seç, öde, bilet) M6'da tamamlanmış olur.

### 9.3 Kodlamadan önce kapanacak doğrulamalar
1. Spring Boot 3.x ile Spring Cloud sürüm uyumluluk tablosu; sabit Docker image tag'leri (JRE 21, MySQL 8.4, Redis, nginx).
2. TMDB: `descriptors` alanının TR için dolu gelip gelmediği, `append_to_response`, atıf ve hotlink şartları.
3. iyzico: token ömrü, `retrieve` yanıt `signature` hesabı.
4. `angularx-qrcode` ve `ng-bootstrap` (12.x) Angular 13 uyumlu sürümleri ve peer bağımlılıkları; `DatePipe` `'+0300'` davranışı.
5. Redis erişilemezken Spring Cloud Gateway `RequestRateLimiter` varsayılan davranışı.

### 9.4 Cevaplar (2026-10-28) → K42
1 sıra uygun; 2 örnek veri `dev` profilinde çalışan seed olacak (migration değil); 3 her milestone kendi içinde alt görevlere bölünecek (bkz. §9.5).

### 9.5 Milestone içi alt görevler
- Her milestone başlamadan hemen önce **alt görev listesine** ayrılır (her görev tek amaçlı, tek commit/küçük PR boyutunda, kendi testiyle). Hepsini şimdiden ayrıntılandırmıyoruz: ilerledikçe öğrenilenler planı değiştireceği için erken detay çoğu zaman boşa gider (YAGNI).
- Her alt görev: amaç, dokunulan servis/modül, kabul ölçütü (test), bağımlılık.
- Örnek (M0): parent POM + `sinema-common` → servis iskeletleri (health) → MySQL init + compose → Redis + gateway'in ayağa kalkması → Flyway altyapısı → `.env.example` + anahtar betiği → README.

**M0 alt görevleri (2026-10-29):**
1. [x] Maven Wrapper + parent POM (Boot 4.1.2, Cloud 2025.1.4) + `sinema-common` (korelasyon kimliği filtresi + otomatik yapılandırma, 4 test)
2. [x] 4 servlet servis iskeleti (auth, catalog, booking, payment) + gateway iskeleti, Actuator health. Portlar: gateway 8080, auth 8081, catalog 8082, booking 8083, payment 8084 (`SERVER_PORT` ile değiştirilebilir)
3. [x] Servis Dockerfile'ları (multi-stage, non-root) + `docker-compose.yml` + MySQL init (4 şema/kullanıcı) + healthcheck'ler
4. [x] Redis + gateway'in compose'da ayağa kalkması (container sağlığı `/actuator/health/liveness`; Redis düşünce liveness UP kalır, K38 fail-open ile uyumlu)
5. [x] Flyway altyapısı (her serviste `V1__baslangic.sql`, `ddl-auto=validate`); entegrasyon testleri Testcontainers + gerçek MySQL 8.4.13, ortak kurulum `sinema-test-destegi` modülünde
6. [x] `.env.example` (M0–M4 tüm secret'lar, milestone etiketli) + `scripts/jwt-anahtar-uret.sh` (RSA 3072, PKCS#8 private + X.509 public, `keys/` gitignore'da). Container mount'ları M1'de (anahtarı kullanan kod gelince). TMDB için `TMDB_API_TOKEN` (v4 Bearer token) kullanılır
7. [x] README (hızlı başlangıç, yerel geliştirme, env tablosu, sık sorunlar, yol haritası); sıfırdan kurulum ayrı compose projesiyle ve yerel tek servis çalıştırma denendi

**M0 tamamlandı (2026-10-29).**

**§9.3-5 doğrulandı (2026-10-29, spring-cloud-gateway v5.0.4 `RedisRateLimiter` kaynağı):** Redis hatasında `onErrorResume` isteğe izin verir ve `X-RateLimit-Remaining: -1` yazar (bilinçli fail-open). Genel trafik için K38 varsayılanla sağlanır; `/auth/login` ve `/auth/register` için fail-closed, limiter'ı saran bir dekoratörle (Decorator pattern; kalan hak `-1` ise reddet) yapılır.

**M1 alt görevleri (2026-10-29):**
1. [x] `sinema-common`: ortak hata formatı (RFC 7807 `ProblemDetail` + `code`, `correlationId`, `errors[]`), özel exception hiyerarşisi, global `@RestControllerAdvice`
2. [x] auth: `users` tablosu (V2), kayıt (`POST /register`): doğrulama, e-posta normalize, BCrypt(12), rol her zaman USER
3. [x] auth: JWT imzalama (private key mount, RS256, `kid`, `sub/role/iss/iat/exp/jti`, 15 dk) + giriş (`POST /login`, genel hata mesajı) + hesap kilidi (5 hata → 5 dk)
4. [x] auth: refresh token (V3 `refresh_tokens`: opak, SHA-256 hash, aile, rotasyon, yeniden kullanım tespiti, 10 sn tolerans), HttpOnly cookie, `POST /refresh`, `POST /logout`, `GET /me`
5. [x] auth: ilk ADMIN `.env` ile (yalnızca admin yokken), `POST /admin/auth/admins`
6. [x] (401/403 problem+json yazıcıları, `GuvenlikHataYoneticisi` ve auth'un private+public key mount'u alt görev 3'te öne çekildi) `sinema-common`: servlet servislerde JWT doğrulama (oauth2-resource-server, public key, `iss`, rol → authority, STATELESS, `@PreAuthorize` açık; health herkese açık) — 4 servise uygulanır
7. [x] Servis JWT'si: auth `POST /internal/v1/auth/service-token` (client credentials, `aud`, `SERVICE` rolü, 5 dk) + `/internal/**` yalnızca `SERVICE` ve doğru `aud` (çağıran taraftaki Feign interceptor M3'te, ilk Feign çağrısıyla)
8. [ ] gateway: rotalar (§3.5, §7.7), reaktif JWT ön doğrulama + rota bazlı rol, `/internal/**` yönlendirilmez, korelasyon kimliği üretimi, CORS (izinli origin listesi, credentials) + refresh/logout için `Origin` kontrolü
9. [ ] gateway: Redis rate limit (genel IP sınırı + login/register/tickets sıkı), auth rotalarında fail-closed dekoratörü; Testcontainers Redis ile Redis açık/kapalı testleri
**Çalışma düzeni (2026-10-29):** gruplar [3], [4+5], [6+7], [8], [9], [10]; her alt görev ayrı commit. Test: her alt görevde değişen modül, grup sonunda tüm `mvnw verify`, Docker uçtan uca yalnızca Dockerfile/compose/migration değişince ve milestone sonunda.

10. [ ] compose: anahtar mount'ları (private yalnızca auth'a), admin ve client secret env'leri; gateway üzerinden uçtan uca duman testi (register → login → me → refresh → logout); README

Eski sorular (referans):
1. **Sıra** (özellikle düzen göçünün M8'de, en sonda olması) uygun mu?
2. **Örnek veri** (M2) Flyway migration'ı olarak mı (her ortamda gelir), yoksa yalnızca `dev` profilinde çalışan ayrı bir seed olarak mı? Önerim `dev` seed.
3. **M0'dan itibaren kodlamaya geçelim** onayını ne zaman vereceksin? (Planlama bu onaya kadar durur; vermeden önce planda değişmesini istediğin bir yer var mı?)
