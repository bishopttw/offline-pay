(function () {
  var body = document.body;
  var accountId = body.dataset.accountId;
  var QUEUE_KEY = 'offlinepay.queue.' + accountId;
  var ONLINE_FEE = Number(body.dataset.onlineFee);
  var OFFLINE_FEE = Number(body.dataset.offlineFee);
  var net = window.OfflinePayNet;

  var form = document.getElementById('sendForm');
  var emailInput = document.getElementById('receiverEmail');
  var amountInput = document.getElementById('amount');
  var pinInput = document.getElementById('pin');
  var pinRow = document.getElementById('pinRow');
  var nameLine = document.getElementById('nameLine');
  var feeLine = document.getElementById('feeLine');
  var modeNote = document.getElementById('modeNote');
  var messageBox = document.getElementById('message');
  var netBadge = document.getElementById('netBadge');
  var simulate = document.getElementById('simulate');
  var queueCard = document.getElementById('queueCard');
  var queueList = document.getElementById('queueList');
  var flushBox = document.getElementById('flushBox');
  var flushPin = document.getElementById('flushPin');
  var flushBtn = document.getElementById('flushBtn');
  var bankModal = document.getElementById('bankModal');

  function naira(value) {
    return '₦' + Number(value).toLocaleString('en-NG',
      { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function makeId() {
     if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
     return Date.now() + '-' + Math.random().toString(16).slice(2);
  }

  var messageTimer = null;
  var TOAST_COLORS = {
    success: ['#DCFCE7', '#166534'],
    error: ['#FEE2E2', '#991B1B'],
    info: ['#E0E7FF', '#3730A3']
  };
  function showMessage(text, kind) {
      var colors = TOAST_COLORS[kind] || TOAST_COLORS.info;
      messageBox.className = '';
      messageBox.textContent = text;
      messageBox.removeAttribute('hidden');
        messageBox.style.cssText = 'position:fixed;top:12px;left:12px;right:12px;max-width:456px;margin:0 auto;'
            + 'z-index:99999;padding:14px 16px;border-radius:12px;font-size:1rem;'
            + 'box-shadow:0 4px 16px rgba(0,0,0,.25);background:' + colors[0] + ';color:' + colors[1] + ';';
      clearTimeout(messageTimer);
      messageTimer = setTimeout(function () { messageBox.setAttribute('hidden', ''); }, 8000);
    }
  function clearMessage() { messageBox.setAttribute('hidden', ''); }

    // ---- the waiting queue lives in this browser, one per account ----
  function loadQueue() {
      try { return JSON.parse(localStorage.getItem(QUEUE_KEY)) || []; } catch (e) { return []; }
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
     queueCard.hidden = queue.length === 0;
     flushBox.hidden = !(online && queue.length > 0);
     queueList.textContent = '';
     queue.forEach(function (item) {
     var li = document.createElement('li');
     var text = document.createElement('span');
     text.textContent = naira(item.amount) + ' to ' + item.email + ' (waiting)';
     var remove = document.createElement('button');
     remove.type = 'button';
     remove.className = 'op-link-btn';
     remove.textContent = 'Cancel';
     remove.addEventListener('click', function () {
         removeFromQueue(item.clientRef);
         renderQueue();
     });
         li.appendChild(text);
         li.appendChild(remove);
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
    netBadge.textContent = online ? 'Online' : 'Offline';
    pinRow.hidden = !online;
    modeNote.textContent = online
    ? 'Online: sent right away.'
    : 'Offline: saved on this phone and sent when you are back online.';
        updateFee();
        renderQueue();
}

      // ---- name preview under the email box ----
      emailInput.addEventListener('blur', async function () {
          nameLine.textContent = '';
          var email = emailInput.value.trim().toLowerCase();
          if (!email) return;
          if (!net.isOnline()) {
            nameLine.textContent = 'Offline: cannot check the name. Double-check the email.';
            return;
          }
          try {
            var r = await net.request('GET', '/transactions/lookup?email=' + encodeURIComponent(email));
            nameLine.textContent = r.ok ? 'Sending to ' + r.data.name : net.errorText(r.data);
          } catch (e) { }
        });
        amountInput.addEventListener('input', updateFee);

         // ---- pressing Send ----
      form.addEventListener('submit', async function (event) {
            event.preventDefault();
            clearMessage();

            var email = emailInput.value.trim().toLowerCase();
            var amount = amountInput.value.trim();
            var pin = pinInput.value.trim();

            if (!/^\S+@\S+\.\S+$/.test(email)) { showMessage('Enter a valid email.', 'error'); return; }
            if (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
              showMessage('Enter a valid amount (up to 2 decimal places).', 'error'); return;
            }
            var clientRef = makeId();

           if (net.isOnline()) {
             if (!/^\d{4}$/.test(pin)) { showMessage('Enter your 4-digit PIN.', 'error'); return; }
             var r = null;
             try {
               r = await net.request('POST', '/transactions/send',
                 { receiverEmail: email, amount: amount, pin: pin, clientRef: clientRef });
             } catch (err) {
               if (err.message === 'SESSION') return;
               r = null;
             }
             if (r) {
               pinInput.value = '';
               if (r.ok) {
                 showMessage('Sent ' + naira(amount) + '. Fee ' + naira(r.data.fee) + '.', 'success');
                 form.reset();
                 nameLine.textContent = '';
                 updateFee();
               } else {
                 showMessage(net.errorText(r.data), 'error');
               }
               return;
             }
           }

            var queue = loadQueue();
                queue.push({ clientRef: clientRef, email: email, amount: amount, createdAt: Date.now() });
                saveQueue(queue);
                pinInput.value = '';
                form.reset();
                nameLine.textContent = '';
                updateFee();
                renderQueue();
                showMessage('Saved on this phone. It will be sent when you are back online. Offline fee: '
                  + naira(OFFLINE_FEE) + '.', 'info');
              });

              // ---- back online: send the waiting transfers ----
                flushBtn.addEventListener('click', async function () {
                  clearMessage();
                  var pin = flushPin.value.trim();
                  if (!/^\d{4}$/.test(pin)) { showMessage('Enter your 4-digit PIN.', 'error'); return; }

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
                           { receiverEmail: item.email, amount: item.amount, pin: pin, clientRef: item.clientRef });
                       } catch (err) {
                         if (err.message === 'SESSION') return;
                         showMessage('Connection lost. Your waiting transfers are safe and will be sent later.', 'error');
                         stopped = true;
                         break;
                       }
                        if (r.ok) {
                               removeFromQueue(item.clientRef);
                               sent++;
                        } else if (/pin/i.test(net.errorText(r.data))) {
                             showMessage(net.errorText(r.data), 'error');
                               stopped = true;
                               break;
                        } else {
                               removeFromQueue(item.clientRef);
                               problems.push(item.email + ': ' + net.errorText(r.data));
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
                          showMessage(text, problems.length ? 'error' : 'success');
                        }
                });

                // ---- the demo switch and the "other banks" popup ----
                  simulate.checked = net.isSimulated();
                  simulate.addEventListener('change', function () { net.setSimulated(simulate.checked); });
                  net.onChange(function (online) {
                    refreshMode();
                    if (online && loadQueue().length > 0) {
                      showMessage('You are back online. Enter your PIN to send your waiting transfers.', 'info');
                    }
                  });

                  document.getElementById('tabBank').addEventListener('click', function () { bankModal.hidden = false; });
                  document.getElementById('bankClose').addEventListener('click', function () { bankModal.hidden = true; });

                   refreshMode();



})();
