import {
  AfterViewInit, Component, ElementRef,
  OnDestroy, OnInit, ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { CvService }   from '../../services/cv.service';
import { firstValueFrom } from 'rxjs';
import { timeout, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

declare const lucide: any;
declare function showToast(msg: string, type?: string): void;

interface Question {
  id:             number;
  category:       string;
  question:       string;
  difficulty:     string;
  tip:            string;
  time_suggested: number;
}

@Component({
  selector:    'app-interview-session',
  templateUrl: './interview-session.component.html',
  styleUrls:   ['./interview-session.component.css']
})
export class InterviewSessionComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('webcam')            webcamEl!:          ElementRef<HTMLVideoElement>;
  @ViewChild('faceCanvas')        faceCanvas!:        ElementRef<HTMLCanvasElement>;
  @ViewChild('liveTranscriptEl')  liveTranscriptEl?:  ElementRef<HTMLParagraphElement>;

  savedInterviewId: number | null = null;

  specialty:    any      = null;
  questions:    Question[] = [];
  currentIndex: number   = 0;
  isLoading:    boolean  = true;
  isRecording:  boolean  = false;
  isAnalyzing:  boolean  = false;
  isCameraOn:   boolean  = true;
  latestCv:     any      = null;

  /** true si le chargement des questions a échoué / a expiré → affiche un
   *  bouton "Réessayer" au lieu de laisser un écran vide ou un spinner infini. */
  loadError = false;

  /** Empêche ngOnInit de tout réinitialiser en double (webcam, timers,
   *  intervals) si Angular relance le composant sans passer par ngOnDestroy. */
  private sessionStarted = false;

  /** Empêche d'empiler plusieurs requêtes d'analyse de frame en parallèle
   *  si le réseau/serveur IA est lent. */
  private isCapturingFrame = false;

  /** Nombre de tentatives d'attache webcam avant abandon (évite une boucle
   *  setTimeout infinie si l'élément #webcam n'apparaît jamais dans le DOM). */
  private attachWebcamRetries = 0;

  /** Watchdog : si le chargement des questions dépasse ce délai sans
   *  réponse (backend down, CORS, etc.), on arrête le spinner et on
   *  propose de réessayer plutôt que de bloquer l'utilisateur. */
  private loadingWatchdog: any;

  // ── Timer ──
  timer         = 0;
  timerInterval: any;

  // ── Webcam / Speech ──
  videoStream:        MediaStream | null = null;
  recognition:        any                = null;
  transcript:         string             = '';
  interimTranscript:  string             = '';
  answers:            any[]              = [];

  // ── Behavior metrics (simulation + real CV) ──
  contactVisuel = 85; posture   = 78;
  engagement    = 90; clarteVocale = 88;
  confiance     = 82; stress    = 30;

  behaviorInterval: any;
  audioBarInterval: any;

  // ── Voice metrics (retournés par IA après chaque réponse) ──
  voiceMetrics: any = null;

  // ── Audio visualizer bars ──
  audioBars: number[] = Array(24).fill(8);

  // ── AI Tips (mis à jour dynamiquement) ──
  aiTips: string[] = [
    'Maintenez le contact visuel avec la caméra',
    'Parlez clairement et avec confiance',
    'Faites de courtes pauses avant de répondre',
    'Utilisez des exemples concrets (méthode STAR)'
  ];

  // ── Voix (Text-to-Speech) ──
  isVoiceEnabled = true;
  private speechSynth = (typeof window !== 'undefined') ? window.speechSynthesis : null;

  // ── Difficulté progressive : score courant ──
  private currentRunningScore = 0;
  private askedIds:     number[] = [];

  private readonly javaUrl   = 'http://localhost:8086/HireVision';

  // ── Computed ──
  get currentQuestion(): Question | null { return this.questions[this.currentIndex] || null; }
  get progress():        number { return this.questions.length ? Math.round((this.currentIndex / this.questions.length) * 100) : 0; }
  get timerDisplay():    string {
    const m = Math.floor(this.timer / 60).toString().padStart(2, '0');
    const s = (this.timer % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }
  get questions_total(): number { return this.specialty?.count || this.questions.length || 15; }

  get behaviorMetrics() {
    return [
      { label: 'Contact visuel', icon: 'eye',       value: this.contactVisuel,  color: this.metricColor(this.contactVisuel) },
      { label: 'Posture',        icon: 'user',       value: this.posture,        color: this.metricColor(this.posture) },
      { label: 'Engagement',     icon: 'heart',      value: this.engagement,     color: this.metricColor(this.engagement) },
      { label: 'Clarté vocale',  icon: 'volume-2',   value: this.clarteVocale,   color: this.metricColor(this.clarteVocale) },
      { label: 'Confiance',      icon: 'shield',     value: this.confiance,      color: this.metricColor(this.confiance) },
      { label: 'Niveau de stress', icon: 'activity', value: this.stress,         color: this.stressColor(this.stress) },
    ];
  }

  trackByLabel(index: number, item: { label: string }): string {
  return item.label;
}

  get voiceMetricsDisplay() {
    if (!this.voiceMetrics) return [];
    return [
      { label: 'Technique',     value: this.voiceMetrics.score_technique     || 0, color: this.metricColor(this.voiceMetrics.score_technique) },
      { label: 'Communication', value: this.voiceMetrics.score_communication || 0, color: this.metricColor(this.voiceMetrics.score_communication) },
      { label: 'Confiance',     value: this.voiceMetrics.score_confiance     || 0, color: this.metricColor(this.voiceMetrics.score_confiance) },
    ];
  }

  private metricColor(v: number): string {
    if (v >= 75) return '#10B981';
    if (v >= 50) return '#3B82F6';
    return '#F59E0B';
  }
  private stressColor(v: number): string {
    if (v <= 30) return '#10B981';
    if (v <= 60) return '#F59E0B';
    return '#EF4444';
  }
  private clamp(v: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, v));
  }

  constructor(
    private router:      Router,
    private http:        HttpClient,
    private authService: AuthService,
    private cvService:   CvService
  ) {}

  ngOnInit(): void {
    if (this.sessionStarted) return; // évite une double initialisation
    const stored = sessionStorage.getItem('interview_specialty');
    if (!stored) { this.router.navigate(['/frontoffice/interviewPrep']); return; }

    let parsed: any;
    try {
      parsed = JSON.parse(stored);
    } catch {
      // sessionStorage corrompu → on ne bloque jamais l'utilisateur ici
      showToast('Session invalide, veuillez recommencer', 'danger');
      this.router.navigate(['/frontoffice/interviewPrep']);
      return;
    }

    this.sessionStarted = true;
    this.specialty = parsed;
    this.loadLatestCv();
    this.loadQuestions();
    this.startTimer();
    this.initWebcam();
    this.initSpeechRecognition();
    this.startBehaviorSimulation();
  }

  ngAfterViewInit(): void { setTimeout(() => this.refreshIcons(), 100); }

  /** Wrapper sûr autour de lucide.createIcons() : ne throw jamais si le CDN
   *  (unpkg.com) est lent/bloqué/hors-ligne ou si l'objet n'est pas encore
   *  chargé. Sans ça, une seule icône ratée lève une exception non catchée
   *  et peut laisser l'écran figé/blanc. */
  private refreshIcons(): void {
    try {
      if (typeof lucide !== 'undefined') lucide.createIcons();
    } catch (e) {
      console.error('lucide.createIcons error:', e);
    }
  }

  // ─────────────────────────────────────────────────
  // DATA LOADING
  // ─────────────────────────────────────────────────

  loadLatestCv(): void {
    const userId = this.authService.getCurrentUserId();
    if (userId) {
      this.cvService.getLatest(userId).subscribe({
        next:  (cv)  => { this.latestCv = cv; },
        error: ()    => {}
      });
    }
  }

  loadQuestions(): void {
    this.isLoading = true;
    this.loadError = false;
    const userId = this.authService.getCurrentUserId();

    // Passer userId pour anti-répétition (Spring Boot → Python)
    let url = `${this.javaUrl}/interview/questions/${encodeURIComponent(this.specialty.id)}`
            + (userId ? `?userId=${userId}` : '?');

    // Spécialité "custom" (compétence détectée dans le CV, pas parmi les 6
    // pré-câblées côté Python) : on transmet titre/description/niveau/durée
    // pour que l'IA génère des questions pertinentes au lieu de retomber
    // silencieusement sur des questions Java.
    if (this.specialty.isCustom) {
      url += `&title=${encodeURIComponent(this.specialty.title)}`
           + `&description=${encodeURIComponent(this.specialty.description || '')}`
           + `&level=${encodeURIComponent(this.specialty.level || 'Intermédiaire')}`
           + `&count=${this.specialty.count || 15}`
           + `&duration=${this.specialty.duration || 45}`;
    }

    // Watchdog : filet de sécurité INDÉPENDANT du timeout RxJS ci-dessous.
    // Si pour une raison quelconque (CORS, requête jamais résolue) le
    // subscribe ne retourne jamais, on force l'arrêt du spinner et on
    // affiche un bouton "Réessayer" au bout de 20s.
    clearTimeout(this.loadingWatchdog);
    this.loadingWatchdog = setTimeout(() => {
      if (this.isLoading) {
        this.isLoading = false;
        this.loadError = true;
        showToast('Le chargement des questions prend trop de temps — réessayez', 'warning');
      }
    }, 20_000);

    this.http.get<any>(url).pipe(
      // Filet de sécurité RxJS : même si Spring/Python restent bloqués
      // (pas de réponse), on ne laisse jamais l'observable pendre indéfiniment.
      timeout(18_000),
      catchError(err => throwError(() => err))
    ).subscribe({
      next: (res) => {
        clearTimeout(this.loadingWatchdog);
        this.questions = res?.questions || [];
        this.isLoading = false;
        if (this.questions.length === 0) {
          // Réponse 200 mais sans questions (ex: Python down → fallback
          // vide côté Spring) : on ne laisse pas un écran vide silencieux.
          this.loadError = true;
          showToast('Aucune question reçue — le service IA est peut-être indisponible', 'danger');
        } else {
          setTimeout(() => this.refreshIcons(), 100);
          this.speakCurrentQuestion();
        }
      },
      error: () => {
        clearTimeout(this.loadingWatchdog);
        this.isLoading = false;
        this.loadError = true;
        showToast('Erreur chargement questions — vérifiez que le service IA (Python) tourne bien', 'danger');
      }
    });
  }

  /** Bouton "Réessayer" affiché quand loadError = true. */
  retryLoadQuestions(): void {
    this.loadQuestions();
  }

  // ─────────────────────────────────────────────────
  // TIMER
  // ─────────────────────────────────────────────────

  startTimer(): void {
    this.timerInterval = setInterval(() => this.timer++, 1000);
  }

  // ─────────────────────────────────────────────────
  // WEBCAM + COMPUTER VISION
  // ─────────────────────────────────────────────────

  initWebcam(): void {
    // Repart de zéro à chaque (re)démarrage de la caméra : sinon, si une
    // tentative précédente avait déjà épuisé ses 50 essais, tout nouvel
    // appel (ex: toggleWebcam) abandonnerait immédiatement sans réessayer.
    this.attachWebcamRetries = 0;
    navigator.mediaDevices.getUserMedia({ video: { width: 1280, height: 720 }, audio: true })
      .then(stream => {
        this.videoStream = stream;
        this.attachWebcam();
      })
      .catch(() => showToast('Caméra non disponible — mode texte activé', 'warning'));
  }

  private attachWebcam(): void {
    if (this.webcamEl?.nativeElement) {
      this.attachWebcamRetries = 0;
      const v = this.webcamEl.nativeElement;
      v.srcObject = this.videoStream;
      v.muted = true;
      v.play().catch(console.error);
    } else if (this.attachWebcamRetries < 50) {
      // Max 50 tentatives (~5s) : si l'élément <video> n'apparaît jamais,
      // on abandonne proprement au lieu de boucler indéfiniment.
      this.attachWebcamRetries++;
      setTimeout(() => this.attachWebcam(), 100);
    } else {
      this.videoStream?.getTracks().forEach(t => t.stop());
      this.videoStream = null;
    }
  }

  toggleWebcam(): void {
    if (this.isCameraOn) {
      this.videoStream?.getVideoTracks().forEach(t => t.stop());
      this.videoStream = null;
      this.isCameraOn  = false;
    } else {
      this.isCameraOn = true;
      this.initWebcam();
    }
    setTimeout(() => this.refreshIcons(), 50);
  }

  /** Capture une frame webcam et l'envoie au backend Spring Boot → Python CV */
  private captureAndAnalyzeFrame(): void {
    if (!this.videoStream || !this.isCameraOn || !this.webcamEl?.nativeElement) return;
    // Garde de concurrence : si une analyse précédente est encore en vol
    // (réseau/IA lents), on n'empile pas une nouvelle requête par-dessus —
    // c'est ce genre d'empilement qui peut finir par saturer le thread
    // principal / la connexion réseau et geler l'onglet.
    if (this.isCapturingFrame) return;

    const video = this.webcamEl.nativeElement;
    if (video.readyState < 2) return;

    const canvas    = document.createElement('canvas');
    canvas.width    = 320;
    canvas.height   = 240;
    const ctx       = canvas.getContext('2d');
    if (!ctx) return;
    ctx.drawImage(video, 0, 0, 320, 240);

    canvas.toBlob(blob => {
      if (!blob) return;
      const fd = new FormData();
      fd.append('frame', blob, 'frame.jpg');
      this.isCapturingFrame = true;

      // Via Spring Boot proxy — timeout côté client en plus du timeout
      // serveur, pour libérer la garde de concurrence même si la réponse
      // ne revient jamais.
      this.http.post<any>(`${this.javaUrl}/interview/analyze-frame`, fd).pipe(
        timeout(15_000),
        catchError(() => throwError(() => new Error('frame-analysis-failed')))
      ).subscribe({
        next: (result) => {
          this.isCapturingFrame = false;
          if (result.eye_contact !== undefined) this.contactVisuel = result.eye_contact;
          if (result.posture     !== undefined) this.posture       = result.posture;
          if (result.engagement  !== undefined) this.engagement    = result.engagement;
          if (result.tips?.length)              this.aiTips        = result.tips;
          setTimeout(() => this.refreshIcons(), 30);
        },
        error: () => { this.isCapturingFrame = false; } // silencieux — simulation continue
      });
    }, 'image/jpeg', 0.7);
  }

  // ─────────────────────────────────────────────────
  // SPEECH RECOGNITION
  // ─────────────────────────────────────────────────

  initSpeechRecognition(): void {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) { showToast('Reconnaissance vocale non supportée', 'warning'); return; }

    this.recognition               = new SR();
    this.recognition.continuous    = true;
    this.recognition.interimResults = true;
    this.recognition.lang          = 'fr-FR';

    this.recognition.onresult = (event: any) => {
      let interim = '', final = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const text = event.results[i][0].transcript;
        event.results[i].isFinal ? (final += text + ' ') : (interim += text);
      }
      this.transcript        += final;
      this.interimTranscript  = interim;
      this.scrollLiveTranscript();
    };
    this.recognition.onerror = (e: any) => {
      if (e.error !== 'aborted') showToast('Erreur micro : ' + e.error, 'warning');
    };
  }

  private scrollLiveTranscript(): void {
    setTimeout(() => {
      const el = this.liveTranscriptEl?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    }, 0);
  }

  // ─────────────────────────────────────────────────
  // VOIX DE L'IA (Text-to-Speech, gratuit — API navigateur)
  // ─────────────────────────────────────────────────

  /** Lit la question courante à voix haute (voix française si disponible). */
  private speakCurrentQuestion(): void {
    if (!this.isVoiceEnabled || !this.speechSynth || !this.currentQuestion) return;

    this.speechSynth.cancel(); // stoppe toute lecture en cours avant d'enchaîner

    const utterance = new SpeechSynthesisUtterance(this.currentQuestion.question);
    utterance.lang  = 'fr-FR';
    utterance.rate  = 1;
    utterance.pitch = 1;

    const frenchVoice = this.speechSynth.getVoices()
      .find(v => v.lang?.toLowerCase().startsWith('fr'));
    if (frenchVoice) utterance.voice = frenchVoice;

    this.speechSynth.speak(utterance);
  }

  /** Active/désactive la lecture à voix haute des questions. */
  toggleVoice(): void {
    this.isVoiceEnabled = !this.isVoiceEnabled;
    if (!this.isVoiceEnabled) {
      this.speechSynth?.cancel();
    } else {
      this.speakCurrentQuestion();
    }
    setTimeout(() => this.refreshIcons(), 30);
  }

  // ─────────────────────────────────────────────────
  // BEHAVIOR SIMULATION (entre les analyses vision)
  // ─────────────────────────────────────────────────

  startBehaviorSimulation(): void {
    let tick = 0;
    this.behaviorInterval = setInterval(() => {
      const j = (range: number) => Math.random() * range - range / 2;
      this.contactVisuel = this.clamp(this.contactVisuel + j(6), 55, 100);
      this.posture       = this.clamp(this.posture       + j(6), 55, 100);
      this.engagement    = this.clamp(this.engagement    + j(4), 65, 100);
      this.clarteVocale  = this.clamp(this.clarteVocale  + j(6), 55, 100);
      this.confiance     = this.clamp(this.confiance     + j(6), 55, 100);
      this.stress        = this.clamp(this.stress        + j(8), 10, 75);

      // Vraie analyse vision toutes les 3 itérations (~9s)
      if (++tick % 3 === 0) this.captureAndAnalyzeFrame();
    }, 3000);
  }

  // ─────────────────────────────────────────────────
  // AUDIO VISUALIZER
  // ─────────────────────────────────────────────────

  private startAudioVisualizer(): void {
    this.audioBarInterval = setInterval(() => {
      this.audioBars = Array(24).fill(0).map(() => Math.floor(Math.random() * 30) + 4);
    }, 120);
  }

  private stopAudioVisualizer(): void {
    clearInterval(this.audioBarInterval);
    this.audioBars = Array(24).fill(4);
  }

  // ─────────────────────────────────────────────────
  // RECORDING TOGGLE
  // ─────────────────────────────────────────────────

  toggleRecording(): void {
    if (!this.isRecording) {
      this.speechSynth?.cancel(); // on n'entend pas l'IA pendant qu'on parle
      this.transcript        = '';
      this.interimTranscript = '';
      this.recognition?.start();
      this.isRecording = true;
      this.startAudioVisualizer();
      showToast('Enregistrement démarré — parlez clairement', 'info');
    } else {
      this.recognition?.stop();
      this.isRecording = false;
      this.stopAudioVisualizer();

      if (this.interimTranscript.trim()) {
        this.transcript       += this.interimTranscript;
        this.interimTranscript = '';
      }

      if (this.transcript.trim()) {
        this.analyzeAndNext();
      } else {
        showToast('Aucune réponse détectée — veuillez réessayer', 'warning');
      }
    }
    setTimeout(() => this.refreshIcons(), 50);
  }

  // ─────────────────────────────────────────────────
  // ANALYSE VOCALE + DIFFICULTÉ PROGRESSIVE
  // ─────────────────────────────────────────────────

  analyzeAndNext(): void {
    this.isAnalyzing = true;
    const q = this.currentQuestion!;
    this.askedIds.push(q.id);

    // Tout passe par Spring Boot (qui gère lui-même son propre fallback
    // si Python est indisponible) — jamais d'appel direct à Python ici.
    this.http.post<any>(`${this.javaUrl}/interview/analyze-voice`, {
      transcript: this.transcript,
      question:   q.question,
      specialty:  this.specialty.title
    }).pipe(
      timeout(20_000),
      catchError(() => throwError(() => new Error('analyze-voice-failed')))
    ).subscribe({
      next:  (result) => this.onVoiceAnalyzed(result, q),
      error: () => {
        this.isAnalyzing = false;
        showToast('Erreur analyse vocale', 'warning');
        this.goToNext();
      }
    });
  }

  private onVoiceAnalyzed(result: any, q: Question): void {
    this.voiceMetrics = result;

    // Mettre à jour métriques vocales
    if (result.score_communication) this.clarteVocale = result.score_communication;
    if (result.score_confiance)     this.confiance    = result.score_confiance;

    // Afficher conseil immédiat de l'analyse vocale
    if (result.analyse_vocale?.conseil_immediat) {
      const tips = [result.analyse_vocale.conseil_immediat, ...this.aiTips.slice(0, 2)];
      this.aiTips = tips;
    }

    this.answers.push({
      question:   q,
      transcript: this.transcript,
      evaluation: result
    });

    // Mettre à jour le score courant (pour difficulté progressive)
    const scores = this.answers
      .filter(a => a.evaluation?.score_global)
      .map(a => a.evaluation.score_global);
    if (scores.length) {
      this.currentRunningScore = scores.reduce((s, v) => s + v, 0) / scores.length;
    }

    this.transcript  = '';
    this.isAnalyzing = false;
    this.updateDynamicTips(result);
    this.goToNext();
    setTimeout(() => this.refreshIcons(), 50);
  }

  private goToNext(): void {
    if (this.currentIndex < this.questions.length - 1) {
      // Difficulté progressive via Spring Boot
      this.http.post<any>(`${this.javaUrl}/interview/next-question`, {
        specialty_id:  this.specialty.id,
        current_score: this.currentRunningScore,
        asked_ids:     this.askedIds,
        all_questions: this.questions
      }).pipe(
        timeout(15_000),
        catchError(() => throwError(() => new Error('next-question-failed')))
      ).subscribe({
        next: (res) => {
          if (!res.finished && res.question) {
            // Trouver l'index de la question recommandée
            const idx = this.questions.findIndex(q => q.id === res.question.id);
            this.currentIndex = idx >= 0 ? idx : this.currentIndex + 1;
          } else {
            this.currentIndex++;
          }
          setTimeout(() => this.refreshIcons(), 50);
          this.speakCurrentQuestion();
        },
        error: () => {
          this.currentIndex++;
          setTimeout(() => this.refreshIcons(), 50);
          this.speakCurrentQuestion();
        }
      });
    } else {
      this.finishInterview();
    }
  }

  private updateDynamicTips(evaluation: any): void {
    const tips: string[] = [];
    if (evaluation.analyse_vocale?.conseil_immediat) {
      tips.push(evaluation.analyse_vocale.conseil_immediat);
    }
    if (evaluation.score_technique    < 60) tips.push('Approfondissez les aspects techniques');
    if (evaluation.score_communication < 65) tips.push('Structurez mieux votre réponse (intro → développement → conclusion)');
    if (evaluation.score_confiance     < 65) tips.push('Parlez avec plus d\'assurance, évitez les hésitations');
    if (tips.length === 0)                   tips.push('Excellente réponse — continuez sur cette lancée !');
    tips.push('Utilisez des exemples concrets issus de vos projets');
    this.aiTips = tips.slice(0, 4);
    setTimeout(() => this.refreshIcons(), 30);
  }

  skipQuestion(): void {
    if (this.currentQuestion) {
      this.askedIds.push(this.currentQuestion.id);
      this.answers.push({
        question:   this.currentQuestion,
        transcript: '(passé)',
        evaluation: {
          score_technique: 0, score_communication: 0,
          score_confiance: 0, score_global: 0,
          niveau: 'Insuffisant', points_forts: '',
          points_ameliorer: 'Question passée', reponse_ideale: ''
        }
      });
    }
    this.transcript = '';
    if (this.currentIndex < this.questions.length - 1) {
      this.currentIndex++;
      setTimeout(() => this.refreshIcons(), 50);
      this.speakCurrentQuestion();
    } else {
      this.finishInterview();
    }
  }

  // ─────────────────────────────────────────────────
  // FINISH + SAVE (via Spring Boot)
  // ─────────────────────────────────────────────────

  async finishInterview(): Promise<void> {
    clearInterval(this.timerInterval);
    clearInterval(this.behaviorInterval);
    clearInterval(this.audioBarInterval);
    clearTimeout(this.loadingWatchdog);
    this.videoStream?.getTracks().forEach(t => t.stop());
    this.recognition?.stop();
    this.speechSynth?.cancel();

    const userId = this.authService.getCurrentUserId();
    const cvId   = this.latestCv?.id;

    if (!userId || !cvId) {
      this.navigateToFeedback();
      return;
    }

    try {
      showToast('Sauvegarde de l\'entretien...', 'info');

      // 1. Créer l'entretien
      const interview = await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/interviews/add`, {
          userId, cvId, startDate: new Date().toISOString()
        })
      );
      this.savedInterviewId = interview.id;

      // 2. Sauvegarder questions + réponses
      for (const ans of this.answers) {
        const question = await firstValueFrom(
          this.http.post<any>(`${this.javaUrl}/questions/add`, {
            content:     ans.question.question,
            difficulty:  (ans.question.difficulty || 'medium').toUpperCase(),
            interviewId: interview.id
          })
        );
        await firstValueFrom(
          this.http.post<any>(`${this.javaUrl}/answers/add`, {
            questionId: question.id,
            answerText: ans.transcript || '(passé)'
          })
        );
      }

      // 3. Behavior analysis (computer vision)
      await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/behavior-analysis/add`, {
          interviewId:    interview.id,
          postureScore:   Math.round(this.posture),
          eyeContactScore: Math.round(this.contactVisuel),
          expressionScore: Math.round(this.engagement),
          videoPath: ''
        })
      );

      // 4. Voice analysis
      await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/voice-analysis/add`, {
          interviewId:          interview.id,
          clarityScore:         Math.round(this.clarteVocale),
          paceScore:            80,
          tonalVariationScore:  Math.round(this.confiance),
          audioPath: ''
        })
      );

      // 5. Feedback scores
      const evaluated = this.answers.filter(a => a.evaluation?.score_global > 0);
      const avg = (key: string) => evaluated.length
        ? Math.round(evaluated.reduce((s: number, a: any) => s + (a.evaluation?.[key] || 0), 0) / evaluated.length)
        : 70;

      await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/feedbacks/add`, {
          interviewId:        interview.id,
          technicalScore:     avg('score_technique'),
          communicationScore: avg('score_communication'),
          confidenceScore:    avg('score_confiance'),
          eyeContactScore:    Math.round(this.contactVisuel)
        })
      );

      showToast('Entretien sauvegardé avec succès !', 'success');
    } catch (err) {
      console.error('Save error:', err);
      showToast('Erreur lors de la sauvegarde', 'danger');
    }

    this.navigateToFeedback();
  }

  private navigateToFeedback(): void {
    const evaluated = this.answers.filter(a => a.evaluation?.score_global > 0);
    const avg = (key: string) => evaluated.length
      ? Math.round(evaluated.reduce((s: number, a: any) => s + (a.evaluation?.[key] || 0), 0) / evaluated.length)
      : 0;

    sessionStorage.setItem('interview_results', JSON.stringify({
      interviewId:    this.savedInterviewId,
      specialty:      this.specialty,
      answers:        this.answers,
      duration:       this.timer,
      questions_total: this.questions.length,
      avg_scores: {
        technique:     avg('score_technique'),
        communication: avg('score_communication'),
        confiance:     avg('score_confiance'),
        global:        avg('score_global')
      },
      behavior: {
        contactVisuel: Math.round(this.contactVisuel),
        posture:       Math.round(this.posture),
        engagement:    Math.round(this.engagement),
        clarteVocale:  Math.round(this.clarteVocale),
        confiance:     Math.round(this.confiance),
        stress:        Math.round(this.stress)
      }
    }));

    this.router.navigate(['/frontoffice/interview-feedback']);
  }

  quit(): void {
    clearInterval(this.timerInterval);
    clearInterval(this.behaviorInterval);
    clearInterval(this.audioBarInterval);
    clearTimeout(this.loadingWatchdog);
    this.videoStream?.getTracks().forEach(t => t.stop());
    this.recognition?.stop();
    this.speechSynth?.cancel();
    this.router.navigate(['/frontoffice/interviewPrep']);
  }

  ngOnDestroy(): void {
    clearInterval(this.timerInterval);
    clearInterval(this.behaviorInterval);
    clearInterval(this.audioBarInterval);
    clearTimeout(this.loadingWatchdog);
    this.videoStream?.getTracks().forEach(t => t.stop());
    this.recognition?.stop();
    this.speechSynth?.cancel();
  }
}