# Auditoría básica

Fecha: 2026-10-03 (revisada el 2026-10-19 tras el rediseño)
Alcance: **GArepasAppV2** (backend Spring Boot/Thymeleaf) + **GArepasMenu** (cliente React)
Método: revisión estática de código y configuración, `./mvnw test` (83/83 ✔), inspección de endpoints y política de acceso. **No es un pentest** ni incluye análisis dinámico ni infraestructura.

> **Rediseño del 2026-10-19 (ver [`ARQUITECTURA.md`](./ARQUITECTURA.md)):** se reescribieron las capas de dominio, web e infraestructura. Estado actual de los hallazgos:
>
> | ID | Hallazgo | Estado tras el rediseño |
> |---|---|---|
> | A-01 | Credenciales por defecto | ✅ Sin defaults en la config base; dev las aporta |
> | A-02 | Siembra de datos demo | ✅ `DataSeeder` solo con perfil `dev` |
> | A-03 | Base no productiva | 🟡 Configurado (falta provisionar PostgreSQL) |
> | A-04 | Credenciales demo en logs | ✅ Resuelto |
> | A-05 | Usuario `cajero1` automático | ✅ Solo en dev |
> | A-06 | HTTPS / cookies seguras | 🟡 Pendiente de TLS en el proxy |
> | A-07 | `UsuarioSeeder` reescribe la clave del admin | ✅ **Resuelto**: el seeder solo crea el usuario si no existe; la clave cambiada desde la app se conserva |
> | A-08 | CDN sin SRI | ✅ **Resuelto de otra forma**: Bootstrap, Iconos y Chart.js se sirven desde `static/vendor` (sin CDN ni dependencia de internet) |
> | A-09 | Cobertura de pruebas de seguridad nula | ✅ 12 pruebas de seguridad + 71 de dominio |
> | A-10 | Spring Boot 3.4 fuera de soporte | ⏳ Plan pendiente (la línea 3.4 se mantiene) |
> | A-11 | Sin healthcheck | ✅ Actuator + `HEALTHCHECK` en el Dockerfile |
> | A-12 | Sin límite de tasa en la API pública | ⏳ Pendiente (opcional) |
> | A-13 | favicon 404 | ✅ Resuelto |
> | A-14 | Sin CSP | ⏳ Pendiente (ya no hay CDN: se puede fijar una CSP estricta) |
> | A-15 | `app.seed.admin-password` con default | ✅ Resuelto |
> | A-16 | `.playwright-mcp/` en el árbol | ✅ Resuelto |
>
> **Hallazgos nuevos del rediseño y cómo se trataron:**
>
> | Hallazgo | Tratamiento |
> |---|---|
> | Overselling: dos líneas del mismo producto pasaban la validación de stock por separado | **Corregido**: las líneas se consolidan antes de validar (`VentaServiceTest.rechazaVentaConElMismoProductoRepetidoQueExcedeElStock`) |
> | Unidades convertidas en varios sitios (formulario, API, servicio) | **Corregido**: una sola conversión, en la capa de servicio (`InsumoServiceTest`, `RecetaServiceTest`) |
> | `Insumo.unidadMedida` significaba dos cosas distintas | **Corregido**: `unidad_presentacion` (lo que elige el usuario) + base derivada (`UnidadMedida.base()`) |
> | Insumo repetido en una receta: suficiencia evaluada por fila | **Corregido**: los ingredientes se consolidan por insumo (`ProduccionServiceTest.consolidaLosIngredientesRepetidosAlCalcularLaSuficiencia`) |
> | Reporte con un día fantasma (BETWEEN inclusivo) | **Corregido**: rango semiabierto (`ReporteServiceTest.elRangoEsSemiabiertoYNoIncluyeLaMedianocheDelDiaSiguiente`) |
> | Costo por unidad sin considerar el rendimiento de la receta | **Corregido** (`CostoServiceTest.elCostoUnitarioDividePorElRendimientoDeLaReceta`) |
> | Margen interpretado como factor (0.30) en vez de porcentaje (30) | **Corregido** (`CostoServiceTest.elMargenDeSugerenciaSeInterpretaComoPorcentaje`) |
> | Anular una venta la borraba del histórico | **Corregido**: `EstadoVenta.ANULADA`; el registro se conserva y deja de contar |
> | Referencias "id suelto" sin clave foránea | **Corregido**: relaciones JPA + FKs en la migración V2 |
> | Ajustes de stock sin rastro ni bloqueo | **Corregido**: `InventarioService` + bitácora `ajustes_inventario` |
> | Se podía dejar el sistema sin ningún administrador | **Corregido** (`UsuarioServiceTest.noSePuedeEliminarElUltimoAdministradorActivo`) |
> | Errores de dominio terminaban en página 500 | **Corregido**: `GlobalExceptionHandler` |
> | Historial de ventas cargaba la tabla completa | **Corregido**: búsqueda en SQL + paginación |
> | SW cacheaba páginas autenticadas (fuga entre usuarios) | **Corregido**: el SW ya no cachea navegaciones |
> | Si el CDN caía, el borrado se ejecutaba sin confirmar | **Corregido**: sin CDN + fallback a `window.confirm` |
> | H2 en `runtime` (viajaba en la imagen de producción) | **Corregido**: scope parametrizado; `-Pproduccion` lo deja en `test` |
> | Docker corría como root | **Corregido**: usuario sin privilegios + `HEALTHCHECK` |

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
