import os
import tempfile
import time
import random
import json
import httpx
import pdfplumber
import cv2
import numpy as np
from threading import Lock
from fastapi import FastAPI, UploadFile, File, Query, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from dotenv import load_dotenv
load_dotenv()

from extractor import (
    extract_skills, extract_education, extract_experience,
    extract_languages, extract_certifications, extract_name, extract_projects
)
from ml_model import predict_profile
from job_matcher import match_skills
from interview_analyzer import (
    get_specialties,
    generate_questions_by_specialty,
    get_next_question_adaptive,
    analyze_voice_response,
    generate_final_feedback,
    compute_final_score,
    _normalize_specialty_id,
    SPECIALTIES
)

app = FastAPI(
    title="HireVision AI Microservice",
    description="Service IA de HireVision : analyse CV, matching emploi, questions d'entretien, feedback vocal & comportemental.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",   # frontend Angular en développement
        "http://frontend:80",      # frontend Docker
        # "https://ton-domaine-en-production.com",  # à décommenter/ajouter quand tu déploies
    ],
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

from llm_client import call_llm, GROQ_API_KEY
GEMINI_API_KEY = GROQ_API_KEY  # alias gardé pour ne pas casser le reste du fichier


# ─────────────────────────────────────────────────────────────────────────────
# Health Check Endpoint (utilisé par Docker et monitoring)
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health", tags=["System"])
def health_check():
    """Vérifie que le service AI est opérationnel."""
    return {
        "status": "ok",
        "service": "HireVision AI Microservice",
        "groq_configured": bool(GROQ_API_KEY),
        "version": "1.0.0"
    }

# ─────────────────────────────────────────────────────────────────────────────
# Cache en mémoire — évite les appels Gemini répétés
# Clé : specialty_id  |  Valeur : { pool: [...], expires: float }
# ─────────────────────────────────────────────────────────────────────────────
_question_cache: dict = {}
_cache_lock = Lock()
CACHE_TTL = 1800  # 30 minutes

# ─────────────────────────────────────────────────────────────────────────────
# OpenCV Haar Cascades — computer vision pour analyse comportementale
# ─────────────────────────────────────────────────────────────────────────────
FACE_CASCADE = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
EYE_CASCADE  = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")


# ─────────────────────────────────────────────────────────────────────────────
# Pydantic models
# ─────────────────────────────────────────────────────────────────────────────

class JobMatchRequest(BaseModel):
    cv_skills:  list[str]
    job_skills: list[str]

class QuestionRequest(BaseModel):
    skills:          list[str]
    job_description: str
    candidate_name:  str = ""
    profile:         str = ""

class EvaluateRequest(BaseModel):
    question: str
    answer:   str
    category: str
    skills:   list[str]

class VoiceAnalyzeRequest(BaseModel):
    transcript: str
    question:   str
    specialty:  str

class FeedbackRequest(BaseModel):
    specialty:  str
    answers:    list
    avg_scores: dict

class FinalScoreRequest(BaseModel):
    score_technique:     float
    score_communication: float
    score_confiance:     float
    hesitation_ratio:    float = 0.0
    eye_contact:         float = 75
    posture:             float = 75
    engagement:          float = 75
    score_stress_vocal:  float | None = None


class NextQuestionRequest(BaseModel):
    specialty_id:  str
    current_score: float
    asked_ids:     list
    all_questions: list


# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────

def extract_text_from_pdf(file_bytes: bytes) -> str:
    with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as tmp:
        tmp.write(file_bytes)
        tmp_path = tmp.name
    text = ""
    try:
        with pdfplumber.open(tmp_path) as pdf:
            for page in pdf.pages:
                text += (page.extract_text() or "") + "\n"
    finally:
        os.unlink(tmp_path)
    return text.strip()


def generate_feedback_with_gemini(skills: list[str], profile: str, score: int) -> str:
    if not GEMINI_API_KEY:
        return f"Profil {profile} détecté avec un score de {score}/100."
    prompt = f"""Tu es un expert en recrutement tech. Analyse ce profil CV :
- Profil détecté : {profile}
- Score global : {score}/100
- Compétences : {', '.join(skills)}
Génère un résumé professionnel en 3 phrases max en français, sans bullet points."""
    try:
        return call_llm(prompt, timeout=30)
    except Exception:
        return f"Profil {profile} avec score {score}/100."


