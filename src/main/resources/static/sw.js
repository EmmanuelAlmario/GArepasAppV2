/*
 * Service worker de la PWA.
 *
 * Estrategia:
 *  - Navegaciones (HTML): red primero; sin red se responde desde la cache
 *    la ultima pagina vista y, en su defecto, /offline.html.
 *  - Estaticos (css/js/imagenes del dominio): cache primero, rellenando la
 *    cache en cuanto se descargan.
 *  - Recursos de terceros (CDN de Bootstrap/fuentes): cache con revalidacion
 *    en segundo plano para no bloquear la carga.
 *
 * Solo se manejan peticiones GET: los formularios POST nunca se cachean.
 * Cambiar CACHE_VERSION fuerza la limpieza de la cache vieja en el install.
 */
const CACHE_VERSION = 'v3';
const CACHE_NAME = `garepas-${CACHE_VERSION}`;

const OFFLINE_URL = '/offline.html';
const PRECACHE = [
    OFFLINE_URL,
    '/css/tokens.css',
    '/css/base.css',
    '/css/components.css',
    '/css/layout.css',
    '/js/ui-feedback.js',
    '/images/icon-192.png',
    '/images/gordo_arepas_logo.png'
];

self.addEventListener('install', (evento) => {
    evento.waitUntil(
        caches.open(CACHE_NAME)
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

    if (request.mode === 'navigate') {
        evento.respondWith(navegarConRedPrimero(request));
        return;
    }

    if (new URL(request.url).origin === self.location.origin) {
        evento.respondWith(cachePrimero(request));
        return;
    }

    evento.respondWith(cacheConRevalidacion(request));
});

/** Red primero para paginas; sin red, cache de la pagina y por ultimo offline. */
async function navegarConRedPrimero(request) {
    try {
        const respuesta = await fetch(request);
        if (respuesta && respuesta.ok) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, respuesta.clone());
        }
        return respuesta;
    } catch (error) {
        const cacheada = await caches.match(request);
        if (cacheada) {
            return cacheada;
        }
        return (await caches.match(OFFLINE_URL)) || Response.error();
    }
}

/** Cache primero para estaticos del propio dominio. */
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
