(function () {
  function $(id) { return document.getElementById(id); }

  // ---- self-check: tells you exactly what is missing instead of failing silently ----
  var needed = ['sendForm', 'receiverAccount', 'amount', 'pin', 'pinRow', 'nameLine', 'feeLine',
    'modeNote', 'status', 'netBadge', 'simulate', 'queueCard', 'queueList', 'flushBox',
    'flushPin', 'flushBtn', 'flushStatus', 'bankModal', 'tabBank', 'bankClose'];
  var missing = needed.filter(function (id) { return !$(id); });
  var net = window.OfflinePayNet;
  if (missing.length || !net) {
    var problem = missing.length ? 'send.html is missing ids: ' + missing.join(', ') : 'net.js did not load';
    console.error(problem);
    alert('Send page problem: ' + problem);
    return;
  }

  var body = document.body;
  var QUEUE_KEY = 'offlinepay.queue.' + body.dataset.accountId;
  var ONLINE_FEE = Number(body.dataset.onlineFee);
  var OFFLINE_FEE = Number(body.dataset.offlineFee);

  var form = $('sendForm');
  var accountInput = $('receiverAccount');
  var amountInput = $('amount');
  var pinInput = $('pin');
  var pinRow = $('pinRow');
  var nameLine = $('nameLine');
  var feeLine = $('feeLine');
  var modeNote = $('modeNote');
  var statusLine = $('status');
  var netBadge = $('netBadge');
  var simulate = $('simulate');
  var queueCard = $('queueCard');
  var queueList = $('queueList');
  var flushBox = $('flushBox');
  var flushPin = $('flushPin');
  var flushBtn = $('flushBtn');
  var flushStatus = $('flushStatus');
  var bankModal = $('bankModal');

  var COLORS = {
    success: ['#DCFCE7', '#166534'],
    error: ['#FEE2E2', '#991B1B'],
    info: ['#E0E7FF', '#3730A3']
  };

  function naira(value) {
    return '₦' + Number(value).toLocaleString('en-NG',
      { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function makeId() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return Date.now() + '-' + Math.random().toString(16).slice(2);
  }
  function show(element, visible) {
    element.style.display = visible ? '' : 'none';
  }
  function say(element, text, kind) {
    if (!text) {
      element.textContent = '';
      element.style.display = 'none';
      return;
    }
    var c = COLORS[kind] || COLORS.info;
    element.textContent = text;
    element.style.cssText = 'display:block;margin:12px 0;padding:12px 14px;border-radius:10px;'
      + 'background:' + c[0] + ';color:' + c[1] + ';';
    element.scrollIntoView({ block: 'nearest' });
  }

  // ---- the waiting queue lives in this browser, one per account ----
  function loadQueue() {
    try {
      var list = JSON.parse(localStorage.getItem(QUEUE_KEY)) || [];
      return list.filter(function (item) { return item && item.account; });
    } catch (e) { return []; }
  }
  function saveQueue(queue) {
    try { localStorage.setItem(QUEUE_KEY, JSON.stringify(queue)); } catch (e) { }
  }
  function removeFromQueue(clientRef) {
    saveQueue(loadQueue().filter(function (item) { return item.clientRef !== clientRef; }));
  }

  function renderQueue() {
    var queue = loadQueue();
    var online = net.isOnline();
    show(queueCard, queue.length > 0);
    show(flushBox, online && queue.length > 0);
    queueList.textContent = '';
        queue.forEach(function (item) {
          var li = document.createElement('li');
          var text = document.createElement('span');
          text.textContent = naira(item.amount) + ' to ' + item.account + ' (waiting)';
          li.appendChild(text);
          queueList.appendChild(li);
        });
  }

  function updateFee() {
    var amount = Number(amountInput.value) || 0;
    var fee = net.isOnline() ? ONLINE_FEE : OFFLINE_FEE;
    feeLine.textContent = 'Fee: ' + naira(fee) + '  |  Total: ' + naira(amount + fee);
  }

    function refreshMode() {
      var online = net.isOnline();
      var connected = navigator.onLine;
      netBadge.textContent = online ? 'Online' : (connected ? 'Offline (SMS mode)' : 'Offline');
      show(pinRow, connected);
      modeNote.textContent = online
        ? 'Online: sent right away.'
        : (connected
            ? 'Offline mode: sent through the SMS channel with a higher fee. The receiver gets it right away.'
            : 'No connection at all: saved on this phone and sent when you are back online.');
      updateFee();
      renderQueue();
    }

  // ---- only digits in the account number and the PIN boxes ----
  [accountInput, pinInput, flushPin].forEach(function (box) {
    box.addEventListener('input', function () { box.value = box.value.replace(/\D/g, ''); });
  });
  amountInput.addEventListener('input', updateFee);

  // ---- name preview under the account number box ----
  accountInput.addEventListener('blur', async function () {
    nameLine.textContent = '';
    var account = accountInput.value.trim();
    if (!account) return;
    if (!/^\d{10}$/.test(account)) {
      nameLine.textContent = 'Account number must be 10 digits.';
      return;
    }
    if (!net.isOnline()) {
      nameLine.textContent = 'Offline: cannot check the name. Double-check the account number.';
      return;
    }
    try {
      var r = await net.request('GET', '/transactions/lookup?account=' + encodeURIComponent(account));
      nameLine.textContent = r.ok
        ? 'Account name: ' + r.data.name + '  |  Bank: ' + r.data.bank
        : net.errorText(r.data);
    } catch (e) { }
  });

  // ---- pressing Send ----
   form.addEventListener('submit', async function (event) {
     event.preventDefault();
     say(statusLine, '');

     var account = accountInput.value.trim();
     var amount = amountInput.value.trim();
     var pin = pinInput.value.trim();

     if (!/^\d{10}$/.test(account)) {
       say(statusLine, 'Enter the 10-digit account number.', 'error'); return;
     }
     if (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
       say(statusLine, 'Enter a valid amount (up to 2 decimal places).', 'error'); return;
     }
     var clientRef = makeId();

     if (navigator.onLine) {
       if (!/^\d{4}$/.test(pin)) {
         say(statusLine, 'Enter your 4-digit PIN.', 'error'); return;
       }
       var viaSms = !net.isOnline();
       var r = null;
       try {
         r = await net.request('POST', viaSms ? '/transactions/sms-send' : '/transactions/send',
           { receiverAccount: account, amount: amount, pin: pin, clientRef: clientRef });
       } catch (err) {
         if (err.message === 'SESSION') return;
         r = null; // no connection: it will be saved for later below
       }
       if (r) {
         pinInput.value = '';
         if (r.ok) {
           form.reset();
           nameLine.textContent = '';
           updateFee();
           say(statusLine, 'Sent ' + naira(amount) + (viaSms ? ' through the SMS channel' : '')
             + '. Fee ' + naira(r.data.fee) + '.', 'success');
         } else {
           say(statusLine, net.errorText(r.data), 'error');
         }
         return;
       }
     }

     var queue = loadQueue();
     queue.push({ clientRef: clientRef, account: account, amount: amount, createdAt: Date.now() });
     saveQueue(queue);
     form.reset();
     nameLine.textContent = '';
     updateFee();
     renderQueue();
     say(statusLine, 'No connection. Saved on this phone, it will be sent when you are back online. Offline fee: '
       + naira(OFFLINE_FEE) + '.', 'info');
   });

  // ---- back online: send the waiting transfers ----
  flushBtn.addEventListener('click', async function () {
    say(flushStatus, '');
    var pin = flushPin.value.trim();
    if (!/^\d{4}$/.test(pin)) {
      say(flushStatus, 'Enter your 4-digit PIN.', 'error'); return;
    }

    flushBtn.disabled = true;
    var sent = 0;
    var problems = [];
    var stopped = false;
    var queue = loadQueue();

    for (var i = 0; i < queue.length; i++) {
      var item = queue[i];
      var r;
      try {
        r = await net.request('POST', '/transactions/queue',
          { receiverAccount: item.account, amount: item.amount, pin: pin, clientRef: item.clientRef });
      } catch (err) {
        if (err.message === 'SESSION') return;
        say(flushStatus, 'Connection lost. Your waiting transfers are safe and will be sent later.', 'error');
        stopped = true;
        break;
      }
      if (r.ok) {
        removeFromQueue(item.clientRef);
        sent++;
      } else if (/pin/i.test(net.errorText(r.data))) {
        say(flushStatus, net.errorText(r.data), 'error');
        stopped = true;
        break;
      } else {
        removeFromQueue(item.clientRef);
        problems.push(item.account + ': ' + net.errorText(r.data));
      }
    }

    if (sent > 0) {
      try { await net.request('POST', '/transactions/sync'); } catch (err) { }
    }
    flushPin.value = '';
    flushBtn.disabled = false;
    renderQueue();

    if (!stopped) {
      var text = 'Sent ' + sent + ' transfer(s). Open History to see the result.';
      if (problems.length) text += ' Not sent: ' + problems.join(' | ');
      var after = loadQueue().length > 0 ? flushStatus : statusLine;
      say(after, text, problems.length ? 'error' : 'success');
    }
  });

  // ---- the demo switch and the "other banks" popup ----
  simulate.checked = net.isSimulated();
  simulate.addEventListener('change', function () { net.setSimulated(simulate.checked); });
  net.onChange(function (online) {
    refreshMode();
    if (online && loadQueue().length > 0) {
      say(flushStatus, 'You are back online. Enter your PIN to send your waiting transfers.', 'info');
    }
  });

  $('tabBank').addEventListener('click', function () { bankModal.style.display = 'flex'; });
  $('bankClose').addEventListener('click', function () { bankModal.style.display = 'none'; });

  refreshMode();
})();