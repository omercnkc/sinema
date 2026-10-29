# Tech Context

Sabit stack ve tüm kurallar: [rules.md](rules.md). Özet:

- Backend: Java 21, **Spring Boot 4.1.2 + Spring Cloud 2025.1.4** (K44), MySQL, JPA/Hibernate, Spring Security + JWT (oauth2-resource-server), OpenFeign (+Resilience4j), Maven 3.9.16 (Maven Wrapper `mvnw`, multi-module, parent POM), Docker/Compose, Flyway, MapStruct, Lombok, springdoc
- Frontend: Angular 13 (NgModule, class-based), Node 16 via nvm (.nvmrc), RxJS, SCSS, Bootstrap + ng-bootstrap
- Repo: monorepo, API Gateway önünde, her servis kendi DB/şeması

## Development setup
- OS: Windows 11, PowerShell (Git Bash da var)
- Git repo var (henüz commit yok)
- Java: Temurin 21.0.12. **Maven kurulu değil** → `.\mvnw.cmd` kullan (wrapper Maven'ı kendisi indirir)
- Docker 29.9 / Compose v5.6 kurulu; Docker Desktop'ın açık olması gerekir
- Node: sistemde v20, nvm 2.0 var; Angular 13 için `.nvmrc` ile Node 16'ya geçilecek (M5)

## Boot 4 notları (3.x'ten farklar, karşılaştıkça eklenir)
- `spring-boot-autoconfigure` SLF4J'yi geçişli getirmez; doğrudan kullanılan `slf4j-api` açıkça eklenir.
- Web starter'ı `spring-boot-starter-webmvc`, Flyway için `spring-boot-starter-flyway` (4.1.2'de var, doğrulandı).
- Boot 4.1.2 BOM: Flyway 12.4.0 (+`flyway-mysql`), Connector/J 9.7.0 (`com.mysql:mysql-connector-j`), Testcontainers 2.0.5.
- Testcontainers 2: artifact `testcontainers-mysql`, sınıf `org.testcontainers.mysql.MySQLContainer` (generic değil); `@ServiceConnection` ile datasource otomatik bağlanır.
- Yeni Maven modülü eklenince Dockerfile'daki `COPY` listesine de eklenmeli (parent POM modülü okumak ister).
- MockMvc: `spring-boot-starter-webmvc-test`, `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- Lombok annotation processor parent POM'daki maven-compiler-plugin `annotationProcessorPaths` ile tanımlı (MapStruct eklenince buraya).
- Spring 7'de 422 = `HttpStatus.UNPROCESSABLE_CONTENT`.
- Resource server starter'ı Boot 4'te `spring-boot-starter-security-oauth2-resource-server` (eski `spring-boot-starter-oauth2-resource-server` deprecated).
- Spring 7 `ProblemDetail.getType()` varsayılan `null` (Spring 6'da about:blank).
- `Jwt.getIssuer()` "iss"i URL'e çevirmeye çalışır; düz ad için `getClaimAsString("iss")`.
- Testlerde JWT anahtarları: `sinema-test-destegi` → `TestJwtAnahtarlari` + `TestJwtAnahtarBaslaticisi` (META-INF/spring.factories ile her @SpringBootTest'e otomatik).
