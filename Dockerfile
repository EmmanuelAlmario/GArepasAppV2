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
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# ---------- Etapa de runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Contenedor pequeno: heap acotado para RAM y costo predecibles
# (~450 MB RSS con la app cargada). Suficiente para pocos usuarios.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx320m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

COPY --from=build /app/target/*.jar app.jar

# Railway inyecta PORT; la app lo lee con ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
