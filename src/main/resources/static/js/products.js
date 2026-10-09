document.querySelector('.menu-toggle')?.addEventListener('click', function () {
  const nav = document.getElementById('primary-nav');
  const expanded = this.getAttribute('aria-expanded') !== 'true';
  this.setAttribute('aria-expanded', String(expanded));
  nav.classList.toggle('open', expanded);
});

document.querySelectorAll('.catalogue-filter-toggle').forEach((toggle) => {
  const panel = document.getElementById(toggle.getAttribute('aria-controls'));
  if (!panel) return;

  const setExpanded = (expanded) => {
    toggle.setAttribute('aria-expanded', String(expanded));
    panel.hidden = !expanded;
  };

  toggle.addEventListener('click', () => {
    setExpanded(toggle.getAttribute('aria-expanded') !== 'true');
  });

  toggle.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
      setExpanded(false);
      toggle.focus();
    }
  });

  panel.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      setExpanded(false);
      toggle.focus();
    }
  });
});
