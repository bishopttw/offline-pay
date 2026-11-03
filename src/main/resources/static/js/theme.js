(function () {
  var KEY = 'offlinepay.theme';
  function stored() {
     try {
       return localStorage.getItem(KEY);
     } catch (e) {
        return null;
     }
  }

  function preferred() {
     return (window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
  }
  function apply(theme) {
     document.documentElement.setAttribute('data-theme', theme);
  }

  var current = stored() || preferred();
  apply(current);

  document.addEventListener('DOMContentLoaded', function () {
    var button = document.createElement('button');
    button.type = 'button';
    button.id = 'themeToggle';
    button.setAttribute('aria-label', 'Switch between dark and light mode');
    function label() {
      button.textContent = current === 'dark' ? 'Light' : 'Dark';
    }
    label();
    button.addEventListener('click', function () {
       current = current === 'dark' ? 'light' : 'dark';
       apply(current);
       try {
         localStorage.setItem(KEY, current);
       } catch (e) { }
       label();
    })
    document.body.appendChild(button);
  });
})();