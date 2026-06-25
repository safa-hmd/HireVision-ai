/* ============================================================
   HireVision AI — FrontOffice JS
   ============================================================ */

/* ── Tabs ── */
function initTabs() {
  document.querySelectorAll('.tabs-list').forEach(list => {
    const triggers = list.querySelectorAll('.tab-trigger');
    const targetContainer = list.closest('.tabs-root') || document;
    triggers.forEach(btn => {
      btn.addEventListener('click', () => {
        const panel = btn.dataset.tab;
        triggers.forEach(t => t.classList.remove('active'));
        btn.classList.add('active');
        targetContainer.querySelectorAll('.tab-panel').forEach(p => {
          p.classList.toggle('active', p.dataset.panel === panel);
        });
      });
    });
    // Activate first by default
    if (!list.querySelector('.tab-trigger.active')) triggers[0]?.click();
  });
}

/* ── Sidebar mobile toggle ── */
function initSidebar() {
  const toggle = document.getElementById('sidebar-toggle');
  const sidebar = document.querySelector('.sidebar');
  if (toggle && sidebar) {
    toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
  }
}

/* ── Active nav link ── */
function initActiveNav() {
  const path = window.location.pathname.split('/').pop();
  document.querySelectorAll('.sidebar-nav a').forEach(a => {
    const href = a.getAttribute('href');
    if (href && href.includes(path)) a.classList.add('active');
  });
}

/* ── Toast notifications ── */
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  const colors = { success: '#10B981', danger: '#EF4444', warning: '#F59E0B', info: '#2563EB' };
  toast.style.cssText = `
    position:fixed; bottom:24px; right:24px; z-index:9999;
    background:${colors[type] || colors.success}; color:#fff;
    padding:14px 20px; border-radius:10px;
    font-family:'DM Sans',sans-serif; font-size:.9rem; font-weight:600;
    box-shadow:0 8px 32px rgba(0,0,0,.18);
    animation:slideIn .25s ease; max-width:320px;
  `;
  toast.textContent = message;
  const style = document.createElement('style');
  style.textContent = `@keyframes slideIn{from{transform:translateY(16px);opacity:0}to{transform:translateY(0);opacity:1}}`;
  document.head.appendChild(style);
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

/* ── Mini chart (SVG line chart) ── */
function drawLineChart(canvasId, data, label = 'Score', color = '#2563EB') {
  const el = document.getElementById(canvasId);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'line',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        label,
        data: data.map(d => d.value),
        borderColor: color,
        backgroundColor: color + '18',
        borderWidth: 2.5,
        tension: 0.35,
        fill: true,
        pointRadius: 4,
        pointBackgroundColor: color,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { color: '#E2E8F0' }, ticks: { font: { family: 'DM Sans', size: 11 }, color: '#64748B' } },
        y: { grid: { color: '#E2E8F0' }, ticks: { font: { family: 'DM Sans', size: 11 }, color: '#64748B' } }
      }
    }
  });
}

function drawBarChart(canvasId, data, label = 'Score', color = '#2563EB') {
  const el = document.getElementById(canvasId);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'bar',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        label,
        data: data.map(d => d.value),
        backgroundColor: color + 'CC',
        borderRadius: 8,
        borderSkipped: false,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false }, ticks: { font: { family: 'DM Sans', size: 11 }, color: '#64748B' } },
        y: { grid: { color: '#E2E8F0' }, ticks: { font: { family: 'DM Sans', size: 11 }, color: '#64748B' } }
      }
    }
  });
}

function drawRadarChart(canvasId, data, color = '#2563EB') {
  const el = document.getElementById(canvasId);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'radar',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        label: 'Score',
        data: data.map(d => d.value),
        borderColor: color,
        backgroundColor: color + '30',
        borderWidth: 2,
        pointBackgroundColor: color,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { r: { grid: { color: '#E2E8F0' }, ticks: { display: false }, pointLabels: { font: { family: 'DM Sans', size: 11 }, color: '#64748B' } } }
    }
  });
}

function drawDoughnutChart(canvasId, data) {
  const el = document.getElementById(canvasId);
  if (!el || typeof Chart === 'undefined') return;
  new Chart(el, {
    type: 'doughnut',
    data: {
      labels: data.map(d => d.label),
      datasets: [{
        data: data.map(d => d.value),
        backgroundColor: data.map(d => d.color),
        borderWidth: 0,
        hoverOffset: 4,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '65%',
      plugins: { legend: { position: 'bottom', labels: { font: { family: 'DM Sans', size: 11 }, color: '#64748B', padding: 16, boxWidth: 12 } } }
    }
  });
}

/* ── File upload ── */
function initFileUpload() {
  document.querySelectorAll('.drop-zone').forEach(zone => {
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragging'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('dragging'));
    zone.addEventListener('drop', e => {
      e.preventDefault(); zone.classList.remove('dragging');
      const file = e.dataTransfer.files[0];
      if (file) zone.dataset.filename = file.name;
    });
    zone.querySelector('input[type=file]')?.addEventListener('change', e => {
      const file = e.target.files[0];
      if (file) { zone.dataset.filename = file.name; showToast('Fichier sélectionné : ' + file.name); }
    });
  });
}

/* ── Recording timer ── */
let recordingInterval = null;
function startTimer(displayId) {
  let secs = 0;
  const el = document.getElementById(displayId);
  recordingInterval = setInterval(() => {
    secs++;
    const m = String(Math.floor(secs/60)).padStart(2,'0');
    const s = String(secs%60).padStart(2,'0');
    if (el) el.textContent = `${m}:${s}`;
  }, 1000);
}
function stopTimer() { clearInterval(recordingInterval); }

/* ── Form validation ── */
function validateForm(formId) {
  const form = document.getElementById(formId);
  if (!form) return true;
  let valid = true;
  form.querySelectorAll('[required]').forEach(input => {
    if (!input.value.trim()) {
      input.style.borderColor = 'var(--danger)';
      valid = false;
    } else {
      input.style.borderColor = '';
    }
  });
  return valid;
}

/* ── Animate numbers (count-up) ── */
function animateNumbers() {
  document.querySelectorAll('[data-countup]').forEach(el => {
    const target = parseFloat(el.dataset.countup);
    const suffix = el.dataset.suffix || '';
    const duration = 800;
    const start = performance.now();
    function update(now) {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      el.textContent = (target % 1 === 0 ? Math.floor(eased * target) : (eased * target).toFixed(1)) + suffix;
      if (progress < 1) requestAnimationFrame(update);
    }
    requestAnimationFrame(update);
  });
}

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  initSidebar();
  initActiveNav();
  initFileUpload();
  animateNumbers();
});
