// 1. TURN ON THE SERVICE WORKER (SW)
if ('serviceWorker' in navigator) {
   window.addEventListener('load', function () {
      navigator.serviceWorker.register('/sw.js');
   });
}

//2. SAVED PAGES CAN CONTAIN AN OLD "PAYMENT SUCCESSFUL" MESSAGE. HIDE IT WHEN OFFLINE.
if (!navigator.online) {
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.alert').forEach(function (box) { box.remove(); });
  });
}

//3. ON THE LOGIN PAGE (AFTER LOGOUT OR TIMEOUT), FORGET THE SAVED PRIVATE PAGES
if (location.pathname === '/login' && 'caches' in window) {
   caches.open('offlinepay-v1').then(function (cache) {
     ['/dashboard', '/send', '/history'].forEach(function (page) { cache.delete(page); });
   });
}