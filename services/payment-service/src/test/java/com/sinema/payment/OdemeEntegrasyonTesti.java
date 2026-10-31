package com.sinema.payment;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.sinema.payment.kullanici.istemci.AliciBilgisi;
import com.sinema.payment.kullanici.istemci.KullaniciIstemcisi;
import com.sinema.payment.rezervasyon.istemci.RezervasyonIstemcisi;
import com.sinema.payment.rezervasyon.istemci.RezervasyonOdemeBilgisi;
import com.sinema.payment.saglayici.mock.MockOdemeSaglayicisi;
import com.sinema.testdestegi.MysqlTestYapilandirmasi;
import com.sinema.testdestegi.TestTokenUretici;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Ödeme entegrasyon testlerinin ortak tabanı: gerçek MySQL ve gerçek (mock) sağlayıcı; booking ve auth sahte.
 * Alt sınıflar aynı Spring context'ini paylaşır; her test kendi rezervasyon id'sini alır.
 *
 * Varsayılan sahte booking: rezervasyon kullanicinin, ödeme bekliyor, 300.00 TL, 3 dk süresi var.
 * Varsayılan sahte auth: kişi bilgileri dolu.
 */
@SpringBootTest(properties = "sinema.payment.saglayici=MOCK")
@AutoConfigureMockMvc
@Import(MysqlTestYapilandirmasi.class)
public abstract class OdemeEntegrasyonTesti {

    protected static final long KULLANICI = 42L;
    protected static final BigDecimal TUTAR = new BigDecimal("300.00");
    private static final AtomicLong REZERVASYON_SAYACI = new AtomicLong(System.currentTimeMillis() % 1_000_000 * 100);

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected RezervasyonIstemcisi rezervasyonIstemcisi;

    @MockitoBean
    protected KullaniciIstemcisi kullaniciIstemcisi;

    @MockitoSpyBean
    protected MockOdemeSaglayicisi saglayici;

    @BeforeEach
    void sahteServisleriKur() {
        when(rezervasyonIstemcisi.odemeBilgisi(anyLong()))
                .thenAnswer(cagri -> rezervasyon(cagri.getArgument(0), KULLANICI, "PENDING_PAYMENT"));
        when(rezervasyonIstemcisi.odemePenceresiniUzat(anyLong()))
                .thenAnswer(cagri -> rezervasyon(cagri.getArgument(0), KULLANICI, "PENDING_PAYMENT"));
        when(kullaniciIstemcisi.aliciBilgisi(anyLong())).thenAnswer(cagri -> new AliciBilgisi(cagri.getArgument(0),
                "ayse@ornek.com", "Ayşe", "Demir", "+905551112233"));
    }

    protected static long yeniRezervasyonId() {
        return REZERVASYON_SAYACI.incrementAndGet();
    }

    protected static RezervasyonOdemeBilgisi rezervasyon(long rezervasyonId, long sahip, String durum) {
        Instant simdi = Instant.now();
        return new RezervasyonOdemeBilgisi(rezervasyonId, sahip, durum, TUTAR, simdi.plus(Duration.ofMinutes(3)),
                false, simdi);
    }

    protected static String yetki(long kullaniciId) {
        return "Bearer " + TestTokenUretici.kullaniciTokeni(kullaniciId, "USER");
    }
}
