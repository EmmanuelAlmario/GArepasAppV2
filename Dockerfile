# =============================================================
# GArepasAppV2 — imagen de produccion
# Multi-stage: compila con JDK 21 y ejecuta con JRE 21.
# Railway construye este Dockerfile (ver railway.json).
# =============================================================

# ---------- Etapa de build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# El wrapper puede venir sin permiso de ejecucion al clonar.
# -Pproduccion deja el driver H2 fuera del jar (en produccion solo hay PostgreSQL).
RUN chmod +x mvnw && ./mvnw -B -DskipTests -Pproduccion package

# ---------- Etapa de runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Contenedor pequeno: heap acotado para RAM y costo predecibles
# (~450 MB RSS con la app cargada). Suficiente para pocos usuarios.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx320m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# Usuario sin privilegios: el contenedor no corre como root.
RUN useradd --system --uid 1001 --create-home --shell /usr/sbin/nologin garepas

COPY --from=build /app/target/*.jar app.jar
RUN chown -R garepas:garepas /app
USER garepas

# Railway inyecta PORT; la app lo lee con ${PORT:8080}.
EXPOSE 8080

# Healthcheck propio: Railway usa /actuator/health, pero el contenedor debe
# saber por si mismo si la app responde.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "wget -q -O- http://127.0.0.1:${PORT:-8080}/actuator/health | grep -q '\"status\":\"UP\"'"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
