# Guía de despliegue

> Estado: backend listo para desplegar con estos pasos.
> El arranque de producción **exige** las variables de entorno: si falta alguna,
> la aplicación no arranca (a propósito) y lo dice con un mensaje claro.

## 1. Requisitos

- Java 21
- PostgreSQL 14+ con una base creada (ej. `gordoarepas`)
- Dominio con HTTPS (reverse proxy: nginx, Caddy, Traefik…)

## 2. Variables de entorno del backend

| Variable | Ejemplo | Notas |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/gordoarepas` | |
| `DB_USERNAME` | `gordoarepas` | usuario dedicado (no `postgres`) |
| `DB_PASSWORD` | *(fuerte)* | |
| `ADMIN_PASSWORD` | *(fuerte)* | contraseña inicial del admin; se re-sincroniza en cada arranque |
| `PORT` | `8080` | opcional |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` | obligatorio con HTTPS |

No definir `SPRING_PROFILES_ACTIVE=dev` en producción (ese perfil usa H2 y datos demo).

## 3. Backend

```bash
./mvnw clean package            # corre 28 pruebas y genera el jar
java -jar target/garepasapp-v2-1.0.0-SNAPSHOT.jar
```

- **Primer arranque**: crea el esquema (`ddl-auto=update`) y el usuario `admin` con `ADMIN_PASSWORD`.
- **Healthcheck**: `GET /actuator/health` → `{"status":"UP"}` (público, sin detalles).

## 4. Reverse proxy (ejemplo nginx)

```nginx
server {
    listen 443 ssl;
    server_name api.tudominio.com;

    ssl_certificate     /etc/letsencrypt/live/api.tudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.tudominio.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 5. Verificación post-despliegue

- [ ] `/actuator/health` responde `UP`
- [ ] login `admin` con la contraseña del entorno
- [ ] HTTPS activo y cookie de sesión segura

## 6. Pendientes conocidos (no bloquean el primer despliegue)

- `ddl-auto=update` → migrar a `validate` + Flyway/Liquibase (B4).
- Spring Boot 3.4.x fuera de soporte OSS (sin parches nuevos): se usa **3.4.13** (último parche de la línea); planificar 4.x más adelante.
- Backups de PostgreSQL y monitoreo.
- Smoke E2E automatizado.