def generate_cv_analysis_insights(raw_text: str, skills: list[str], profile: str, score: int) -> dict:
    if not GROQ_API_KEY:
        return {
            "summary": f"Profil {profile} détecté avec un score global de {score}/100.",
            "proposed_summary": f"Développeur {profile} spécialisé en {', '.join(skills[:4])}.",
            "optimization_suggestions": [
                "Ajouter des liens vers vos projets récents (GitHub, Portfolio).",
                "Détailler les technologies utilisées sous chaque expérience.",
                "Ajouter des certifications pertinentes dans votre domaine."
            ],
            "strengths": [f"Bonne maîtrise de {s}" for s in skills[:3]] if skills else ["Expérience pertinente"],
            "weaknesses": ["Manque de certifications visibles"] if len(skills) < 5 else ["Compétences secondaires à renforcer"],
            "recommendations": [
                "Suivre des cours en ligne pour les outils DevOps et Cloud.",
                "Créer des projets personnels démontrant l'usage de vos compétences."
            ]
        }

    prompt = f"""Tu es un expert en recrutement technique et optimisation de CV de développeurs.
Analyse le texte brut du CV suivant ainsi que les compétences extraites et le score calculé.

Profil détecté : {profile}
Score global calculé : {score}/100
Compétences détectées : {', '.join(skills)}

Texte brut du CV :
{raw_text[:3000]}

Génère une analyse structurée en français pour aider le candidat à s'améliorer.
Réponds UNIQUEMENT sous la forme d'un objet JSON valide contenant exactement ces clés :
- "summary": (string, 2-3 phrases résumant le profil actuel)
- "proposed_summary": (string, proposition d'un résumé professionnel accrocheur et optimisé pour le CV du candidat)
- "optimization_suggestions": (liste de strings, 3 à 5 conseils concrets et actionnables pour améliorer la rédaction, le format et le contenu de son CV)
- "strengths": (liste de strings, 2 à 4 points forts du candidat)
- "weaknesses": (liste de strings, 2 à 4 points faibles ou axes d'amélioration)
- "recommendations": (liste de strings, 2 à 4 recommandations pratiques de formation ou de projets)
"""
    try:
        raw = call_llm(prompt, timeout=30).strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"):
                raw = raw[4:]
        data = json.loads(raw.strip())
        return data
    except Exception as e:
        print(f"Error in generate_cv_analysis_insights: {e}")
        return {
            "summary": f"Profil {profile} détecté avec un score global de {score}/100.",
            "proposed_summary": f"Développeur {profile} spécialisé en {', '.join(skills[:4])}.",
            "optimization_suggestions": [
                "Ajouter des liens vers vos projets récents (GitHub, Portfolio).",
                "Détailler les technologies utilisées sous chaque expérience.",
                "Ajouter des certifications pertinentes dans votre domaine."
            ],
            "strengths": [f"Bonne maîtrise de {s}" for s in skills[:3]] if skills else ["Expérience pertinente"],
            "weaknesses": ["Manque de certifications visibles"] if len(skills) < 5 else ["Compétences secondaires à renforcer"],
            "recommendations": [
                "Suivre des cours en ligne pour les outils DevOps et Cloud.",
                "Créer des projets personnels démontrant l'usage de vos compétences."
            ]
        }


def evaluate_answer_with_gemini(question: str, answer: str, category: str, skills: list[str]) -> dict:
    if not GEMINI_API_KEY:
        return {
            "score": 70, "niveau": "Bien",
            "points_forts": "Bonne réponse",
            "points_ameliorer": "Plus de détails",
            "reponse_ideale": "Réponse structurée avec exemples"
        }
    prompt = f"""Évalue cette réponse d'entretien :
Question : {question}
Catégorie : {category}
Réponse : {answer}
Compétences : {', '.join(skills)}
Réponds UNIQUEMENT en JSON :
{{"score":<0-100>,"niveau":"<Excellent|Bien|Moyen|Insuffisant>","points_forts":"...","points_ameliorer":"...","reponse_ideale":"..."}}"""
    try:
        raw = call_llm(prompt, timeout=30).strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"): raw = raw[4:]
        return json.loads(raw.strip())
    except Exception:
        return {
            "score": 70, "niveau": "Bien",
            "points_forts": "Réponse reçue",
            "points_ameliorer": "Développez davantage",
            "reponse_ideale": "Réponse avec exemples concrets"
        }


