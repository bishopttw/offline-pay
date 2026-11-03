(function () {
  var SIM_KEY = 'offlinepay.simulateOffline';
  var listeners = [];

  function isSimulated() {
    try { return localStorage.getItem(SIM_KEY) === '1'; } catch (e) { return false; }
  }
  function isOnline() {
    return navigator.onLine && !isSimulated();
  }
  function notify() {
    listeners.forEach(function (fn) { fn(isOnline()); });
  }
  function setSimulated(on) {
    try { localStorage.setItem(SIM_KEY, on ? '1' : '0'); } catch (e) { }
    notify();
  }
  function onChange(fn) {
    listeners.push(fn);
  }

  window.addEventListener('online', notify);
  window.addEventListener('offline', notify);
  window.addEventListener('storage', notify);

  function errorText(data) {
    if (!data) return 'Something went wrong.';
    if (typeof data === 'string') return data;
    if (typeof data !== 'object') return '';
    if (data.error) return errorText(data.error);
    var parts = Object.values(data).map(errorText).filter(Boolean);
    return parts.length ? parts.join(' ') : 'Something went wrong.';
  }

  async function request(method, url, bodyObject) {
    var headers = { 'Accept': 'application/json' };
    if (method !== 'GET') {
      var tokenResponse = await fetch('/transactions/csrf', { credentials: 'same-origin' });
      if (tokenResponse.status === 401 || tokenResponse.redirected) {
        window.location.href = '/login';
        throw new Error('SESSION');
      }
      if (!tokenResponse.ok) throw new Error('SERVER');
      var token = await tokenResponse.json();
      headers[token.headerName] = token.token;
      headers['Content-Type'] = 'application/json';
    }
    var response = await fetch(url, {
      method: method,
      headers: headers,
      credentials: 'same-origin',
      body: bodyObject ? JSON.stringify(bodyObject) : undefined
    });
    if (response.status === 401) {
      window.location.href = '/login';
      throw new Error('SESSION');
    }
    var data = {};
    try { data = await response.json(); } catch (e) { }
    return { ok: response.ok, status: response.status, data: data };
  }

  window.OfflinePayNet = {
    isOnline: isOnline, isSimulated: isSimulated, setSimulated: setSimulated,
    onChange: onChange, request: request, errorText: errorText
  };
})();