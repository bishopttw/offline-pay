const CACHE = 'offlinepay-v2';
const SHELL = ['/offline.html', '/css/style.css', '/manifest.json',
               '/icons/icon-192.png', '/icons/icon-512.png', '/js/pwa.js'];
const PAGES = ['/dashboard', '/send', '/history'];

function shouldSave(path) {
  return PAGES.includes(path) || SHELL.includes(path)
      || path.startsWith('/css/') || path.startsWith('/js/') || path.startsWith('/icons/');
}

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  const save = shouldSave(url.pathname);
  const isPage = request.mode === 'navigate';
  if (!save && !isPage) return;

  event.respondWith(
    fetch(request)
      .then((response) => {
        if (save && response.ok && !response.redirected) {
          const copy = response.clone();
          caches.open(CACHE).then((cache) => cache.put(url.pathname, copy));
        }
        return response;
      })
      .catch(() =>
        (save ? caches.match(url.pathname) : Promise.resolve(undefined)).then((saved) => {
          if (saved) return saved;
          if (isPage) return caches.match('/offline.html');
          return Response.error();
        })
      )
  );
});