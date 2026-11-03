(function () {
  var net = window.OfflinePayNet;
  var button = document.getElementById('syncBtn');
  var note = document.getElementById('syncNote');
  if (!button) return;

  function refresh() {
     var online = net.isOnline();
     button.disabled = !online;
     note.textContent = online ? '' : 'Go online to sync.';
  }
  net.onChange(refresh);
  refresh();

  button.addEventListener('click', async function () {
    button.disabled = true;
    note.textContent = 'Syncing...';
    try {
      var r = await net.request('POST', '/transactions/sync');
      if (r.ok) {
         location.reload();
         return;
      }
      note.textContent= new.errorText(r.data);
    } catch (err) {
      if (err.message === 'SESSION') return;
      note.textContent = 'Could not reach the server.';
    }
    button.disabled = false;
  });
})();