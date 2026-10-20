






FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .


RUN chmod +x mvnw && ./mvnw -B -DskipTests -Pproduccion package


FROM eclipse-temurin:21-jre
WORKDIR /app



ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx320m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"


RUN useradd --system --uid 1001 --create-home --shell /usr/sbin/nologin garepas

COPY --from=build /app/target/*.jar app.jar
RUN chown -R garepas:garepas /app
USER garepas


EXPOSE 8080



HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "wget -q -O- http://127.0.0.1:${PORT:-8080}/actuator/health | grep -q '\"status\":\"UP\"'"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
