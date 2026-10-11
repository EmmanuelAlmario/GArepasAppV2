# Auditoría básica

Fecha: 2026-10-03
Alcance: **GArepasAppV2** (backend Spring Boot/Thymeleaf) + **GArepasMenu** (cliente React)
Método: revisión estática de código y configuración, `./mvnw test` (3/3 ✔), `npm audit --omit=dev` (0 vulnerabilidades ✔), inspección de endpoints y política de acceso. **No es un pentest** ni incluye análisis dinámico ni infraestructura.

---

## Resumen

| Severidad | Cantidad |
|---|---|
| P0 (bloqueante) | 3 |
| P1 (importante) | 4 |
| P2 (recomendado) | 5 |
| P3 (menor) | 3 |

> **Actualización 2026-10-03 (limpieza ejecutada):** A-01 ✅ mitigado (sin defaults en la config base; dev los aporta) · A-02 ✅ resuelto (demo off + `@Profile("dev")`) · A-03 🟡 mitigado por configuración (falta provisionar PostgreSQL) · A-04 ✅ resuelto · A-05 🟡 mitigado (el cajero demo solo existe en perfil dev) · A-08 ✅ resuelto (SRI + versiones nuevas) · A-16 ✅ resuelto. Se agregó `ValidacionProduccion`: en perfiles distintos de `dev`, el arranque se detiene con mensaje claro si faltan `DB_*`, `ADMIN_PASSWORD`, `MENU_PUBLICO_URL` o `CORS_ORIGINS`. El resto de hallazgos sigue vigente. Las líneas citadas abajo corresponden a la revisión original, antes de la limpieza.
>
> **Actualización 2026-10-03 (segunda pasada):** A-09 ✅ resuelto — nueva suite `SeguridadWebTest` (12 pruebas MockMvc: login válido/inválido, CSRF en login y POST de negocio, matriz de roles ADMIN/VENTAS, API pública sin stock, healthcheck). A-11 ✅ resuelto — `spring-boot-starter-actuator` exponiendo solo `/actuator/health` (público, sin detalles ni componentes). A-13 ✅ resuelto — favicon SVG servido por el backend. Además: `VentaServiceTest` (6), `JornadaServiceTest` (4) y `ProduccionServiceTest` (3) cubren las reglas de stock, jornada única y efectos de producción. **Total: 28 pruebas, 28 en verde.**
>
> **Actualización 2026-10-03 (tercera pasada):** por decisión del proyecto se mantiene la línea 3.4; se actualizó el parche **3.4.5 → 3.4.13** (último de la línea, correcciones dentro de la misma API, 28/28 pruebas y jar verificado). A-10 sigue vigente como plan (upgrade a 4.x más adelante). Guía de despliegue creada: [`DESPLIEGUE.md`](./DESPLIEGUE.md).

**Lo que ya está bien**

- CSRF activo (no se deshabilitó en `SecurityConfig`).
- Contraseñas con `BCryptPasswordEncoder`.
- Autorización por rol verificada en spot-check (ver sección final).
- CORS restringido: solo `/api/public/**`, solo `GET`, orígenes configurables.
- La API pública **no expone stock exacto**, solo disponibilidad.
- Páginas de error sin trazas ni información interna.
- Sin consola H2 y sin Actuator expuesto.
- Secretos fuera del repositorio (`.env` ignorado).
- `spring.jpa.open-in-view=false`.
- Cliente React con `npm audit` limpio; `prefers-reduced-motion` respetado.

---

## P0 — Bloqueantes

| ID | Hallazgo | Evidencia | Recomendación |
|---|---|---|---|
| A-01 | **Credenciales por defecto** `admin123` / `cajero123` | `application.properties:31`, `UsuarioSeeder.java:23`, `DataSeeder.java:100` | Quitar los fallbacks; exigir variables de entorno; rotar claves usadas en demos. |
| A-02 | **Siembra de datos demo sobre base limpia** | `DataSeeder.java` + `application.properties:35` | `app.seed.demo-data=false` y retirar el seeder de producción (B1/B5 del documento de pendientes). |
| A-03 | **Base de datos no productiva**: H2 en memoria con `ddl-auto=update` | `application.properties:12-17`, `pom.xml` (H2 `runtime`) | PostgreSQL + `validate` + migraciones versionadas. |

## P1 — Importantes

