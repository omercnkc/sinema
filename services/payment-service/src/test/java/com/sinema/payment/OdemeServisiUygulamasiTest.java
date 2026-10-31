package com.sinema.payment;

import com.sinema.testdestegi.ServisAltyapiTesti;
import org.springframework.test.context.TestPropertySource;

// Health, korelasyon kimliği ve Flyway testleri ServisAltyapiTesti'nden gelir (gerçek MySQL ile).
// Varsayılan sağlayıcıyla (IYZICO) açılır: adaptörün kurulduğu da böylece doğrulanır; anahtarlar sahte, çağrı yapılmaz.
@TestPropertySource(properties = {
        "sinema.payment.iyzico.api-anahtari=sandbox-test-anahtari",
        "sinema.payment.iyzico.gizli-anahtar=sandbox-test-sirri"})
class OdemeServisiUygulamasiTest extends ServisAltyapiTesti {
}
