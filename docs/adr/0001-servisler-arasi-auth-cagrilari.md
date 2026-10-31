# ADR 0001: Servislerin auth-service'i çağırması (iki istisna)

- **Durum:** Kabul edildi (2026-10-29: istisna 1; 2026-10-31: istisna 2)
- **İlgili kararlar:** K36 (servisler arası Bearer servis JWT), K48 (kayıtta ad/soyad/telefon), planlama §3.3, §5.5

## Bağlam

Servisler arası bağımlılık tek yönlüdür: `payment → booking → catalog`. Planlamanın ilk halinde (§3.3) hiçbir servis
auth-service'i çağırmıyordu; kullanıcıyı JWT'deki `sub` ve `role` ile tanıyorlardı. İki ihtiyaç bu kuralın dışına
çıkmayı gerektirdi.

## Karar

auth-service'e yalnızca aşağıdaki iki çağrı yapılır. Başka bir yönde servis çağrısı eklemek yeni bir ADR ister.

1. **Servis token'ı (tüm çağıran servisler):** `POST /internal/v1/auth/service-token`. Servisler arası çağrılar
   kullanıcı JWT'si değil, kısa ömürlü (5 dk) servis JWT'si taşır (K36). Token'ı auth üretir: istemci kimliği
   `spring.application.name`, sırrı `.env`'den. auth, hangi servisin hangi hedefe token alabileceğini sınırlar
   (`sinema.auth.servis-istemcileri.*.izinli-hedefler`).
2. **Alıcı bilgisi (yalnızca payment):** `GET /internal/v1/auth/users/{id}/billing-info`. iyzico ödeme başlatırken
   gerçek ad, soyad, e-posta ve telefon ister (K48). Bu bilgiler auth'ta tutulur; kopyalanmaz.

## Gerekçe

- **Döngü yok:** auth hiçbir servisi çağırmaz. Bağımlılık grafiği yönsüz döngü içermez.
- **Kişisel veri tek yerde:** ad/telefon JWT'ye konmaz (planlama §5.1: token'da kişisel veri yok) ve başka servise
  kopyalanmaz; yalnızca ödeme anında okunur. Böylece güncellendiğinde tek yerden değişir.
- **Yalnızca okuma:** istisna 2 auth'ta hiçbir şey değiştirmez.
- **Erişim dar:** `/internal/**` gateway'den hiç yönlendirilmez; auth tarafında yalnızca `SERVICE` rolü ve
  `aud=auth-service` olan token kabul edilir; bu hedefe yalnızca payment token alabilir.

## Değerlendirilen alternatifler

- **Kişi bilgisini JWT'ye koymak:** her istekle taşınır, loglara ve tarayıcıya yayılır; §5.1'e aykırı. Reddedildi.
- **Ön yüzün ödeme isteğinde göndermesi:** istemciden gelen alıcı bilgisine güvenilemez ve kayıtla tutarlılık
  garantisi yoktur. Reddedildi.
- **payment'ın kendi kopyasını tutması:** veri tekrarı ve senkronizasyon gerekir (YAGNI). Reddedildi.

## Sonuçlar

- auth çalışmıyorsa ödeme başlatılamaz (503 `AUTH_UNAVAILABLE`); devre kesici ve zaman aşımı ortak Feign
  altyapısından gelir.
- Kişi bilgisi olmayan hesaplar (admin, K48 öncesi hesaplar) için payment yapılandırmadaki yedek değerleri kullanır.
