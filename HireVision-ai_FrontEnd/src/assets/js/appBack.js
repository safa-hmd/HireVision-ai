/* ============================================================
   HireVision AI — BackOffice JS
   ============================================================ */

/* ── Tabs ── */
function initTabs() {
  document.querySelectorAll('.tabs-list').forEach(list => {
    const triggers = list.querySelectorAll('.tab-trigger');
    const root = list.closest('.tabs-root') || document;
    triggers.forEach(btn => {
      btn.addEventListener('click', () => {
        triggers.forEach(t => t.classList.remove('active'));
        btn.classList.add('active');
        root.querySelectorAll('.tab-panel').forEach(p => {
          p.classList.toggle('active', p.dataset.panel === btn.dataset.tab);
        });
      });
    });
    if (!list.querySelector('.tab-trigger.active')) triggers[0]?.click();
  });
}

/* ── Active nav ── */
function initActiveNav() {
  const path = window.location.pathname.split('/').pop();
  document.querySelectorAll('.sidebar-nav a').forEach(a => {
    if (a.getAttribute('href') && a.getAttribute('href').includes(path))
      a.classList.add('active');
  });
}

/* ── Sidebar mobile ── */
function initSidebar() {
  const btn = document.getElementById('sidebar-toggle');
  const sb  = document.querySelector('.sidebar');
  if (btn && sb) btn.addEventListener('click', () => sb.classList.toggle('open'));
}

/* ── Toast ── */
function showToast(msg, type = 'success') {
  const colors = { success:'#10B981', danger:'#EF4444', warning:'#F59E0B', info:'#2563EB' };
  const t = document.createElement('div');
  t.style.cssText = `position:fixed;bottom:24px;right:24px;z-index:9999;background:${colors[type]||colors.success};color:#fff;padding:13px 20px;border-radius:10px;font-family:'DM Sans',sans-serif;font-size:.88rem;font-weight:600;box-shadow:0 8px 32px rgba(0,0,0,.18);animation:toastIn .25s ease;max-width:320px;`;
  t.textContent = msg;
  const s = document.createElement('style');
  s.textContent = `@keyframes toastIn{from{transform:translateY(14px);opacity:0}to{transform:translateY(0);opacity:1}}`;
  document.head.appendChild(s);
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3200);
}

/* ── Charts ── */
function drawLineChart(id, data, label = 'Valeur', color = '#4F46E5') {
  const el = document.getElementById(id);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'line',
    data: {
      labels: data.map(d => d.label),
      datasets: [{ label, data: data.map(d => d.value), borderColor: color, backgroundColor: color + '18', borderWidth: 2.5, tension: 0.35, fill: true, pointRadius: 4, pointBackgroundColor: color }]
    },
    options: { responsive:true, maintainAspectRatio:false, plugins:{ legend:{ display:false } }, scales:{ x:{ grid:{ color:'#E2E8F0' }, ticks:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } }, y:{ grid:{ color:'#E2E8F0' }, ticks:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } } } }
  });
}

function drawBarChart(id, data, label = 'Valeur', color = '#4F46E5') {
  const el = document.getElementById(id);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'bar',
    data: {
      labels: data.map(d => d.label),
      datasets: [{ label, data: data.map(d => d.value), backgroundColor: color + 'CC', borderRadius: 8, borderSkipped: false }]
    },
    options: { responsive:true, maintainAspectRatio:false, plugins:{ legend:{ display:false } }, scales:{ x:{ grid:{ display:false }, ticks:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } }, y:{ grid:{ color:'#E2E8F0' }, ticks:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } } } }
  });
}

function drawMultiBar(id, datasets, labels) {
  const el = document.getElementById(id);
  if (!el || typeof Chart === 'undefined') return;
  const colors = ['#4F46E5','#10B981','#F59E0B','#EF4444'];
  new Chart(el, {
    type: 'bar',
    data: {
      labels,
      datasets: datasets.map((ds, i) => ({ label: ds.label, data: ds.data, backgroundColor: colors[i % colors.length] + 'CC', borderRadius: 6, borderSkipped: false }))
    },
    options: { responsive:true, maintainAspectRatio:false, plugins:{ legend:{ position:'bottom', labels:{ font:{ family:'DM Sans', size:11 }, color:'#64748B', padding:14, boxWidth:12 } } }, scales:{ x:{ grid:{ display:false }, ticks:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } }, y:{ grid:{ color:'#E2E8F0' }, ticks:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } } } }
  });
}

function drawDoughnutChart(id, data) {
  const el = document.getElementById(id);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'doughnut',
    data: {
      labels: data.map(d => d.label),
      datasets: [{ data: data.map(d => d.value), backgroundColor: data.map(d => d.color), borderWidth: 0, hoverOffset: 4 }]
    },
    options: { responsive:true, maintainAspectRatio:false, cutout:'65%', plugins:{ legend:{ position:'bottom', labels:{ font:{ family:'DM Sans', size:11 }, color:'#64748B', padding:14, boxWidth:12 } } } }
  });
}

function drawRadarChart(id, data, color = '#4F46E5') {
  const el = document.getElementById(id);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'radar',
    data: {
      labels: data.map(d => d.label),
      datasets: [{ label:'Score', data: data.map(d => d.value), borderColor: color, backgroundColor: color+'30', borderWidth:2, pointBackgroundColor: color }]
    },
    options: { responsive:true, maintainAspectRatio:false, plugins:{ legend:{ display:false } }, scales:{ r:{ grid:{ color:'#E2E8F0' }, ticks:{ display:false }, pointLabels:{ font:{ family:'DM Sans', size:11 }, color:'#64748B' } } } }
  });
}

/* ── Count-up ── */
function animateNumbers() {
  document.querySelectorAll('[data-countup]').forEach(el => {
    const target = parseFloat(el.dataset.countup);
    const suffix = el.dataset.suffix || '';
    const start = performance.now();
    const duration = 900;
    function step(now) {
      const p = Math.min((now - start) / duration, 1);
      const e = 1 - Math.pow(1 - p, 3);
      el.textContent = (target % 1 === 0 ? Math.floor(e * target) : (e * target).toFixed(1)) + suffix;
      if (p < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  });
}

/* ── Confirm dialog ── */
function confirmAction(msg, cb) {
  if (window.confirm(msg)) cb();
}

/* ── Simple search filter ── */
function initTableSearch(inputId, tableId) {
  const input = document.getElementById(inputId);
  const table = document.getElementById(tableId);
  if (!input || !table) return;
  input.addEventListener('input', () => {
    const q = input.value.toLowerCase();
    table.querySelectorAll('tbody tr').forEach(row => {
      row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
  });
}

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  initSidebar();
  initActiveNav();
  animateNumbers();
});
