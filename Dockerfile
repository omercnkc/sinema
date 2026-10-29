# syntax=docker/dockerfile:1
# Tüm Java servisleri için tek Dockerfile; hangi servisin derleneceği SERVIS argümanıyla seçilir.
# Kullanım: docker build --build-arg SERVIS=booking-service .
# Build context repo köküdür: servisler parent POM'a ve sinema-common'a ihtiyaç duyar.

ARG JDK_IMAJI=eclipse-temurin:21.0.12.1_1-jdk-resolute
ARG JRE_IMAJI=eclipse-temurin:21.0.12.1_1-jre-resolute

# ---- 1. aşama: derleme (JDK + Maven Wrapper) ----
FROM ${JDK_IMAJI} AS derleme
ARG SERVIS
WORKDIR /kaynak
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY sinema-common sinema-common
COPY services services
# Maven deposu BuildKit önbelleğinde tutulur; bağımlılıklar her build'de yeniden inmez
RUN --mount=type=cache,target=/root/.m2 \
    test -n "$SERVIS" \
    && chmod +x mvnw \
    && ./mvnw -B -q -pl "services/$SERVIS" -am package -DskipTests \
    && cp "services/$SERVIS/target/$SERVIS-"*.jar /kaynak/uygulama.jar

# ---- 2. aşama: çalışma (yalnızca JRE, root olmayan kullanıcı) ----
FROM ${JRE_IMAJI}
RUN groupadd --system sinema && useradd --system --gid sinema --no-create-home sinema
WORKDIR /uygulama
COPY --from=derleme --chown=sinema:sinema /kaynak/uygulama.jar uygulama.jar
USER sinema
# Zamanlar UTC (planlama §2.4-9); bellek sınırı container limitine göre ayarlanır
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=UTC", "-jar", "uygulama.jar"]
