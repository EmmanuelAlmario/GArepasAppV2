# Pendientes para producción

Estado revisado: 2026-10-19
Alcance: **GArepasAppV2** (backend Thymeleaf) + **GArepasMenu** (cliente React)
Documentos hermanos: [`ARQUITECTURA.md`](./ARQUITECTURA.md) (diseño del rediseño) · [`AUDITORIA-BASICA.md`](./AUDITORIA-BASICA.md) · [`DESPLIEGUE.md`](./DESPLIEGUE.md) (genérica) · [`DESPLIEGUE-RAILWAY.md`](./DESPLIEGUE-RAILWAY.md) (destino elegido)

> **Rediseño del 2026-10-19:** se reorganizó la aplicación en capas (`domain` / `web` / `api`), se corrigieron los defectos de fondo del inventario (overselling, conversión de unidades, suficiencia por fila, rango de reportes) y se añadió la migración `V2__rediseño_dominio.sql`. Ver [`ARQUITECTURA.md`](./ARQUITECTURA.md).
>
> **Antes del primer despliegue con la V2, haz copia de seguridad de la base:** la migración renombra columnas (`insumos.precio_por_gramo` → `precio_unidad_base`, `insumos.unidad_medida` → `unidad_presentacion`), elimina `detalle_recetas.unidad_medida`, añade `ventas.estado` y crea tres claves foráneas.
>
> **La V3** (clientes, crédito y configuración del negocio) crea las tablas `clientes`, `abonos` y `configuracion_negocio`, y añade a `ventas` las columnas `subtotal`, `impuestos`, `saldo`, `forma_pago`, `fecha_vencimiento` y `cliente_id`. También necesita copia de seguridad previa.
>
> **Nuevo en esta versión:** clientes con cartera y saldo, ventas a crédito con abonos, impuestos por negocio y recibo imprimible (módulo inspirado en TREINTA, ver [`ARQUITECTURA.md`](./ARQUITECTURA.md) §7).

---

## 0. Bloqueantes (no desplegar sin esto)

| # | Pendiente | Evidencia (tras la limpieza) | Acción | Estado |
|---|---|---|---|---|
| B1 | **Datos demo** | `application.properties` (base) → `app.seed.demo-data=false` y `DataSeeder` anotado `@Profile("dev")` | Nada más: en producción la clase no se registra. | ✅ Resuelto |
| B2 | **Contraseñas por defecto** | Base sin defaults (`${ADMIN_PASSWORD}` obligatoria); `admin123`/`cajero123` viven solo en `application-dev.properties` | Rotar cualquier clave usada en demos antes de desplegar. | ✅ Resuelto (rotar claves) |
| B3 | **Base de datos no productiva** | Base: PostgreSQL por `DB_*` obligatorias; H2 solo en perfil `dev` | Provisionar el servidor PostgreSQL y crear la base. | 🟡 Configurado (falta servidor) |
| B4 | **Esquema con `ddl-auto=update`** | `application-prod.properties` → `ddl-auto=validate` + Flyway con `baseline-version=1` | Aplicar las migraciones `V2` y `V3` (se ejecutan solas al arrancar). **Copia de seguridad previa.** | ✅ Resuelto (aplicar V2 y V3) |
| B5 | **Log con credenciales demo** | `DataSeeder` ahora registra "Datos de prueba de desarrollo listos (perfil dev)." | — | ✅ Resuelto |
| B6 | **URLs locales por defecto** | Base exige `CORS_ORIGINS` (sin defaults); dev aporta localhost | Definir los dominios reales en el entorno. | 🟡 Configurado (faltan valores) |

---

## 1. Datos inventados / demo a limpiar

Todo esto lo crea `config/DataSeeder.java` y **solo se ejecuta** si `app.seed.demo-data=true` y la tabla de productos está vacía:

| Entidad | Cantidad | Contenido inventado |
|---|---|---|
| Usuario | 1 | `cajero1 / cajero123` (rol VENTAS) |
| Insumos | 10 | Harina, queso, huevo, mantequilla, aceite, sal, azúcar, chocolate, bolsas de papel y cúrcuma (inactiva) |
| Recetas | 4 | Arepa de queso, arepa plana, arepa de huevo, chocolate caliente |
| Productos | 6 | 4 con receta + gaseosa sin receta + "Combo familiar" inactivo |
| Jornadas | 10 | 9 cerradas (días −9 a −1) + 1 abierta (hoy) |
| Producciones | 9 | 8 completadas retroactivas + 1 pendiente (chocolate sin stock) |
| Ventas | 24 | 21 retroactivas con clientes inventados + 3 del día |
| Gastos | 15 | 8 de materia prima (generados por producción) + 7 operativos inventados |

**Limpieza sugerida**

- **Opción A (recomendada)**: `app.seed.demo-data=false` en producción y eliminar `DataSeeder.java` + la propiedad `app.seed.demo-cajero-password`. La clase queda solo como referencia histórica.
- **Opción B**: conservar la clase detrás de perfil de desarrollo (`@Profile("dev")`) con `demo-data=false` por defecto.

