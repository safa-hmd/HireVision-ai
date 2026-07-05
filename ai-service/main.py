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
    extract_languages, extract_certifications, extract_name
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

app = FastAPI(title="HireVision AI Microservice")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_URL     = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

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
        response = httpx.post(
            f"{GEMINI_URL}?key={GEMINI_API_KEY}",
            json={"contents": [{"parts": [{"text": prompt}]}]},
            timeout=30
        )
        data = response.json()
        return data["candidates"][0]["content"]["parts"][0]["text"]
    except Exception:
        return f"Profil {profile} avec score {score}/100."


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
        response = httpx.post(
            f"{GEMINI_URL}?key={GEMINI_API_KEY}",
            json={"contents": [{"parts": [{"text": prompt}]}]},
            timeout=30
        )
        data = response.json()
        raw = data["candidates"][0]["content"]["parts"][0]["text"].strip()
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
    languages      = extract_languages(raw_text)
    certifications = extract_certifications(raw_text)
    ml_result      = predict_profile(skills)
    summary        = generate_feedback_with_gemini(
        skills, ml_result.get("profile", "Non déterminé"), ml_result.get("global_score", 0)
    )
    return {
        "skills":         skills,
        "education":      education,
        "experience":     experience,
        "certifications": certifications,
        "languages":      languages,
        "summary":        summary,
        "profile":        ml_result.get("profile"),
        "confidence":     ml_result.get("confidence"),
        "global_score":   ml_result.get("global_score"),
        "skill_scores":   ml_result.get("skill_scores", {})
    }


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
    excluded_questions:  str  = Query(default=None, description="JSON array de questions à exclure")
):
    """
    Génère les questions avec anti-répétition :
    - Pool de 30+ questions par spécialité
    - Sélection aléatoire différente à chaque session
    - Exclusion optionnelle des questions déjà posées à cet utilisateur
    """
    specialty_id = _normalize_specialty_id(specialty_id)
    if specialty_id not in SPECIALTIES:
        print(f"[WARN] /interview/questions a reçu un specialty_id inconnu : '{specialty_id}'")
    count = SPECIALTIES.get(specialty_id, {}).get("count", 15)

    # Parser les questions exclues si fournies
    excluded_list = []
    if excluded_questions:
        try:
            excluded_list = json.loads(excluded_questions)
        except Exception:
            pass

    # Vérifier le cache
    pool = _get_cached_pool(specialty_id)

    if pool is None:
        # Générer via Gemini (ou pool local) et mettre en cache
        fresh = generate_questions_by_specialty(specialty_id, excluded_list)
        _set_cache(specialty_id, fresh)
        pool = _get_cached_pool(specialty_id) or fresh

    # Filtrer les questions exclues (anti-répétition), avec filet de sécurité :
    # si l'exclusion laisse moins de questions que nécessaire, on garde le pool
    # complet plutôt que de renvoyer une session incomplète.
    if excluded_list:
        filtered = [q for q in pool if q.get("question") not in excluded_list]
        if len(filtered) >= count:
            pool = filtered

    # Sélection aléatoire depuis le pool
    rng = random.Random(time.time())
    selected = pool.copy()
    rng.shuffle(selected)

    # Distribution équilibrée
    easy   = [q for q in selected if q.get("difficulty") == "easy"]
    medium = [q for q in selected if q.get("difficulty") == "medium"]
    hard   = [q for q in selected if q.get("difficulty") == "hard"]

    n_easy   = max(2, count // 4)
    n_hard   = max(2, count // 4)
    n_medium = count - n_easy - n_hard

    result = (easy[:n_easy] + medium[:n_medium] + hard[:n_hard])[:count]
    if len(result) < count:
        extra = [q for q in selected if q not in result]
        result += extra[:count - len(result)]

    rng.shuffle(result)
    for i, q in enumerate(result):
        q = dict(q)
        q["id"] = i + 1
        result[i] = q

    return {"specialty_id": specialty_id, "questions": result}


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
        "gemini": "configured" if GEMINI_API_KEY else "not configured (fallback mode)",
        "cache":  cache_info
    }