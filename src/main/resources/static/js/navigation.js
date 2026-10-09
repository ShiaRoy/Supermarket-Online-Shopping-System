(() => {
  const mobileLayout = window.matchMedia("(max-width: 960px)");

  document.querySelectorAll(".soss-header").forEach((header) => {
    const nav = header.querySelector(".soss-nav");
    const toggle = header.querySelector(".soss-menu-toggle");
    if (!nav || !toggle) return;

    const adminMenus = [...header.querySelectorAll(".soss-admin-menu")];
    const setMobileOpen = (open) => {
      nav.classList.toggle("is-open", open);
      toggle.setAttribute("aria-expanded", String(open));
      toggle.setAttribute("aria-label", open ? "Close navigation menu" : "Open navigation menu");
    };

    toggle.addEventListener("click", () => {
      setMobileOpen(!nav.classList.contains("is-open"));
    });

    document.addEventListener("click", (event) => {
      adminMenus.forEach((menu) => {
        if (menu.open && !menu.contains(event.target)) menu.open = false;
      });
      if (!header.contains(event.target) && nav.classList.contains("is-open")) {
        setMobileOpen(false);
      }
    });

    nav.addEventListener("click", (event) => {
      if (event.target.closest("a, .soss-logout")) {
        adminMenus.forEach((menu) => { menu.open = false; });
        if (mobileLayout.matches) setMobileOpen(false);
      }
    });

    header.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") return;
      const openMenu = adminMenus.find((menu) => menu.open);
      if (openMenu) {
        openMenu.open = false;
        openMenu.querySelector("summary")?.focus();
      } else if (nav.classList.contains("is-open")) {
        setMobileOpen(false);
        toggle.focus();
      }
    });

    mobileLayout.addEventListener("change", (event) => {
      if (!event.matches) setMobileOpen(false);
    });
  });
})();