def analyze_webcam_frame(frame_bytes: bytes) -> dict:
    """Computer vision : analyse une frame JPEG via OpenCV Haar Cascades."""
    try:
        nparr = np.frombuffer(frame_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            return _default_vision_metrics()

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        h, w = frame.shape[:2]

        faces = FACE_CASCADE.detectMultiScale(
            gray, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60)
        )

        if len(faces) == 0:
            return {
                "eye_contact": 40, "posture": 50, "engagement": 45,
                "face_detected": False,
                "tips": [
                    "Rapprochez-vous de la caméra",
                    "Assurez-vous d'être bien éclairé",
                    "Centrez votre visage dans le cadre"
                ]
            }

        fx, fy, fw, fh = max(faces, key=lambda f: f[2] * f[3])
        face_cx = fx + fw / 2
        face_cy = fy + fh / 2

        center_offset_x = abs(face_cx - w / 2) / (w / 2)
        center_offset_y = abs(face_cy - h / 2) / (h / 2)

        face_roi_gray = gray[fy:fy+fh, fx:fx+fw]
        eyes          = EYE_CASCADE.detectMultiScale(face_roi_gray, scaleFactor=1.1, minNeighbors=5)
        eyes_detected = len(eyes) >= 2

        eye_contact = int(
            100
            - (center_offset_x * 25)
            - (center_offset_y * 15)
            + (10 if eyes_detected else -15)
        )
        eye_contact = max(30, min(100, eye_contact))

        face_area_ratio = (fw * fh) / (w * h)
        if 0.06 <= face_area_ratio <= 0.30:
            posture_size = 90
        elif face_area_ratio < 0.04:
            posture_size = 55
        elif face_area_ratio > 0.45:
            posture_size = 60
        else:
            posture_size = 75

        vert_score = 90 if face_cy < h * 0.55 else 70
        posture    = max(30, min(100, int((posture_size + vert_score) / 2)))
        engagement = max(40, min(100, int(eye_contact * 0.6 + posture * 0.4)))

        tips = []
        if eye_contact < 65:
            tips.append("Regardez directement la caméra pour un meilleur contact visuel")
        if posture < 65:
            tips.append("Ajustez votre position — centrez votre visage dans le cadre")
        if not eyes_detected:
            tips.append("Améliorez l'éclairage pour que vos yeux soient bien visibles")
        if not tips:
            tips.append("Excellent contact visuel — continuez ainsi !")

        return {
            "eye_contact":    eye_contact,
            "posture":        posture,
            "engagement":     engagement,
            "face_detected":  True,
            "eyes_detected":  eyes_detected,
            "tips":           tips
        }
    except Exception:
        return _default_vision_metrics()


def _default_vision_metrics() -> dict:
    return {
        "eye_contact": 75, "posture": 70, "engagement": 78,
        "face_detected": False,
        "tips": ["Maintenez le contact visuel avec la caméra"]
    }


def _get_cached_pool(specialty_id: str) -> list | None:
    """Retourne le pool en cache si encore valide."""
    with _cache_lock:
        cached = _question_cache.get(specialty_id)
        if cached and cached["expires"] > time.time():
            return cached["pool"]
    return None


def _set_cache(specialty_id: str, questions: list) -> None:
    """Stocke un pool étendu en cache (pool × 2 pour la variété)."""
    with _cache_lock:
        pool = questions * 2  # doubler pour avoir plus de variété
        random.shuffle(pool)
        _question_cache[specialty_id] = {
            "pool":    pool,
            "expires": time.time() + CACHE_TTL
        }


# ─────────────────────────────────────────────────────────────────────────────
# CV Endpoints
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/analyze")
async def analyze_cv(file: UploadFile = File(...)):
    file_bytes     = await file.read()
    raw_text       = extract_text_from_pdf(file_bytes)
    skills         = extract_skills(raw_text)
    education      = extract_education(raw_text)
    experience     = extract_experience(raw_text)
    projects       = extract_projects(raw_text)
    languages      = extract_languages(raw_text)
    certifications = extract_certifications(raw_text)
    ml_result      = predict_profile(skills)
    
    insights = generate_cv_analysis_insights(
        raw_text, skills, ml_result.get("profile", "Non déterminé"), ml_result.get("global_score", 0)
    )
    
    return {
        "skills":         skills,
        "education":      education,
        "experience":     experience,
        "projects":       projects,
        "certifications": certifications,
        "languages":      languages,
        "summary":        insights.get("summary"),
        "proposed_summary": insights.get("proposed_summary"),
        "optimization_suggestions": insights.get("optimization_suggestions"),
        "strengths":      insights.get("strengths"),
        "weaknesses":     insights.get("weaknesses"),
        "recommendations": insights.get("recommendations"),
        "profile":        ml_result.get("profile"),
        "confidence":     ml_result.get("confidence"),
        "global_score":   ml_result.get("global_score"),
        "skill_scores":   ml_result.get("skill_scores", {})
    }


