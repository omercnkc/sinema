package com.sinema.testdestegi;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Her @SpringBootTest context'i açılırken anahtar konumlarını test anahtarlarına yönlendirir.
 * META-INF/spring.factories ile kendiliğinden devreye girer; testlerin tek tek ayar yapması gerekmez.
 * addFirst: application.yml'deki gerçek konumu (./keys/...) ezmesi gerekir.
 */
public class TestJwtAnahtarBaslaticisi implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testJwtAnahtarlari", Map.of(
                "sinema.auth.jwt.ozel-anahtar", TestJwtAnahtarlari.ozelAnahtarKonumu(),
                "sinema.auth.jwt.acik-anahtar", TestJwtAnahtarlari.acikAnahtarKonumu())));
    }
}