| ID | Hallazgo | Evidencia | Recomendación |
|---|---|---|---|
| A-04 | **Credenciales demo impresas en logs** | `DataSeeder.java:124` | Eliminar la línea (aunque el flag quede en dev, el log no debe exponer claves). |
| A-05 | **Usuario `cajero1` creado automáticamente** con contraseña por defecto | `DataSeeder.java` | Los usuarios de negocio se crean desde el panel de usuarios; el seeder no debe gestionarlos. |
| A-06 | **HTTPS/cookies seguras no configuradas** | `application.properties` (sin `server.servlet.session.cookie.secure`) | Terminar TLS en reverse proxy y activar `cookie.secure=true`; revisar `SameSite` (Lax por defecto es aceptable para uso interno). |
| A-07 | **`UsuarioSeeder` re-escribe la contraseña del admin en cada arranque** desde `ADMIN_PASSWORD` | `UsuarioSeeder.run` (upsert) | Decidir: (a) mantener sincronización (documentarlo), o (b) cambiar a "solo crear si no existe" para que un cambio de clave en la app no se pierda. |
| A-10 | **Framework fuera de soporte**: Spring Boot 3.4 EOL 2025-12-31 (y 3.5 EOL 2026-06-30) | `pom.xml` (3.4.5) | Planificar upgrade a **4.x** (última 4.1.1, soporte hasta 2027-07) con la suite de pruebas como red de seguridad. Es un salto mayor: requiere sesión de trabajo propia. |

## P2 — Recomendados

| ID | Hallazgo | Evidencia | Recomendación |
|---|---|---|---|
| A-08 | **CDN sin SRI** en Bootstrap CSS/JS y Chart.js | `head.html:13`, `layout.html:17`, `reportes/resumen.html:183` | Agregar `integrity`+`crossorigin` o self-host. Aprovechar para actualizar: Bootstrap 5.3.8, Chart.js 4.5.1. |
| A-09 | **Cobertura de pruebas de seguridad nula** | Solo `InsumoServiceTest` (3 tests) | Agregar MockMvc: login ok/ko, 403 por rol, POST sin CSRF, `/api/public/menu` público. |
| A-11 | **Sin healthcheck** | — | Actuator solo `health` o endpoint propio para el orquestador. |
| A-12 | **Sin límite de tasa en la API pública** | `PublicMenuController` | Opcional según tráfico: caché y/o rate limit. Hoy solo expone catálogo y disponibilidad. |
| A-15 | **`app.seed.admin-password` con valor por defecto en properties** | `application.properties:31` | Igual que A-01/A-07: sin default. |

## P3 — Menores

| ID | Hallazgo | Evidencia | Recomendación |
|---|---|---|---|
| A-13 | **favicon 404** en el backend | consola del navegador | Agregar `favicon.ico` (el menú ya tiene uno). |
| A-14 | **Sin CSP/headers adicionales** | `SecurityConfig` | Evaluar `Content-Security-Policy` si se self-hostean assets; los defaults de Spring Security son razonables para el estado actual. |
| A-16 | **`.playwright-mcp/` con 51 artefactos de prueba** en el árbol del proyecto | `GArepasAppV2/.playwright-mcp/` (240 KB) | Borrar y agregar a `.gitignore`. |

---

## Autorización verificada (spot-check)

| Ruta | Regla | ✔ |
|---|---|---|
| `/usuarios/**` | ADMIN | ✔ |
| `/insumos/**`, `/productos/**`, `/recetas/**`, `/producciones/**`, `/gastos/**`, `/reportes/**` | ADMIN | ✔ |
| `/ventas/historial`, `POST /ventas/*/eliminar` | ADMIN | ✔ |
| `/ventas/**`, `/jornadas/**` | ADMIN o VENTAS | ✔ |
| `/api/public/**`, `/login`, `/error`, estáticos | Público | ✔ |
| Cualquier otra | Autenticado | ✔ |

## Método y límites

- Revisión manual de: `SecurityConfig`, `application.properties`, `UsuarioSeeder`, `DataSeeder`, `pom.xml`, plantillas (CDN/integración), cliente React (`package.json`, `detalles.js`, `App.jsx`).
- Ejecuciones: `./mvnw test` (3/3, BUILD SUCCESS), `npm audit --omit=dev` (0 vulnerabilidades), verificación de versiones contra `endoflife.date` y GitHub Releases.
- **No incluye**: pentest, escaneo de vulnerabilidades de dependencias (OWASP DC), pruebas de carga, revisión de servidores/infraestructura ni revisión de datos reales.
