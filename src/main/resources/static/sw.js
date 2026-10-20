/*
 * Service worker de la PWA.
 *
 * Estrategia:
 *  - Navegaciones (HTML): red primero. La respuesta NO se cachea: las paginas
 *    del sistema muestran datos del negocio y de la sesion (nombre de usuario,
 *    ventas, costos). Cachearlas hacia que, en un dispositivo compartido, la
 *    persona siguiente vieran el HTML de la anterior. Sin red se responde
 *    /offline.html.
 *  - CSS y JS del propio dominio: RED PRIMERO con la cache como respaldo.
 *    Antes era "cache primero": al publicar un cambio de estilos, el
 *    navegador seguía sirviendo el CSS viejo desde la cache y la aplicacion
 *    se veia con el diseno anterior (o sin estilos). Con red primero, un
 *    despliegue nuevo se ve de inmediato y, sin conexion, se usa la cache.
 *  - Recursos inmutables (vendor, imagenes, fuentes): cache primero.
 *  - Recursos de terceros (fuentes web): cache con revalidacion en segundo
 *    plano para no bloquear la carga.
 *
 * Solo se manejan peticiones GET: los formularios POST nunca se cachean.
 * Cambiar CACHE_VERSION fuerza la limpieza de la cache vieja en el install.
 */
const CACHE_VERSION = 'v5';
const CACHE_NAME = `garepas-${CACHE_VERSION}`;

const OFFLINE_URL = '/offline.html';
const PRECACHE = [
    OFFLINE_URL,
    '/css/tokens.css',
    '/css/base.css',
    '/css/components.css',
    '/css/layout.css',
    '/js/ui-feedback.js',
    '/js/tablas-moviles.js',
    '/js/filas.js',
    '/js/pos-cart.js',
    '/vendor/css/bootstrap.min.css',
    '/vendor/css/bootstrap-icons.css',
    '/vendor/js/bootstrap.bundle.min.js',
    '/vendor/js/chart.umd.min.js',
    '/images/icon-192.png',
    '/images/gordo_arepas_logo.png'
];

/* Recursos que no cambian entre despliegues: se sirven desde la cache. */
const INMUTABLES = ['/vendor/', '/images/', '/fonts/'];

self.addEventListener('install', (evento) => {
    evento.waitUntil(
        caches.open(CACHE_NAME)
            // addAll es atomico: si un recurso falla, no se instala el worker
            // y se evita dejar la app a medias.
            .then((cache) => cache.addAll(PRECACHE))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (evento) => {
    evento.waitUntil(
        caches.keys()
            .then((nombres) => Promise.all(
                nombres
                    .filter((nombre) => nombre.startsWith('garepas-') && nombre !== CACHE_NAME)
                    .map((nombre) => caches.delete(nombre))
            ))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (evento) => {
    const { request } = evento;

    // Solo GET: los POST de los formularios van siempre a la red.
    if (request.method !== 'GET') {
        return;
    }

    const url = new URL(request.url);
    const esPropio = url.origin === self.location.origin;

    if (request.mode === 'navigate') {
        evento.respondWith(navegarConRedPrimero(request));
        return;
    }

    if (esPropio) {
        if (INMUTABLES.some((prefijo) => url.pathname.startsWith(prefijo))) {
            evento.respondWith(cachePrimero(request));
            return;
        }
        // CSS y JS: red primero para que un despliegue nuevo se vea ya.
        evento.respondWith(redPrimeroConRespaldo(request));
        return;
    }

    evento.respondWith(cacheConRevalidacion(request));
});

/**
 * Red primero para paginas. Sin red se responde /offline.html: la pagina
 * visitada NO se cachea porque contiene datos de la sesion.
 */
async function navegarConRedPrimero(request) {
    try {
        return await fetch(request);
    } catch (error) {
        return (await caches.match(OFFLINE_URL)) || Response.error();
    }
}

/**
 * Red primero con la cache como respaldo: si hay red, se usa la version
 * del servidor (y se actualiza la cache); si no, la ultima guardada.
 */
async function redPrimeroConRespaldo(request) {
    const cache = await caches.open(CACHE_NAME);
    try {
        const respuesta = await fetch(request);
        if (respuesta && respuesta.ok) {
            cache.put(request, respuesta.clone());
        }
        return respuesta;
    } catch (error) {
        const cacheada = await cache.match(request);
        if (cacheada) {
            return cacheada;
        }
        return Response.error();
    }
}

/** Cache primero para recursos inmutables del propio dominio. */
async function cachePrimero(request) {
    const cacheada = await caches.match(request);
    if (cacheada) {
        return cacheada;
    }
    try {
        const respuesta = await fetch(request);
        if (respuesta && respuesta.ok) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, respuesta.clone());
        }
        return respuesta;
    } catch (error) {
        return Response.error();
    }
}

/** Cache inmediata + revalidacion en segundo plano (recursos de terceros). */
async function cacheConRevalidacion(request) {
    const cache = await caches.open(CACHE_NAME);
    const cacheada = await cache.match(request);

    const revalidar = fetch(request)
        .then((respuesta) => {
            if (respuesta && respuesta.ok) {
                cache.put(request, respuesta.clone());
            }
            return respuesta;
        })
        .catch(() => cacheada);

    return cacheada || revalidar;
}
