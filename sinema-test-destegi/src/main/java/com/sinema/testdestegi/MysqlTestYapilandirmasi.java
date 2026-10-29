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

    @Bean
    @ServiceConnection
    public MySQLContainer mysqlKonteyneri() {
        return new MySQLContainer(MYSQL_IMAJI);
    }
}
