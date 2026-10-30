package com.sinema.testdestegi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Testler için Docker'da gerçek bir MySQL başlatır. @ServiceConnection sayesinde Spring,
 * datasource adresini/kullanıcısını/parolasını container'dan alır; application.yml'deki değerler ezilir.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MysqlTestYapilandirmasi {

    // docker-compose.yml ile aynı sürüm: testler üretimdekiyle aynı MySQL davranışını görür
    private static final String MYSQL_IMAJI = "mysql:8.4.13";

    /**
     * Bağlantı parametreleri application.yml'deki üretim adresiyle aynı: oturum UTC'dir. Bu olmadan
     * JdbcTemplate'e verilen Timestamp, JVM'in yerel saat dilimiyle (geliştirici makinesinde İstanbul)
     * yazılır ve Hibernate'in UTC yazdığı değerlerle 3 saat kayar.
     */
    @Bean
    @ServiceConnection
    public MySQLContainer mysqlKonteyneri() {
        return new MySQLContainer(MYSQL_IMAJI)
                .withUrlParam("connectionTimeZone", "UTC")
                .withUrlParam("forceConnectionTimeZoneToSession", "true");
    }
}
