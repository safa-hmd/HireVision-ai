/* ============================================================
   HireVision AI — Login / Auth JS
   ============================================================ */

/* ── Toggle password visibility ── */
function togglePwd(inputId, iconId) {
  const input = document.getElementById(inputId);
  const icon  = document.getElementById(iconId);
  if (!input) return;
  const isHidden = input.type === 'password';
  input.type = isHidden ? 'text' : 'password';
  icon.setAttribute('data-lucide', isHidden ? 'eye-off' : 'eye');
  lucide.createIcons();
}

/* ── Show loading state on button ── */
function setLoading(btnId, labelId, iconId, loading) {
  const btn   = document.getElementById(btnId);
  const label = document.getElementById(labelId);
  const icon  = document.getElementById(iconId);
  if (!btn) return;
  btn.disabled = loading;
  if (label) label.textContent = loading ? 'Veuillez patienter…' : btn.dataset.label || label.textContent;
  if (icon)  icon.style.display = loading ? 'none' : '';
}

/* ── Toast ── */
function showToast(msg, type = 'success') {
  const colors = { success: '#10B981', danger: '#EF4444', warning: '#F59E0B', info: '#2563EB' };
  const t = document.createElement('div');
  t.style.cssText = `position:fixed;bottom:24px;right:24px;z-index:9999;background:${colors[type]||colors.success};color:#fff;padding:13px 20px;border-radius:10px;font-family:'DM Sans',sans-serif;font-size:.9rem;font-weight:600;box-shadow:0 8px 32px rgba(0,0,0,.2);animation:tin .25s ease;max-width:320px;`;
  t.textContent = msg;
  const s = document.createElement('style');
  s.textContent = `@keyframes tin{from{transform:translateY(14px);opacity:0}to{transform:translateY(0);opacity:1}}`;
  document.head.appendChild(s);
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}

/* ── Simple email validator ── */
function isEmail(v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v); }

/* ── Handle login form submit ── */
function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('email')?.value.trim();
  const pwd   = document.getElementById('password')?.value;
  let valid = true;

  const emailErr = document.getElementById('email-err');
  const pwdErr   = document.getElementById('pwd-err');

  if (emailErr) emailErr.classList.remove('show');
  if (pwdErr)   pwdErr.classList.remove('show');

  if (!email || !isEmail(email)) {
    if (emailErr) emailErr.classList.add('show');
    valid = false;
  }
  if (!pwd || pwd.length < 4) {
    if (pwdErr) pwdErr.classList.add('show');
    valid = false;
  }
  if (!valid) return;

  // Simulate auth
  const btn = document.getElementById('login-btn');
  const lbl = document.getElementById('btn-label');
  if (btn) btn.disabled = true;
  if (lbl) lbl.textContent = 'Connexion en cours…';

  setTimeout(() => {
    // Demo routing
    if (email.includes('admin')) {
      window.location.href = '../backoffice/html/index.html';
    } else {
      window.location.href = '../frontoffice/html/index.html';
    }
  }, 1200);
}

/* ── Demo quick login ── */
function demoLogin(role) {
  const emailEl = document.getElementById('email');
  const pwdEl   = document.getElementById('password');
  if (emailEl) emailEl.value = role === 'admin' ? 'admin@hirevision.ai' : 'jean@exemple.com';
  if (pwdEl)   pwdEl.value   = 'demo1234';
  showToast('Connexion démo ' + (role === 'admin' ? 'Admin' : 'Candidat') + '…', 'info');
  setTimeout(() => {
    window.location.href = role === 'admin'
      ? '../backoffice/html/index.html'
      : '../frontoffice/html/index.html';
  }, 900);
}

/* ── Social login stub ── */
function handleSocial(provider) {
  showToast('Redirection vers ' + provider + '…', 'info');
  setTimeout(() => { window.location.href = '../frontoffice/html/index.html'; }, 1000);
}