En el cliente del menú (`GArepasMenu`):

| Elemento | Dónde | Acción |
|---|---|---|
| Descripciones e ingredientes inventados | `src/data/detalles.js` | Reemplazar por copy real del negocio o aprobar el actual como definitivo. |
| Ilustraciones SVG placeholder | `src/components/IlustracionProducto.jsx` | Decidir: fotos reales (campo `imagen` en `detalles.js`) o aprobar las ilustraciones. |

---

## 2. Artefactos de prueba a borrar

| Elemento | Tamaño | Acción |
|---|---|---|
| `GArepasAppV2/.playwright-mcp/` | 240 KB · 51 archivos | ✅ Borrado y agregado a `.gitignore`. |
| `GArepasAppV2/target/`, `GArepasMenu/dist/`, `GArepasMenu/node_modules/` | — | Ya cubiertos por `.gitignore`; no versionar. |

---

## 3. Configuración del entorno de producción

- Crear `.env` en el servidor (nunca en el repo) con: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_PASSWORD` (fuerte), `MENU_PUBLICO_URL`, `CORS_ORIGINS`, `PORT` si aplica.
- `ADMIN_PASSWORD`: documentar que `UsuarioSeeder` **re-sincroniza la contraseña del admin desde el entorno en cada arranque**. Si más adelante se cambia desde la app, quitar la variable o ajustar el seeder a "solo crear si no existe".
- Backend detrás de reverse proxy con HTTPS.
- Menú publicado como estático con `VITE_API_URL` apuntando al backend real (rebuild) y su dominio agregado a `CORS_ORIGINS`.

---

## 4. Calidad y operación

| Pendiente | Detalle |
|---|---|
| Pruebas | ✅ **95 pruebas en verde** (`VentaServiceTest` 15, `SeguridadWebTest` 12, `CarteraTest` 10, `CostoServiceTest` 7, `RecetaServiceTest` 7, `PublicMenuWebTest` 7, `InsumoServiceTest` 6, `UsuarioServiceTest` 6, `EsquemaFlywayTest` 6, `JornadaServiceTest` 5, `ProduccionServiceTest` 5, `ReporteServiceTest` 3, `MenuPublicoItemResponseTest` 4). Cada corrección de fondo tiene su prueba de regresión (ver `ARQUITECTURA.md` §8). Cobertura con JaCoCo: `./mvnw test` genera `target/site/jacoco/index.html`. Falta: smoke E2E. Nota: los tests con contexto Spring deben usar `@ActiveProfiles("dev")` (si no, `ValidacionProduccion` exige variables de producción). |
| Healthcheck | ✅ `/actuator/health` expuesto (solo estado, sin detalles), público para el orquestador + `HEALTHCHECK` en el Dockerfile. |
| Logs | Definir destino (archivo/centralizado) y nivel INFO en producción. |
| Backups | Plan de respaldo de PostgreSQL (diario + retención + prueba de restauración). **Obligatorio antes de aplicar la migración V2.** |
| Monitoreo | Uptime y errores (aunque sea básico). |
| favicon | ✅ `static/images/favicon.svg` enlazado en `head.html` (ya no hay 404). El menú ya tiene uno. |
| CDN | ✅ **Eliminado**: Bootstrap, Bootstrap Icons y Chart.js se sirven desde `static/vendor`. La app no depende de internet para funcionar. |
| Dependencias de terceros | ✅ El driver H2 ya no viaja en la imagen de producción (`-Pproduccion` lo deja en scope `test`). |
| Framework | Spring Boot **3.4.13** (línea 3.4 fuera de soporte OSS desde dic-2025). Decisión del proyecto: mantener 3.4 por ahora. Planificar upgrade a 4.x (4.1.1, soporte hasta jul-2027) con la suite de pruebas. Ver auditoría A-10. |

---

## 5. Checklist de salida (resumen)

- [x] B1 `demo-data=false` y `DataSeeder` solo con perfil dev
- [x] B2 sin contraseñas por defecto en la config base (dev las aporta) — falta **rotar claves de demos**
- [x] B5 logs sin credenciales
- [ ] B3 conectar PostgreSQL real (configuración lista)
- [x] B4 `ddl-auto=validate` + migraciones (aplicar V2 y V3 con copia de seguridad previa)
- [ ] B6 definir valores reales de menú y CORS (configuración lista)
- [x] `.playwright-mcp/` borrado y en `.gitignore`
- [x] CDN eliminado: librerías servidas desde `static/vendor`
- [ ] `detalles.js` e ilustraciones aprobadas o reemplazadas
- [ ] HTTPS + `.env` en servidor
- [x] Pruebas de seguridad mínimas (MockMvc: `SeguridadWebTest`, 12 pruebas)
- [x] Pruebas de servicios y de regresión (83 en total)
- [x] Healthcheck (`/actuator/health`, solo estado) + `HEALTHCHECK` del contenedor
- [x] favicon (SVG de marca; ya no hay 404)
- [ ] Smoke E2E
- [ ] Backups + monitoreo
- [ ] Plan de upgrade de Spring Boot
