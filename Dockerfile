FROM eclipse-temurin:17.0.13_11-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17.0.13_11-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 payment && mkdir /data && chown payment:payment /data
COPY --from=builder --chown=payment:payment /workspace/build/libs/payment-system-0.0.1-SNAPSHOT.jar app.jar

USER payment
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