/* ── Password strength ── */
function checkStrength(pwd) {
  let score = 0;
  if (pwd.length >= 8)              score++;
  if (/[A-Z]/.test(pwd))           score++;
  if (/[0-9]/.test(pwd))           score++;
  if (/[^A-Za-z0-9]/.test(pwd))    score++;
  return score; // 0–4
}

function updateStrength(inputId, barId, labelId) {
  const val   = document.getElementById(inputId)?.value || '';
  const bars  = document.querySelectorAll(`#${barId} > div`);
  const label = document.getElementById(labelId);
  const score = checkStrength(val);
  const colors = ['#EF4444','#F59E0B','#3B82F6','#10B981'];
  const labels = ['Très faible','Moyen','Bon','Excellent'];
  bars.forEach((b, i) => {
    b.style.background = i < score ? colors[score - 1] : 'var(--border)';
  });
  if (label) {
    label.textContent = val.length ? labels[score - 1] || '' : '';
    label.style.color = colors[score - 1] || 'var(--text-muted)';
  }
}

/* ── Register multi-step ── */
let currentStep = 1;
const totalSteps = 3;

function goToStep(n) {
  if (n < 1 || n > totalSteps) return;
  document.querySelectorAll('.register-step').forEach(s => {
    s.style.display = s.dataset.step == n ? 'block' : 'none';
  });
  document.querySelectorAll('.step').forEach((s, i) => {
    s.classList.toggle('active', i + 1 === n);
    s.classList.toggle('done',   i + 1 < n);
  });
  currentStep = n;
  // Update progress text
  const prog = document.getElementById('step-progress');
  if (prog) prog.textContent = `Étape ${n} sur ${totalSteps}`;
}

function nextStep() {
  if (currentStep === 1) {
    const firstName = document.getElementById('firstName')?.value.trim();
    const lastName  = document.getElementById('lastName')?.value.trim();
    const email     = document.getElementById('reg-email')?.value.trim();
    if (!firstName || !lastName) { showToast('Veuillez renseigner votre nom complet.', 'warning'); return; }
    if (!email || !isEmail(email)) { showToast('Adresse email invalide.', 'warning'); return; }
  }
  if (currentStep === 2) {
    const pwd  = document.getElementById('reg-pwd')?.value;
    const pwd2 = document.getElementById('reg-pwd2')?.value;
    if (!pwd || pwd.length < 8) { showToast('Le mot de passe doit contenir au moins 8 caractères.', 'warning'); return; }
    if (pwd !== pwd2) { showToast('Les mots de passe ne correspondent pas.', 'danger'); return; }
  }
  goToStep(currentStep + 1);
}

function prevStep() { goToStep(currentStep - 1); }

function handleRegister(e) {
  if (e) e.preventDefault();
  const plan = document.querySelector('.plan-card.selected')?.dataset.plan || 'free';
  const btn = document.getElementById('reg-btn');
  if (btn) { btn.disabled = true; btn.textContent = 'Création du compte…'; }
  showToast('Compte créé avec succès !', 'success');
  setTimeout(() => { window.location.href = '../frontoffice/html/index.html'; }, 1500);
}

function selectPlan(el, plan) {
  document.querySelectorAll('.plan-card').forEach(c => c.classList.remove('selected'));
  el.classList.add('selected');
}

/* ── Forgot password ── */
function handleForgot(e) {
  e.preventDefault();
  const email = document.getElementById('forgot-email')?.value.trim();
  if (!email || !isEmail(email)) {
    showToast('Veuillez entrer une adresse email valide.', 'warning');
    return;
  }
  const btn = document.getElementById('forgot-btn');
  if (btn) { btn.disabled = true; btn.textContent = 'Envoi en cours…'; }
  setTimeout(() => {
    document.getElementById('forgot-form').style.display = 'none';
    document.getElementById('forgot-success').style.display = 'block';
  }, 1200);
}
