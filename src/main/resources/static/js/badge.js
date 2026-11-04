(function () {
  function online() {
    var real = navigator.onLine;
    var simulated = (window.OfflinePayNet && typeof OfflinePayNet.isOnline === 'function')
      ? OfflinePayNet.isOnline() : true;
    return real && simulated;
  }

  function update() {
    var badge = document.getElementById('status-badge');
    var text = document.getElementById('status-text');
    if (!badge || !text) return;
    var on = online();
    text.textContent = on ? 'Online' : 'Offline';
    badge.classList.toggle('online', on);
    badge.classList.toggle('offline', !on);
  }

  window.addEventListener('online', update);
  window.addEventListener('offline', update);
  document.addEventListener('DOMContentLoaded', update);
  setInterval(update, 2000);
})();