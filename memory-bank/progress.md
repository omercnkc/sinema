# Progress

## What works
Hiçbir kod yok (bilinçli – planlama aşaması).

## What's left
Tüm planlama ve geliştirme.

## Status
Memory bank + kurallar hazır (2026-10-26).

## Decision log
- 2026-10-26: Cline-style Memory Bank benimsendi.
- 2026-10-26: Stack ve agent kuralları rules.md olarak kalıcılaştırıldı.
- 2026-10-26: Planlama bitene kadar kod yazılmayacak.
- 2026-10-26: Proje kapsamı = sinema salonu koltuk seçimi ve rezervasyon sistemi.
- 2026-10-26: planlama.md açıldı; Adım 1 kararları (K1-K9) kaydedildi, açık sorular bekliyor.
- 2026-10-26: K10-K15 eklendi (satış kapanışı A, hold 3 dk, mock ödeme, düzen kilidi, blog=film içeriği).
- 2026-10-26: K12 güncellendi (iyzico Sandbox), K16 film alanları eklendi.
- 2026-10-26: K10 güncellendi (başlangıçtan +10 dk satış), K17 düzen sürümleme onaylandı, K18 TMDB önerisi eklendi.
- 2026-10-26: K18 onaylandı (TMDB), K19-K22 eklendi. Adım 1 tamamlandı.
- 2026-10-26: Adım 2 domain modeli taslağı yazıldı (planlama.md §2), 5 açık soru.
- 2026-10-26: K23-K26 (tek fiyat/salon, 5 koltuk/işlem, kampanya yok, seans arası 15 dk).
- 2026-10-26: K27-K30 eklendi. Adım 2 tamamlandı.
- 2026-10-26: K30 güncellendi (bilet bilgileri + QR doğrulama adresi).
- 2026-10-26: Adım 3 servis sınırları taslağı yazıldı (planlama.md §3), 6 açık soru.
- 2026-10-26: K31 (5 servis onaylandı). Adım 3 tamam, Adım 4 concurrency taslağı yazıldı (planlama.md §4).
- 2026-10-27: Adım 4 soruları cevaplandı: K32 (P2 ödeme penceresi), K33 (W=30 dk, polling 5 sn), K34 (seans iptali kapsam dışı). K35 düzen göçü (V2, sessiz güncelleme, otomatik upgrade, bildirim) TASLAK; K17/değişmez 5 ve 7 ile çelişiyor, planlama.md §4.13'te sorular bekliyor.
- 2026-10-27: K35 kararları: yedek koltuk = boş + eski koltuğa/perdeye yakın, boş yoksa değişiklik reddedilir, bildirim uygulama içi. Kalan varsayımlar §4.13'te.
- 2026-10-27: Adım 4 tamamlandı. Adım 5 Auth/JWT taslağı yazıldı (planlama.md §5), 7 açık soru.
- 2026-10-27: K36 (RS256, 15 dk/14 gün, cookie refresh, e-posta doğrulama/şifre sıfırlama yok, servisler arası Bearer servis JWT, ilk admin .env). Adım 5 tamam; sıradaki Adım 6.
- 2026-10-27: Adım 6 taslağı yazıldı (planlama.md §6: repo yapısı, compose topolojisi, DB/Flyway, secret, gateway rate limit), 8 açık soru.
- 2026-10-27: K37 (repo yapısı onaylı, ayrı origin + CORS, Flyway, gateway rate limit Redis ile, sinema-common servlet, iyzico önce doküman). `git init` + .gitignore yapıldı (commit yok). Bekleyen: bağımlılık onayları (§6.11-1), Redis kesinti davranışı.
- 2026-10-27: K38 (oauth2-resource-server onaylı, Redis fail-open/fail-closed). Adım 6 tamam. Adım 7 başladı: iyzico CF doğrulama notları §7.1 (callback metodu/token ömrü/imza formülü hâlâ doğrulanacak).
- 2026-10-27: K39 (iyzico sadece yerel sandbox; callback = tarayıcı yönlendirmesiyle token POST, sonuç retrieve ile; tünel/webhook yok). Token ömrü ve response signature hesabı kodlamadan önce doğrulanacak.
- 2026-10-27: Adım 7 API sözleşme taslağı yazıldı (planlama.md §7.2–7.9): ortak kurallar, auth/catalog/booking/payment uçları, K35 göç orkestrasyonu (booking yönetir, catalog activate), 6 açık soru.
- 2026-10-27: K40 (hold bırakma, seat-status misafire açık, film gizleme, ad/soyad yok). Adım 7 tamam. Adım 8 Angular 13 taslağı yazıldı (planlama.md §8), 6 açık soru.
- 2026-10-28: K41 (Bootstrap CSS+SCSS, angularx-qrcode onaylı, ödeme tam yönlendirme, undo yok, yalnızca Türkçe, TMDB hotlink). Adım 8 tamam. Adım 9 milestone taslağı yazıldı (planlama.md §9: M0–M9), 3 açık soru.
- 2026-10-28: K41 güncellendi: ng-bootstrap eklenecek (12.x, uyumluluk kodlamadan önce doğrulanacak).
- 2026-10-28: K42 (milestone sırası onaylı, dev seed, milestone başlarken alt görevlere bölünür). Adım 9 tamam: 9 planlama adımının hepsi bitti; kodlama "kodlamaya geçelim" onayını bekliyor.
- 2026-10-28: db-planlama.md açıldı (tablo/kolon/indeks, servisler arası yumuşak referanslar, §2.2'ye farklar) ve DB şeması canvas olarak çizildi (Design artifact, 6 artboard). Onay bekleyen 4 açık soru db-planlama.md §9'da.
- 2026-10-29: K43 (DB planı onaylandı: film listeleri ayrı tablo, active_pending_key üretilmiş kolon, HELD silinir/SOLD kalır, callback logu 90 gün). Planlama tamamen kapandı; M0 "kodlamaya geçelim" onayını bekliyor.
- 2026-10-29: DB şeması tasarımı K43'e göre güncellendi. Kullanıcı kodlamayı başlattı. K44: Spring Boot 4.1.2 + Spring Cloud 2025.1.4 (3.x EOL). M0 alt görev 1 bitti: mvnw, parent POM, sinema-common (KorelasyonKimligiFiltresi), `mvnw verify` yeşil (4 test).
- 2026-10-29: M0 tamamlandı: 5 servis iskeleti, tek multi-stage Dockerfile, compose (MySQL 8.4.13, Redis 8.8.4, 4 servis + gateway, healthcheck'ler), MySQL init (4 şema/kullanıcı, izolasyon doğrulandı), JPA + Flyway V1, Testcontainers (sinema-test-destegi), JWT anahtar betiği, .env.example, README. 15 test yeşil.
- 2026-10-29: M1 tamamlandı: ortak hata biçimi (RFC 7807), kayıt/giriş (RS256, hesap kilidi), rotasyonlu refresh token + cookie, logout, /me, ilk admin + admin ekleme, sinema-common ortak JWT güvenlik zinciri (4 servis), servis token'ı (/internal), gateway (rotalar, JWT ön doğrulama, CORS, Origin denetimi, korelasyon), Redis rate limit (giriş/kayıt fail-closed). 103 test + 15 adımlı duman testi yeşil.
- 2026-10-30: M2 tamamlandı: K45 (TMDB verisi 3 ayda bir yenilenir), K46 (akıllı işaret enum'u, TMDB descriptors eşlemesi gerçek API ile doğrulandı). Catalog: şube/salon, sürümlü düzen + K13, film + TMDB, seans, iç uçlar, dev örnek veri. Duman testi 29 adım.
- 2026-10-30: M3 tamamlandı: Feign altyapısı (servis token'ı, GET'e özel retry, devre kesici), booking şeması (K9 UNIQUE, active_pending_key), koltuk tutma (READ COMMITTED + JDBC koşullu yazma, eşzamanlılık testleri), seat-status + K47 önbellek, rezervasyonlarım/bırakma, K29 (kullanıcı bazlı), süre dolumu job'ı, payment iç uçları (uzatma, onay + bilet kodu, ret). Booking 71 test, duman testi rezervasyon adımlarıyla.
