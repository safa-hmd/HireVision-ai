/**
 * Plusieurs composants s'appuient sur des fonctions/objets globaux chargés en
 * dehors d'Angular via des balises <script> dans index.html (lucide-icons,
 * mini "framework" maison pour les toasts/onglets/graphiques du back-office).
 * Ils sont déclarés côté TypeScript avec `declare`, donc le compilateur est
 * content, mais rien ne les définit réellement dans l'environnement Karma :
 * les appeler sans stub fait planter le test avec un ReferenceError.
 *
 * `installGlobalUiStubs()` les définit comme no-op sur `window` avant que le
 * composant ne soit instancié ; `uninstallGlobalUiStubs()` nettoie après coup
 * pour ne pas polluer les autres specs.
 */

const STUBBED_KEYS = [
  'lucide',
  'showToast',
  'confirmAction',
  'animateNumbers',
  'initTabs',
  'drawBarChart',
  'drawDoughnutChart',
  'drawLineChart',
  'drawRadarChart'
] as const;

export function installGlobalUiStubs(): void {
  const w = window as any;
  w.lucide = { createIcons: () => {} };
  w.showToast = () => {};
  w.confirmAction = (_message: string, onConfirm: () => void) => onConfirm();
  w.animateNumbers = () => {};
  w.initTabs = () => {};
  w.drawBarChart = () => {};
  w.drawDoughnutChart = () => {};
  w.drawLineChart = () => {};
  w.drawRadarChart = () => {};
}

export function uninstallGlobalUiStubs(): void {
  const w = window as any;
  for (const key of STUBBED_KEYS) {
    delete w[key];
  }
}
