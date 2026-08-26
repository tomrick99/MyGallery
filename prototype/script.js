/**
 * TOM RICK — Photography Archive · Visual Prototype
 * 所有渲染均基于 mockPhotos.js 中的 MOCK_PHOTOS 数据。
 */
(function () {
  "use strict";

  var photos = window.MOCK_PHOTOS || MOCK_PHOTOS;

  /* ---------- 1. Hero：随机精选照片 ---------- */
  function renderHero() {
    var pool = photos.filter(function (p) { return p.featured; });
    if (!pool.length) pool = photos;
    var pick = pool[Math.floor(Math.random() * pool.length)];

    var img = document.getElementById("heroImage");
    var caption = document.getElementById("heroCaption");
    img.src = pick.src;
    img.alt = pick.title;
    caption.textContent = pick.title + " — " + (pick.location || "") + " · " + pick.year;
  }

  /* ---------- 2. 横向照片流 ---------- */
  function renderStream() {
    var stream = document.getElementById("stream");
    // 为了轨道感，照片集重复一轮，让流更长
    var items = photos.concat(photos);
    var variants = ["a", "b", "c"];

    items.forEach(function (p, i) {
      var el = document.createElement("div");
      el.className = "stream__item stream__item--" + variants[i % 3];
      el.innerHTML =
        '<figure><img src="' + p.src + '" alt="' + p.title + '" loading="lazy" /></figure>' +
        "<figcaption>" + p.title + "</figcaption>";
      stream.appendChild(el);
    });
  }

  /* 滚轮 / 触控板纵向滚动 → 横向移动；空闲时缓慢自动漂移 */
  function initStreamMotion() {
    var stream = document.getElementById("stream");
    var idle = true;
    var idleTimer = null;

    stream.addEventListener("wheel", function (e) {
      if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
        e.preventDefault();
        stream.scrollLeft += e.deltaY;
      }
      pauseDrift();
    }, { passive: false });

    // 拖拽支持
    var dragging = false, startX = 0, startScroll = 0;
    stream.addEventListener("pointerdown", function (e) {
      dragging = true; startX = e.clientX; startScroll = stream.scrollLeft;
      stream.classList.add("is-dragging");
      pauseDrift();
    });
    window.addEventListener("pointermove", function (e) {
      if (!dragging) return;
      stream.scrollLeft = startScroll - (e.clientX - startX);
    });
    window.addEventListener("pointerup", function () {
      dragging = false;
      stream.classList.remove("is-dragging");
    });

    function pauseDrift() {
      idle = false;
      clearTimeout(idleTimer);
      idleTimer = setTimeout(function () { idle = true; }, 2600);
    }

    // 缓慢自动漂移（到末尾后回绕）
    (function drift() {
      if (idle && !dragging) {
        stream.scrollLeft += 0.45;
        if (stream.scrollLeft >= stream.scrollWidth - stream.clientWidth - 1) {
          stream.scrollLeft = 0;
        }
      }
      requestAnimationFrame(drift);
    })();
  }

  /* ---------- 3. 时间归档（year → month 分组） ---------- */
  function renderArchive() {
    var root = document.getElementById("archiveRoot");

    // 按 year desc → month 出现顺序分组
    var years = {};
    photos.forEach(function (p) {
      if (!years[p.year]) years[p.year] = {};
      if (!years[p.year][p.month]) years[p.year][p.month] = [];
      years[p.year][p.month].push(p);
    });

    Object.keys(years).sort(function (a, b) { return b - a; }).forEach(function (year) {
      var months = years[year];
      var count = Object.keys(months).reduce(function (n, m) { return n + months[m].length; }, 0);

      var yearEl = document.createElement("div");
      yearEl.className = "archive-year reveal";
      yearEl.innerHTML = '<h2 class="archive-year__label">' + year + "<small>" +
        count + " FRAMES</small></h2>";

      Object.keys(months).forEach(function (month) {
        var monthEl = document.createElement("div");
        monthEl.className = "archive-month";
        var html = '<div class="archive-month__label">' + month + "</div>" +
          '<div class="archive-month__grid">';

        months[month].forEach(function (p, i) {
          html +=
            '<div class="archive-card archive-card--' + (i % 4) + '">' +
              '<figure><img src="' + p.src + '" alt="' + p.title + '" loading="lazy" /></figure>' +
              "<figcaption><span>" + p.title + "</span><span>" + (p.location || "") + "</span></figcaption>" +
            "</div>";
        });

        html += "</div>";
        monthEl.innerHTML = html;
        yearEl.appendChild(monthEl);
      });

      root.appendChild(yearEl);
    });
  }

  /* ---------- 4. 滚动入场动效 ---------- */
  function initReveal() {
    var targets = document.querySelectorAll(".reveal, .archive-card, .stream__item");
    targets.forEach(function (t) { t.classList.add("reveal"); });

    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08 });

    targets.forEach(function (t) { io.observe(t); });
  }

  /* ---------- init ---------- */
  renderHero();
  renderStream();
  renderArchive();
  initStreamMotion();
  initReveal();
})();
