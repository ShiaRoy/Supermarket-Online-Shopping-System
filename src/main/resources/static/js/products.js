document.querySelector('.menu-toggle')?.addEventListener('click', function () {
  const nav = document.getElementById('primary-nav');
  const expanded = this.getAttribute('aria-expanded') !== 'true';
  this.setAttribute('aria-expanded', String(expanded));
  nav.classList.toggle('open', expanded);
});
