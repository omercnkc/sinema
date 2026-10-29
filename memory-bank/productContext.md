# Product Context

Sinema salonu koltuk seçimi ve rezervasyon sistemi (full stack, SPA + microservice).
**Detaylı kararlar ve plan: [../planlama.md](../planlama.md)** (tek doğru kaynak).
## Özet kararlar (2026-10-26)
- Rezervasyon = film + saat (seans) + salon; örnek: İstanbul'da 3 salon
- Roller: GUEST (görüntüleme), USER (giriş yapınca satın alma), ADMIN (panel: salon/film/seans/fiyat/blog)
- Salon düzeni özelleştirilebilir (hücre tipli grid), sabit harita değil
- Alınan koltuk iptal edilemez; hold süresi zorunlu ve sunucu tarafında; kullanıcı başı max 5 koltuk
- Aynı koltuk aynı anda iki kişiye satılamaz (en kritik gereksinim)
- Kararlar: hold=3 dk, satış kapanışı=başlangıçtan 10 dk sonra (güncel), ödeme=iyzico Sandbox (+test mock), düzen sadece seanssız aralıkta değişir, "blog"=film içeriği
- Film alanları: başlık, açıklama, afiş, süre, tür, vizyon, yaş sınırı, akıllı işaretler, oyuncular, yönetmen, yıldız
- 3 şube (3/4/3 salon), tek fiyat/salon, koltuk tipi STANDARD+ACCESSIBLE, bilet=kod+QR, 3 terk edilmiş hold -> 10 dk cooldown
- Film verisi TMDB API'den (yıldız=TMDB puanı, yaş sınırı=release_dates TR, işaretler TMDB descriptors varsa yoksa admin)
