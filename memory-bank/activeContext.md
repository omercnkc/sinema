# Active Context

## Current focus
Planlama aşaması, adım adım ilerliyoruz. Kod YOK. Konu: sinema koltuk seçimi & rezervasyon sistemi.

## Recent changes
- memory-bank/ çekirdek dosyaları + rules.md oluşturuldu
- CLAUDE.md eklendi (memory-bank dosyalarını her oturumda yükler)
- Stack ve proje tipi projectbrief/techContext'e işlendi

## Next steps (planlama – bkz. planlama.md)
1. Sitenin amacı/özellikleri ve kullanıcı rolleri (productContext)
2. Servis sınırları (bounded context) ve her servisin DB'si
3. Monorepo klasör yapısı, Maven multi-module, docker-compose topolojisi
4. Auth/JWT akışı (gateway ↔ auth-service ↔ diğerleri)
5. Angular 13 modül/route planı
6. Açık sorular (techContext) için kararlar → ADR

## Active decisions
- Adım 4: K32–K34 kesin (P2 ödeme penceresi, W=30 dk, polling 5 sn, seans iptali kapsam dışı).
- Adım 4 tamam (K35 dahil). Adım 5 tamam (K36). Adım 6 tamam (K37, K38). Adım 7 tamam (K40). Adım 8 tamam (K41). Adım 9 tamam (K42): planlama bitti. Kodlama yalnızca kullanıcı "kodlamaya geçelim" deyince, M0'dan (alt görevlere bölünerek) başlar. iyzico yerel/sandbox, webhook yok (K39).
- 2026-10-29: DB planı onaylandı (K43, db-planlama.md).
- 2026-10-29: **KODLAMA BAŞLADI.** M0 alt görevleri planlama.md §9.5'te. **M0 TAMAMLANDI** (7 alt görev). GitHub: https://github.com/omercnkc/sinema (main; push yalnızca kullanıcı isteyince). M1 başladı: 10 alt görev planlama.md §9.5'te. §9.3-5 doğrulandı (RedisRateLimiter fail-open; auth rotaları için dekoratörle fail-closed). M1 alt görev 1 bitti (GlobalHataYoneticisi + SinemaIstisnasi hiyerarşisi, sinema-common otomatik yapılandırmasında). Not: Spring Security'nin 401/403'ü filtrede oluşur, advice'a gelmez → alt görev 6'da entry point/access denied handler aynı ProblemDetail biçimini yazmalı; @PreAuthorize'ın AccessDeniedException'ı advice'taki catch-all'a düşüp 500 olmamalı. Sıradaki: alt görev 2 (users + register). Image tag'leri: eclipse-temurin:21.0.12.1_1-jdk/jre-resolute, mysql:8.4.13, (Redis için) redis:8.8.4-alpine. Kullanıcıyla "beraber" ilerleniyor: her sınıf ne/nasıl anlatılır.
