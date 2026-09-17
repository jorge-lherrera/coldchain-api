# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25.0.4_7-jdk-noble AS build

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon --console=plain bootJar -x test \
 && cp build/libs/*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted


FROM eclipse-temurin:25.0.4_7-jre-noble AS runtime

RUN apt-get update \
 && apt-get install --yes --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 coldchain \
 && useradd --system --uid 10001 --gid coldchain --no-create-home --shell /usr/sbin/nologin coldchain

WORKDIR /opt/coldchain

COPY --from=build --chown=coldchain:coldchain /workspace/extracted/dependencies/ ./
COPY --from=build --chown=coldchain:coldchain /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=coldchain:coldchain /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=coldchain:coldchain /workspace/extracted/application/ ./

USER coldchain

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=10 \
    CMD curl --fail --silent --show-error http://localhost:8080/api/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "application.jar"]
