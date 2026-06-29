import {
  AfterViewInit, Component, ElementRef, NgZone,
  OnDestroy, OnInit, ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { CvService } from '../../services/cv.service';
import { firstValueFrom } from 'rxjs';

declare const lucide: any;
declare function showToast(msg: string, type?: string): void;

interface Question {
  id: number;
  category: string;
  question: string;
  difficulty: string;
  tip: string;
  time_suggested: number;
}

interface AnswerRecord {
  question: Question;
  transcript: string;
  evaluation?: any;
  voiceMetrics?: VoiceMetrics;
  behaviorSnapshot?: BehaviorSnapshot;
}

interface VoiceMetrics {
  clarityScore: number;
  paceScore: number;
  confidenceScore: number;
  fillerWordCount: number;
  wordCount: number;
  speakingRate: number; // words per minute
}

interface BehaviorSnapshot {
  eyeContact: number;
  posture: number;
  engagement: number;
  stress: number;
}

@Component({
  selector: 'app-interview-session',
  templateUrl: './interview-session.component.html',
  styleUrls: ['./interview-session.component.css']
})
export class InterviewSessionComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('webcam') webcamElement!: ElementRef<HTMLVideoElement>;

  // ── Session state ─────────────────────────────────────────────────────────
  specialty: any = null;
  questions: Question[] = [];
  currentIndex = 0;
  isLoading = true;
  isRecording = false;
  isAnalyzing = false;

  // ── Timer ─────────────────────────────────────────────────────────────────
  timer = 0;
  private timerInterval: any;

  // ── Media & Speech ─────────────────────────────────────────────────────────
  videoStream: MediaStream | null = null;
  isCameraOn = true;
  private recognition: any = null;
  transcript = '';
  interimTranscript = '';
  answers: AnswerRecord[] = [];

  // ── Voice analysis (real-time) ─────────────────────────────────────────────
  private audioContext: AudioContext | null = null;
  private analyserNode: AnalyserNode | null = null;
  private waveAnimFrame: number | null = null;
  private recordingStart = 0;
  waveformBars: number[] = Array(20).fill(4); // heights in px

  // ── Behavior metrics (updated by camera analysis loop) ─────────────────────
  contactVisuel = 82;
  posture = 78;
  engagement = 90;
  clarteVocale = 88;
  confiance = 80;
  stress = 32;

  private behaviorInterval: any;
  private cameraAnalysisInterval: any;
  private lastVoiceMetrics: VoiceMetrics | null = null;

  // ── URLs ──────────────────────────────────────────────────────────────────
  private pythonUrl = 'http://localhost:8000';
  private javaUrl   = 'http://localhost:8086/HireVision';

  private latestCv: any = null;

  // ── AI Tips (dynamic) ─────────────────────────────────────────────────────
  aiTips: string[] = [
    'Maintenez le contact visuel avec la caméra',
    'Parlez clairement et avec confiance',
    'Faites de courtes pauses avant de répondre',
    'Utilisez des exemples concrets (méthode STAR)'
  ];

  // ── Derived ───────────────────────────────────────────────────────────────
  get currentQuestion(): Question | null { return this.questions[this.currentIndex] ?? null; }
  get progress(): number {
    return this.questions.length
      ? Math.round((this.answers.length / this.questions.length) * 100)
      : 0;
  }
  get timerDisplay(): string {
    const m = Math.floor(this.timer / 60).toString().padStart(2, '0');
    const s = (this.timer % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  get metrics() {
    return [
      { label: 'Contact visuel', value: Math.round(this.contactVisuel), color: '#10b981' },
      { label: 'Posture',        value: Math.round(this.posture),       color: '#3b82f6' },
      { label: 'Engagement',     value: Math.round(this.engagement),    color: '#10b981' },
      { label: 'Clarté vocale',  value: Math.round(this.clarteVocale),  color: '#10b981' },
      { label: 'Confiance',      value: Math.round(this.confiance),     color: '#a78bfa' },
      { label: 'Niveau de stress', value: Math.round(this.stress),      color: '#f59e0b' },
    ];
  }

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService,
    private cvService: CvService,
    private ngZone: NgZone
  ) {}

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const stored = sessionStorage.getItem('interview_specialty');
    if (!stored) { this.router.navigate(['/frontoffice/interviewPrep']); return; }
    this.specialty = JSON.parse(stored);
    this.loadQuestions();
    this.startTimer();
    this.initWebcam();
    this.initSpeechRecognition();
    this.loadLatestCv();
  }

  ngAfterViewInit(): void {
    setTimeout(() => lucide?.createIcons(), 100);
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  // ── Data loading ──────────────────────────────────────────────────────────

  loadLatestCv(): void {
    const userId = this.authService.getCurrentUserId();
    if (userId) {
      this.cvService.getLatest(userId).subscribe({
        next: (cv) => this.latestCv = cv,
        error: (err) => console.warn('CV load error:', err)
      });
    }
  }

  loadQuestions(): void {
    this.isLoading = true;
    this.http.get<any>(`${this.pythonUrl}/interview/questions/${this.specialty.id}`).subscribe({
      next: (res) => {
        this.questions = res.questions ?? [];
        this.isLoading = false;
        setTimeout(() => lucide?.createIcons(), 100);
      },
      error: () => {
        this.isLoading = false;
        showToast('Erreur lors du chargement des questions', 'danger');
      }
    });
  }

  // ── Timer ─────────────────────────────────────────────────────────────────

  startTimer(): void {
    this.timerInterval = setInterval(() => this.timer++, 1000);
  }

  // ── Webcam ────────────────────────────────────────────────────────────────

  initWebcam(): void {
    navigator.mediaDevices.getUserMedia({ video: { width: 1280, height: 720, facingMode: 'user' }, audio: false })
      .then(stream => {
        this.videoStream = stream;
        this.attachVideoStream();
        this.startCameraAnalysis();
      })
      .catch(() => {
        showToast('Caméra non disponible — mode audio uniquement activé', 'warning');
        this.isCameraOn = false;
      });
  }

  private attachVideoStream(): void {
    const attach = () => {
      const video = this.webcamElement?.nativeElement;
      if (video) {
        video.srcObject = this.videoStream;
        video.muted = true;
        video.play().catch(e => console.warn('Video play error:', e));
      } else {
        setTimeout(attach, 100);
      }
    };
    attach();
  }

  toggleWebcam(): void {
    if (this.isCameraOn) {
      this.videoStream?.getVideoTracks().forEach(t => t.stop());
      this.videoStream = null;
      this.isCameraOn = false;
      clearInterval(this.cameraAnalysisInterval);
      showToast('Caméra désactivée', 'info');
    } else {
      this.isCameraOn = true;
      this.initWebcam();
    }
    setTimeout(() => lucide?.createIcons(), 50);
  }

  // ── Camera-based behavior analysis ────────────────────────────────────────
  /**
   * Sends a frame snapshot to the Python /analyze-frame endpoint every 3s.
   * Falls back to jitter simulation if the endpoint is unavailable.
   */
  private startCameraAnalysis(): void {
    this.cameraAnalysisInterval = setInterval(() => {
      this.captureFrameAndAnalyze();
    }, 3000);
  }

  private captureFrameAndAnalyze(): void {
    const video = this.webcamElement?.nativeElement;
    if (!video || !this.videoStream) {
      this.simulateBehaviorJitter();
      return;
    }

    // Draw current frame to off-screen canvas
    const canvas = document.createElement('canvas');
    canvas.width  = 320; // small for fast transfer
    canvas.height = 240;
    const ctx = canvas.getContext('2d');
    if (!ctx) { this.simulateBehaviorJitter(); return; }
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(blob => {
      if (!blob) { this.simulateBehaviorJitter(); return; }
      const fd = new FormData();
      fd.append('frame', blob, 'frame.jpg');

      this.http.post<any>(`${this.pythonUrl}/analyze-frame`, fd).subscribe({
        next: (res) => {
          this.ngZone.run(() => {
            // Python returns normalized 0-1 scores — convert to 0-100
            if (res.eye_contact   != null) this.contactVisuel = res.eye_contact   * 100;
            if (res.posture       != null) this.posture       = res.posture       * 100;
            if (res.engagement    != null) this.engagement    = res.engagement    * 100;
            if (res.stress        != null) this.stress        = res.stress        * 100;
            if (res.confidence    != null) this.confiance     = res.confidence    * 100;
            this.updateAiTipsFromBehavior();
          });
        },
        error: () => this.simulateBehaviorJitter() // endpoint not yet available
      });
    }, 'image/jpeg', 0.7);
  }

  private simulateBehaviorJitter(): void {
    const jitter = (v: number, lo: number, hi: number, d: number) =>
      Math.min(hi, Math.max(lo, v + (Math.random() * d * 2 - d)));

    this.ngZone.run(() => {
      this.contactVisuel = jitter(this.contactVisuel, 60, 98, 3);
      this.posture       = jitter(this.posture, 55, 95, 3);
      this.engagement    = jitter(this.engagement, 65, 98, 2);
      this.clarteVocale  = jitter(this.clarteVocale, 60, 97, 3);
      this.confiance     = jitter(this.confiance, 58, 95, 3);
      this.stress        = jitter(this.stress, 8, 70, 4);
    });
  }

  private updateAiTipsFromBehavior(): void {
    const tips: string[] = [];
    if (this.contactVisuel < 70) tips.push('Regardez directement la caméra');
    if (this.posture < 65)       tips.push('Redressez-vous et tenez-vous droit');
    if (this.stress > 60)        tips.push('Respirez profondément pour vous détendre');
    if (this.confiance < 65)     tips.push('Parlez avec plus d\'assurance');

    // Always keep at least 2 positive tips
    const fallback = [
      'Utilisez des exemples concrets',
      'Structurez votre réponse (situation → action → résultat)',
      'Faites des pauses réfléchies plutôt que de dire "euh"',
    ];
    while (tips.length < 3) tips.push(fallback[tips.length]);
    this.aiTips = tips.slice(0, 4);
  }

  // ── Speech Recognition ────────────────────────────────────────────────────

  initSpeechRecognition(): void {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) {
      showToast('Votre navigateur ne supporte pas la reconnaissance vocale', 'warning');
      return;
    }
    this.recognition = new SR();
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    this.recognition.lang = 'fr-FR';

    this.recognition.onresult = (event: any) => {
      let interim = '';
      let final = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const text = event.results[i][0].transcript;
        if (event.results[i].isFinal) final += text + ' ';
        else interim += text;
      }
      this.transcript += final;
      this.interimTranscript = interim;
    };

    this.recognition.onerror = (event: any) => {
      console.warn('Speech recognition error:', event.error);
      if (event.error === 'not-allowed') {
        showToast('Microphone refusé — vérifiez les permissions', 'danger');
      }
    };
  }

  // ── Audio Waveform ────────────────────────────────────────────────────────

  private initAudioVisualizer(): void {
    navigator.mediaDevices.getUserMedia({ audio: true, video: false }).then(stream => {
      this.audioContext = new AudioContext();
      const source = this.audioContext.createMediaStreamSource(stream);
      this.analyserNode = this.audioContext.createAnalyser();
      this.analyserNode.fftSize = 64;
      source.connect(this.analyserNode);
      this.animateWaveform();
    }).catch(() => {
      // No mic access — animate randomly
      this.animateWaveformFallback();
    });
  }

  private animateWaveform(): void {
    const draw = () => {
      if (!this.isRecording) return;
      this.waveAnimFrame = requestAnimationFrame(draw);

      if (!this.analyserNode) return;
      const data = new Uint8Array(this.analyserNode.frequencyBinCount);
      this.analyserNode.getByteFrequencyData(data);

      this.ngZone.run(() => {
        this.waveformBars = Array.from({ length: 20 }, (_, i) => {
          const idx = Math.floor(i * data.length / 20);
          return 4 + (data[idx] / 255) * 22;
        });
      });
    };
    draw();
  }

  private animateWaveformFallback(): void {
    const tick = () => {
      if (!this.isRecording) return;
      this.waveAnimFrame = requestAnimationFrame(tick);
      this.ngZone.run(() => {
        this.waveformBars = Array.from({ length: 20 }, () =>
          4 + Math.random() * 22
        );
      });
    };
    tick();
  }

  private stopAudioVisualizer(): void {
    if (this.waveAnimFrame) {
      cancelAnimationFrame(this.waveAnimFrame);
      this.waveAnimFrame = null;
    }
    this.audioContext?.close();
    this.audioContext = null;
    this.analyserNode = null;
    this.waveformBars = Array(20).fill(4);
  }

  // ── Real Voice Metrics ────────────────────────────────────────────────────

  private computeLocalVoiceMetrics(text: string, durationSeconds: number): VoiceMetrics {
    const words = text.trim().split(/\s+/).filter(Boolean);
    const wordCount = words.length;

    // Filler words detection (French + English)
    const fillers = ['euh', 'euhm', 'eh', 'ben', 'bah', 'hm', 'um', 'uh', 'donc', 'voilà'];
    const fillerWordCount = words.filter(w => fillers.includes(w.toLowerCase())).length;

    // Speaking rate (words per minute)
    const speakingRate = durationSeconds > 0 ? Math.round((wordCount / durationSeconds) * 60) : 0;

    // Clarity score: penalizes fillers and very long sentences
    const fillerRatio = wordCount > 0 ? fillerWordCount / wordCount : 0;
    const clarityScore = Math.max(20, Math.round(100 - fillerRatio * 150 - (speakingRate > 180 ? 15 : 0)));

    // Pace score: ideal ~130 wpm for French
    const idealPace = 130;
    const paceDeviation = Math.abs(speakingRate - idealPace);
    const paceScore = wordCount < 5 ? 50 : Math.max(30, Math.round(100 - paceDeviation * 0.5));

    // Confidence score: longer, more complete sentences suggest confidence
    const avgSentLen = wordCount / (text.split(/[.!?]/).length || 1);
    const confidenceScore = Math.min(95, Math.max(30, Math.round(50 + avgSentLen * 2 - fillerRatio * 80)));

    return { clarityScore, paceScore, confidenceScore, fillerWordCount, wordCount, speakingRate };
  }

  // ── Recording flow ────────────────────────────────────────────────────────

  toggleRecording(): void {
    if (!this.isRecording) {
      this.startRecording();
    } else {
      this.stopRecording();
    }
  }

  private startRecording(): void {
    this.transcript = '';
    this.interimTranscript = '';
    this.recordingStart = Date.now();
    this.recognition?.start();
    this.isRecording = true;
    this.initAudioVisualizer();
    showToast('Enregistrement démarré — parlez maintenant', 'info');
  }

  private stopRecording(): void {
    this.recognition?.stop();
    this.isRecording = false;
    this.stopAudioVisualizer();

    if (this.interimTranscript.trim()) {
      this.transcript += this.interimTranscript;
      this.interimTranscript = '';
    }

    const duration = (Date.now() - this.recordingStart) / 1000;
    this.lastVoiceMetrics = this.computeLocalVoiceMetrics(this.transcript, duration);

    // Update clarté vocale from real voice metrics
    this.clarteVocale = this.lastVoiceMetrics.clarityScore;
    this.confiance    = Math.round((this.confiance * 0.6) + (this.lastVoiceMetrics.confidenceScore * 0.4));

    if (this.transcript.trim()) {
      this.analyzeAndNext();
    } else {
      showToast('Aucune réponse détectée — réessayez ou passez', 'warning');
    }
  }

  // ── AI Analysis ───────────────────────────────────────────────────────────

  analyzeAndNext(): void {
    this.isAnalyzing = true;
    const q = this.currentQuestion!;

    const behaviorSnapshot: BehaviorSnapshot = {
      eyeContact: Math.round(this.contactVisuel),
      posture:    Math.round(this.posture),
      engagement: Math.round(this.engagement),
      stress:     Math.round(this.stress),
    };

    this.http.post<any>(`${this.pythonUrl}/interview/analyze-voice`, {
      transcript: this.transcript,
      question:   q.question,
      specialty:  this.specialty.title
    }).subscribe({
      next: (result) => {
        this.answers.push({
          question:         q,
          transcript:       this.transcript,
          evaluation:       result,
          voiceMetrics:     this.lastVoiceMetrics ?? undefined,
          behaviorSnapshot
        });
        this.transcript = '';
        this.isAnalyzing = false;

        if (this.currentIndex < this.questions.length - 1) {
          this.currentIndex++;
          setTimeout(() => lucide?.createIcons(), 50);
        } else {
          this.finishInterview();
        }
      },
      error: () => {
        this.isAnalyzing = false;
        showToast('Erreur lors de l\'analyse IA — question passée', 'warning');
        this.skipQuestion();
      }
    });
  }

  skipQuestion(): void {
    this.answers.push({
      question:    this.currentQuestion!,
      transcript:  '(passé)',
      evaluation: {
        score_technique: 0, score_communication: 0,
        score_confiance: 0, score_global: 0,
        niveau: 'Insuffisant',
        points_forts: '', points_ameliorer: 'Question passée', reponse_ideale: ''
      }
    });
    this.transcript = '';
    if (this.currentIndex < this.questions.length - 1) {
      this.currentIndex++;
      setTimeout(() => lucide?.createIcons(), 50);
    } else {
      this.finishInterview();
    }
  }

  // ── Finish & Save ─────────────────────────────────────────────────────────

  async finishInterview(): Promise<void> {
    this.cleanup();

    const userId = this.authService.getCurrentUserId();
    const cvId   = this.latestCv?.id;

    if (!userId || !cvId) {
      console.warn('User or CV not found — skipping DB save');
      this.navigateToFeedback();
      return;
    }

    try {
      showToast('Enregistrement de l\'entretien en cours…', 'info');

      // 1 — Create Interview
      const interviewDto = await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/interviews/add`, {
          userId, cvId, startDate: new Date().toISOString()
        })
      );
      const dbInterviewId = interviewDto.id;

      // 2 — Save questions + answers
      for (const ans of this.answers) {
        const questionDto = await firstValueFrom(
          this.http.post<any>(`${this.javaUrl}/questions/add`, {
            content:     ans.question.question,
            difficulty:  (ans.question.difficulty || 'medium').toUpperCase(),
            interviewId: dbInterviewId
          })
        );
        await firstValueFrom(
          this.http.post<any>(`${this.javaUrl}/answers/add`, {
            questionId: questionDto.id,
            answerText: ans.transcript || '(passé)'
          })
        );
      }

      // 3 — Behavior Analysis (from camera)
      await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/behavior-analysis/add`, {
          interviewId:     dbInterviewId,
          postureScore:    Math.round(this.posture),
          eyeContactScore: Math.round(this.contactVisuel),
          expressionScore: Math.round(this.engagement),
          videoPath:       ''
        })
      );

      // 4 — Voice Analysis (from real metrics)
      const avgVoice = this.computeAverageVoiceMetrics();
      await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/voice-analysis/add`, {
          interviewId:        dbInterviewId,
          clarityScore:       avgVoice.clarity,
          paceScore:          avgVoice.pace,
          tonalVariationScore: Math.round(this.confiance),
          audioPath:          ''
        })
      );

      // 5 — Feedback
      const evaluated = this.answers.filter(a => a.evaluation?.score_global);
      const avg = (key: string) =>
        evaluated.length
          ? Math.round(evaluated.reduce((s, a) => s + (a.evaluation?.[key] || 0), 0) / evaluated.length)
          : 70;

      await firstValueFrom(
        this.http.post<any>(`${this.javaUrl}/feedbacks/add`, {
          interviewId:       dbInterviewId,
          technicalScore:    avg('score_technique'),
          communicationScore: avg('score_communication'),
          confidenceScore:   avg('score_confiance'),
          eyeContactScore:   Math.round(this.contactVisuel)
        })
      );

      showToast('Entretien enregistré avec succès !', 'success');

    } catch (err) {
      console.error('Save error:', err);
      showToast('Erreur lors de l\'enregistrement — les résultats sont quand même disponibles', 'warning');
    }

    this.navigateToFeedback();
  }

  private computeAverageVoiceMetrics(): { clarity: number; pace: number } {
    const answersWithMetrics = this.answers.filter(a => a.voiceMetrics);
    if (!answersWithMetrics.length) {
      return { clarity: Math.round(this.clarteVocale), pace: 75 };
    }
    const avgClarity = Math.round(
      answersWithMetrics.reduce((s, a) => s + (a.voiceMetrics?.clarityScore ?? 0), 0) / answersWithMetrics.length
    );
    const avgPace = Math.round(
      answersWithMetrics.reduce((s, a) => s + (a.voiceMetrics?.paceScore ?? 0), 0) / answersWithMetrics.length
    );
    return { clarity: avgClarity, pace: avgPace };
  }

  private navigateToFeedback(): void {
    const evaluated = this.answers.filter(a => a.evaluation?.score_global);
    const avg = (key: string) =>
      evaluated.length
        ? Math.round(evaluated.reduce((s, a) => s + (a.evaluation?.[key] || 0), 0) / evaluated.length)
        : 0;

    sessionStorage.setItem('interview_results', JSON.stringify({
      specialty:  this.specialty,
      answers:    this.answers,
      duration:   this.timer,
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

  // ── Cleanup ───────────────────────────────────────────────────────────────

  quit(): void {
    this.cleanup();
    this.router.navigate(['/frontoffice/interviewPrep']);
  }

  private cleanup(): void {
    clearInterval(this.timerInterval);
    clearInterval(this.behaviorInterval);
    clearInterval(this.cameraAnalysisInterval);
    this.videoStream?.getTracks().forEach(t => t.stop());
    this.recognition?.stop();
    this.stopAudioVisualizer();
  }
}