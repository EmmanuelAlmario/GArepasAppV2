  




















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

  
const INMUTABLES = ['/vendor/', '/images/', '/fonts/'];

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
          
        evento.respondWith(redPrimeroConRespaldo(request));
        return;
    }

    evento.respondWith(cacheConRevalidacion(request));
});

  



async function navegarConRedPrimero(request) {
    try {
        return await fetch(request);
    } catch (error) {
        return (await caches.match(OFFLINE_URL)) || Response.error();
    }
}

  



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
