# ADR 0002: Düzen göçü — booking'in catalog'a yazma çağrısı ve satılmış biletin değişmesi

- **Durum:** Kabul edildi (2026-11-01)
- **İlgili kararlar:** K35 (düzen değişikliğinde bilet göçü), K51 (göç kararları), K17 (düzen sürümleme), K13
  (seanssız aralıkta yayım), K6 (bilet iptal edilmez), planlama §3.3, §4.13, §7.8, db-planlama §6-5

## Bağlam

Salon düzeni değişince başlamamış seansların biletleri yeni düzene taşınmalı (K35). Düzen ve seanslar catalog'da,
koltuk kayıtları ve biletler booking'dedir. İki kural bu işle çatışır:

1. Servisler arası bağımlılık tek yönlüdür (`payment → booking → catalog`) ve booking catalog'u yalnızca **okur**
   (seans bilgisi, koltuk çözümleme).
2. Satılmış (SOLD) koltuk satırı değişmez (değişmez §2.4-5, K6): iptal ve iade yok.

## Karar

1. **Göçü booking yönetir, catalog yalnızca uygular.** Booking, catalog'un iç ucundan göç kaynağını okur
   (`GET /internal/v1/catalog/halls/{id}/migration-source`), planı kendisi hesaplar, biletleri taşır ve son adımda
   catalog'un **yazan** iç ucunu çağırır: `POST /internal/v1/catalog/halls/{id}/layouts/{layoutId}/activate`. Bu,
   booking'in catalog'a yaptığı **tek yazma çağrısıdır**. Yön değişmez (catalog booking'i çağırmaz; döngü yok).
   `activate` aynı düzenle tekrar çağrıldığında hata vermez (idempotent).
2. **SOLD satırın değişmezliğine tek istisna göçtür.** `seat_bookings.layout_cell_id` ile `reservation_seats`
   (hücre + etiket) yalnızca göç işleminde ve her değişiklik `seat_migration_audit`'e yazılarak değişir
   (`KoltukGocDeposu`). Satırın durumu, sahibi, fiyatı ve bilet kodu değişmez; bu bir iptal değil, aynı biletin
   yeni düzendeki karşılığıdır. Taşımadan önce biletin üzerindeki etiket denetim kaydına yazılır.

## Akış ve tutarlılık

Dağıtık transaction yok (Saga benzeri, telafili):

1. Göç kaydı + salon bakım kilidi (commit). Kilitli salonda koltuk tutulamaz (`409 HALL_UNDER_MAINTENANCE`).
2. Plan kilit altında yeniden hesaplanır; yerleştirilemeyen bilet varsa hiçbir şey yazılmadan reddedilir.
3. Biletler + denetim kaydı tek booking transaction'ında (commit).
4. catalog `activate`.
5. Başarı: bildirimler denetim kaydından yazılır, kilit kalkar.

Sıra bilinçlidir: catalog önce etkinleştirilseydi, booking adımı başarısız olduğunda biletler eski düzenin
koltuklarını gösteren yeni bir düzen kalırdı. `activate` hata verirse:

- **Net iş hatası** (409/422/404): catalog'un transaction'ı geri alınmıştır → biletler denetim kaydından eski
  hücre/etikete döner (`ROLLED_BACK`). Bildirim zaten yazılmamıştır.
- **Belirsiz hata** (zaman aşımı, 503): catalog etkinleştirmiş olabilir → körlemesine geri alınmaz. Göç `FAILED`
  (askıda) kalır, salon kilitli kalır, admin aynı göçü tekrar uygulayınca yalnızca `activate` yeniden denenir.

## Değerlendirilen alternatifler

- **catalog'un booking'i çağırması** (yayımlayınca biletleri taşıtması): bağımlılık yönünü ters çevirir ve döngü
  yaratır (§3.3). Reddedildi.
- **Olay/mesaj kuyruğu ile eventual consistency:** onaysız yeni bileşen (broker) gerekir; tek seferlik, admin
  başlatmalı bir işlem için fazla (YAGNI). Reddedildi.
- **Biletleri yeni rezervasyon olarak yeniden kesmek (eskiyi iptal):** K6 ile çelişir, bilet kodu ve QR değişir,
  ödeme kaydıyla bağ kopar. Reddedildi.
- **Göçsüz yayımı da sunmak** (gelecek seanslar eski düzende kalır): iki davranış ve daha fazla test; K51'de tek yol
  seçildi. catalog'un doğrudan yayım ucu başlamamış seansı olan salonda 409 verir.

## Sonuçlar

- Yeni bir yazma yönü (booking → catalog `activate`) vardır; başka bir yazma çağrısı eklemek yeni bir ADR ister.
- SOLD satırın hücresi değişebilir; her değişiklik denetim kaydında iz bırakır ve müşteriye (etiketi değişen ya da
  yedek atanan biletlerde) uygulama içi bildirim gider.
- Bilinen kalan risk (tek booking kopyası): kilit commit'inden hemen önce kilidi kontrol etmiş bir koltuk tutma
  işlemi, göçün son doğrulamasından sonra commit ederse yakalanmaz; pencere milisaniyelerdir. Birden fazla booking
  kopyasına geçilirse kilit denetimi satır kilidiyle yapılmalıdır.