@app.get("/analyze-github")
async def analyze_github(username: str = Query(..., description="Le nom d'utilisateur GitHub")):
    from github_analyzer import analyze_github_profile
    return await analyze_github_profile(username)


# ─────────────────────────────────────────────────────────────────────────────
# Job Matching
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/match-job")
def match_job(request: JobMatchRequest):
    return match_skills(request.cv_skills, request.job_skills)


# ─────────────────────────────────────────────────────────────────────────────
# Question / Evaluate (anciens endpoints compatibles)
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/generate-questions")
def generate_questions(request: QuestionRequest):
    from question_generator import generate_interview_questions
    questions = generate_interview_questions(
        skills=request.skills,
        job_description=request.job_description,
        candidate_name=request.candidate_name,
        profile=request.profile
    )
    return {"total": len(questions), "questions": questions}


@app.post("/evaluate-answer")
def evaluate_answer(request: EvaluateRequest):
    return evaluate_answer_with_gemini(
        request.question, request.answer, request.category, request.skills
    )


# ─────────────────────────────────────────────────────────────────────────────
# Interview Session Endpoints
# Angular → Spring Boot (:8086) → ici (:8000)
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/interview/specialties")
def interview_specialties():
    return get_specialties()


@app.get("/interview/questions/{specialty_id}")
def interview_questions(
    specialty_id:        str,
    user_id:             int  = Query(default=None, description="userId pour exclure questions déjà posées"),
    excluded_questions:  str  = Query(default=None, description="JSON array de questions à exclure"),
    title:               str  = Query(default=None, description="Titre affiché (spécialité custom issue du CV)"),
    description:         str  = Query(default=None, description="Description (spécialité custom)"),
    level:               str  = Query(default=None, description="Niveau affiché (spécialité custom)"),
    count:               int  = Query(default=None, description="Nombre de questions voulu (spécialité custom)"),
    duration:            int  = Query(default=None, description="Durée en minutes (spécialité custom)"),
):
    """
    Génère les questions avec anti-répétition :
    - Pool de 30+ questions par spécialité connue
    - Sélection aléatoire différente à chaque session
    - Exclusion optionnelle des questions déjà posées à cet utilisateur
    - Spécialités "custom" (compétence détectée dans un CV, pas parmi les 6
      pré-câblées) : title/description/level/count/duration permettent à
      Gemini de générer des questions pertinentes au lieu de retomber sur Java.
    """
    normalized_id = _normalize_specialty_id(specialty_id)
    is_known = normalized_id in SPECIALTIES

    custom_spec = None
    if not is_known:
        if title:
            custom_spec = {
                "title": title, "description": description,
                "level": level, "count": count, "duration": duration
            }
        else:
            print(f"[WARN] /interview/questions a reçu un specialty_id inconnu sans titre custom : '{specialty_id}'")

    cache_key = normalized_id if is_known else f"custom:{normalized_id}"
    effective_count = (SPECIALTIES.get(normalized_id, {}).get("count")
                       or count or 15)

    # Parser les questions exclues si fournies
    excluded_list = []
    if excluded_questions:
        try:
            excluded_list = json.loads(excluded_questions)
        except Exception:
            pass

    # Vérifier le cache
    pool = _get_cached_pool(cache_key)

    if pool is None:
        # Générer via Gemini (ou pool local/générique) et mettre en cache
        fresh = generate_questions_by_specialty(specialty_id, excluded_list, custom_spec)
        _set_cache(cache_key, fresh)
        pool = _get_cached_pool(cache_key) or fresh

    # Filtrer les questions exclues (anti-répétition), avec filet de sécurité :
    # si l'exclusion laisse moins de questions que nécessaire, on garde le pool
    # complet plutôt que de renvoyer une session incomplète.
    if excluded_list:
        filtered = [q for q in pool if q.get("question") not in excluded_list]
        if len(filtered) >= effective_count:
            pool = filtered

    # Sélection aléatoire depuis le pool
    rng = random.Random(time.time())
    selected = pool.copy()
    rng.shuffle(selected)

    # Distribution équilibrée
    easy   = [q for q in selected if q.get("difficulty") == "easy"]
    medium = [q for q in selected if q.get("difficulty") == "medium"]
    hard   = [q for q in selected if q.get("difficulty") == "hard"]

    n_easy   = max(2, effective_count // 4)
    n_hard   = max(2, effective_count // 4)
    n_medium = effective_count - n_easy - n_hard

    result = (easy[:n_easy] + medium[:n_medium] + hard[:n_hard])[:effective_count]
    if len(result) < effective_count:
        extra = [q for q in selected if q not in result]
        result += extra[:effective_count - len(result)]

    # NB : on NE mélange PAS `result` ici — l'ordre easy → medium → hard construit
    # ci-dessus est intentionnel (entretien qui monte en difficulté). Le pool
    # `selected` a déjà été mélangé plus haut, donc les questions restent
    # variées à l'intérieur de chaque palier de difficulté.
    for i, q in enumerate(result):
        q = dict(q)
        q["id"] = i + 1
        result[i] = q

    return {"specialty_id": normalized_id, "questions": result}


@app.post("/interview/analyze-voice")
def interview_analyze_voice(request: VoiceAnalyzeRequest):
    """Analyse la réponse vocale (texte seulement) avec détection d'hésitations + scoring Gemini.
    Gardé pour compatibilité — préférez /interview/analyze-voice-audio si vous pouvez
    envoyer le fichier audio réel (analyse plus fiable du ton/rythme/stress)."""
    return analyze_voice_response(
        transcript=request.transcript,
        question=request.question,
        specialty=request.specialty
    )


@app.post("/interview/analyze-voice-audio")
async def interview_analyze_voice_audio(
    transcript: str = Form(...),
    question:   str = Form(...),
    specialty:  str = Form(...),
    audio:      UploadFile = File(None),
):
    """
    Comme /interview/analyze-voice, mais accepte en plus le fichier audio réel
    (webm/wav/mp3...) envoyé en multipart. Angular doit envoyer un FormData avec
    les champs 'transcript', 'question', 'specialty' et 'audio' (le blob audio).
    Gemini écoute alors le ton, le rythme et les vraies hésitations, pas seulement
    les mots détectés dans le texte transcrit.
    """
    audio_bytes = await audio.read() if audio else None
    audio_mime  = audio.content_type if audio else None
    return analyze_voice_response(
        transcript=transcript,
        question=question,
        specialty=specialty,
        audio_bytes=audio_bytes,
        audio_mime=audio_mime,
    )


@app.post("/interview/final-score")
def interview_final_score(request: FinalScoreRequest):
    """
    Combine le score de contenu (technique), la voix, et les métriques webcam
    (eye_contact, posture, engagement) en UN score global + UN score de stress
    cohérents. À appeler après avoir reçu les résultats de /interview/analyze-voice(-audio)
    et /interview/analyze-frame.
    """
    return compute_final_score(
        score_technique=request.score_technique,
        score_communication=request.score_communication,
        score_confiance=request.score_confiance,
        hesitation_ratio=request.hesitation_ratio,
        eye_contact=request.eye_contact,
        posture=request.posture,
        engagement=request.engagement,
        score_stress_vocal=request.score_stress_vocal,
    )


@app.post("/interview/analyze-frame")
async def interview_analyze_frame(frame: UploadFile = File(...)):
    """Computer vision : reçoit une frame JPEG depuis Spring Boot."""
    frame_bytes = await frame.read()
    return analyze_webcam_frame(frame_bytes)


@app.post("/interview/feedback")
def interview_feedback(request: FeedbackRequest):
    """Génère le rapport final + plan d'apprentissage personnalisé."""
    return generate_final_feedback(
        specialty=request.specialty,
        answers=request.answers,
        avg_scores=request.avg_scores
    )


@app.post("/interview/next-question")
def interview_next_question(request: NextQuestionRequest):
    """
    Difficulté progressive : retourne la prochaine question
    adaptée au score actuel de l'utilisateur.
    """
    next_q = get_next_question_adaptive(
        current_score=request.current_score,
        asked_ids=request.asked_ids,
        all_questions=request.all_questions
    )
    if next_q is None:
        return {"finished": True, "question": None}
    return {"finished": False, "question": next_q}


@app.delete("/interview/cache/{specialty_id}")
def clear_cache(specialty_id: str):
    """Vide le cache d'une spécialité (utile pour forcer un refresh)."""
    with _cache_lock:
        if specialty_id in _question_cache:
            del _question_cache[specialty_id]
    return {"message": f"Cache vidé pour {specialty_id}"}


@app.delete("/interview/cache")
def clear_all_cache():
    """Vide tout le cache de questions."""
    with _cache_lock:
        _question_cache.clear()
    return {"message": "Tout le cache vidé"}


# ─────────────────────────────────────────────────────────────────────────────
# Health
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    cache_info = {k: "cached" for k in _question_cache.keys()}
    return {
        "status": "ok",
        "groq": "configured" if GEMINI_API_KEY else "not configured (fallback mode)",
        "cache":  cache_info
    }