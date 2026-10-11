# Despliegue en Railway

Preparado el 2026-10-03 para ejecutarse cuando el plan esté activo.
Complementa a [`DESPLIEGUE.md`](./DESPLIEGUE.md) (guía genérica del jar).

## Estado de partida

| Elemento | Estado |
|---|---|
| Cuenta | Trial **expirado** → activar **Hobby ($5/mes)** antes de desplegar |
| Proyecto | `alert-possibility` (id `c71dc7d9-f30c-441f-8648-4c19e5cd0528`) |
| Postgres | `Postgres-hcnd` **offline**, con volumen `postgres-volume-u4-l` (5 GB) **que contiene los datos del sistema anterior** |
| Volumen huérfano | `postgres-volume` (5 GB, sin montar) → borrar tras verificar los datos |
| Servicio viejo | `GArepasApp` (repo `EmmanuelAlmario/GArepasApp`, otra implementación) → se reemplaza |
| Backend nuevo | `GArepasAppV2` con `Dockerfile` + `railway.json` (healthcheck `/actuator/health`) |

## Arquitectura objetivo

```
Proyecto Railway: alert-possibility (production)
├── Postgres-hcnd     (existente, se reutiliza)          europe-west4
└── garepas-backend   (repo GArepasAppV2, Dockerfile)    europe-west4
```

> **Región**: el Postgres vive en `europe-west4`. Los servicios nuevos deben crearse en la **misma región**; si no, cada consulta cruza regiones (más latencia y egress facturable).

## Paso 0 — Activar el plan

Railway → Account → Plans → **Hobby**. Verificar que los servicios puedan arrancar.

## Paso 1 — Publicar el código en GitHub

Railway construye desde GitHub (ninguno de los dos proyectos es repo git todavía; los `.gitignore`/`.dockerignore` ya están listos):

```bash
# Backend
cd GArepasAppV2
git init -b main && git add -A
git commit -m "feat: version lista para produccion (perfiles, tests, Docker)"
git remote add origin git@github.com:EmmanuelAlmario/GArepasAppV2.git
git push -u origin main

```

(Los repos se pueden crear desde GitHub o vía el MCP de GitHub.)

## Paso 2 — Variables del backend

En el servicio `garepas-backend`, entorno **production**:

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://${{Postgres-hcnd.PGHOST}}:${{Postgres-hcnd.PGPORT}}/${{Postgres-hcnd.PGDATABASE}}?sslmode=require` |
| `DB_USERNAME` | `${{Postgres-hcnd.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres-hcnd.PGPASSWORD}}` |
| `ADMIN_PASSWORD` | *(secreto fuerte)* |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` |

Notas:
- **No definir `PORT`**: lo inyecta Railway y la app lo lee (`${PORT:8080}`).
- Si la conexión falla por SSL, quitar `?sslmode=require` del `DB_URL` (la red privada de Railway no lo exige; la plantilla `postgres-ssl` sí lo soporta).
- Las referencias `${{Postgres-hcnd.X}}` las resuelve Railway en el servidor; nunca viajan valores al cliente.

## Paso 3 — Rescate de datos (sistema anterior)

Se ejecuta cuando el plan esté activo:

1. **Redeployar el Postgres.** Si el redeploy falla porque el deployment fue removido sin build, reconectar la imagen `ghcr.io/railwayapp-templates/postgres-ssl:18` como fuente del servicio (o re-montar el volumen existente en un servicio nuevo). **El dato está en el volumen.**
2. Crear **túnel TCP temporal** → `pg_dump` local (`psql`/`pg_dump` 18.6 ya están instalados) → borrar el túnel.
3. Analizar el esquema viejo y mapear al nuevo: **insumos, recetas, productos** (precios y stock). Ventas y gastos: **descartados**.
4. Renombrar las tablas viejas a `*_legacy`, dejar que el backend nuevo cree su esquema y copiar con `INSERT ... SELECT`.
5. Verificar el catálogo en la app y, recién entonces, borrar el volumen huérfano.

## Paso 4 — Servicio nuevo

| Servicio | Repo | Builder | Healthcheck | Región | Variables |
|---|---|---|---|---|---|
| `garepas-backend` | `GArepasAppV2` (main) | Dockerfile | `/actuator/health` | europe-west4 | las del Paso 2 |

Luego:
1. Generar dominio a cada servicio.
2. (Opcional) Renombrar el proyecto en el dashboard (`alert-possibility` → `gordo-arepas`).

## Paso 5 — Verificación

- [ ] `/actuator/health` → `{"status":"UP"}`
- [ ] Login `admin` con la contraseña del entorno
- [ ] HTTPS activo y cookie de sesión segura

## Costos

- **Hobby**: $5/mes, con **$5 de uso incluido** (RAM $10/GB/mes, CPU $20/vCPU/mes).
- Estimación con backend + Postgres encendidos 24/7: **~$6-8/mes**.
- Con **App Sleeping** (servicio → Settings) baja a **~$3/mes** → entra en el incluido. Suficiente para 3 usuarios.
- Borrar el volumen huérfano ahorra ~$0.75/mes.
