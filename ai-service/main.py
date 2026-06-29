import os
import tempfile
import httpx
import pdfplumber
import cv2
import numpy as np
from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from extractor import (
    extract_skills, extract_education, extract_experience,
    extract_languages, extract_certifications, extract_name
)
from ml_model import predict_profile
from job_matcher import match_skills
from interview_analyzer import (
    get_specialties,
    generate_questions_by_specialty,
    analyze_voice_response,
    generate_final_feedback
)

app = FastAPI(title="HireVision AI Microservice")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

# ── OpenCV Haar Cascades (computer vision) ────────────────────────────────────
FACE_CASCADE = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
EYE_CASCADE  = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_eye.xml')


# ── Pydantic Models ───────────────────────────────────────────────────────────

class JobMatchRequest(BaseModel):
    cv_skills: list[str]
    job_skills: list[str]

class QuestionRequest(BaseModel):
    skills: list[str]
    job_description: str
    candidate_name: str = ""
    profile: str = ""

class EvaluateRequest(BaseModel):
    question: str
    answer: str
    category: str
    skills: list[str]

class VoiceAnalyzeRequest(BaseModel):
    transcript: str
    question: str
    specialty: str

class FeedbackRequest(BaseModel):
    specialty: str
    answers: list
    avg_scores: dict


# ── Helpers ───────────────────────────────────────────────────────────────────

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
    except:
        return f"Profil {profile} avec score {score}/100."


def evaluate_answer_with_gemini(question: str, answer: str, category: str, skills: list[str]) -> dict:
    if not GEMINI_API_KEY:
        return {"score": 70, "niveau": "Bien", "points_forts": "Bonne réponse",
                "points_ameliorer": "Plus de détails", "reponse_ideale": "Réponse structurée avec exemples"}
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
        import json
        raw = data["candidates"][0]["content"]["parts"][0]["text"].strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"): raw = raw[4:]
        return json.loads(raw.strip())
    except:
        return {"score": 70, "niveau": "Bien", "points_forts": "Réponse reçue",
                "points_ameliorer": "Développez davantage", "reponse_ideale": "Réponse avec exemples concrets"}


def analyze_webcam_frame(frame_bytes: bytes) -> dict:
    """Computer vision : analyse une frame JPEG, retourne eye_contact, posture, engagement."""
    try:
        nparr = np.frombuffer(frame_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            return _default_vision_metrics()

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        h, w = frame.shape[:2]

        faces = FACE_CASCADE.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60))

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
        eyes = EYE_CASCADE.detectMultiScale(face_roi_gray, scaleFactor=1.1, minNeighbors=5)
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
        posture = max(30, min(100, int((posture_size + vert_score) / 2)))
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
            "eye_contact": eye_contact,
            "posture": posture,
            "engagement": engagement,
            "face_detected": True,
            "eyes_detected": eyes_detected,
            "tips": tips
        }
    except:
        return _default_vision_metrics()


def _default_vision_metrics() -> dict:
    return {
        "eye_contact": 75, "posture": 70, "engagement": 78,
        "face_detected": False,
        "tips": ["Maintenez le contact visuel avec la caméra"]
    }


# ── CV Endpoints ──────────────────────────────────────────────────────────────

@app.post("/analyze")
async def analyze_cv(file: UploadFile = File(...)):
    file_bytes = await file.read()
    raw_text = extract_text_from_pdf(file_bytes)
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
        "skills": skills, "education": education, "experience": experience,
        "certifications": certifications, "languages": languages,
        "summary": summary, "profile": ml_result.get("profile"),
        "confidence": ml_result.get("confidence"),
        "global_score": ml_result.get("global_score"),
        "skill_scores": ml_result.get("skill_scores", {})
    }


# ── Job Matching ──────────────────────────────────────────────────────────────

@app.post("/match-job")
def match_job(request: JobMatchRequest):
    return match_skills(request.cv_skills, request.job_skills)


# ── Question / Evaluate ───────────────────────────────────────────────────────

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


# ── Interview Session Endpoints ───────────────────────────────────────────────
# Appelés par Spring Boot (:8086) qui sert de proxy entre Angular et ce service

@app.get("/interview/specialties")
def interview_specialties():
    return get_specialties()


@app.get("/interview/questions/{specialty_id}")
def interview_questions(specialty_id: str):
    questions = generate_questions_by_specialty(specialty_id)
    return {"specialty_id": specialty_id, "questions": questions}


@app.post("/interview/analyze-voice")
def interview_analyze_voice(request: VoiceAnalyzeRequest):
    return analyze_voice_response(
        transcript=request.transcript,
        question=request.question,
        specialty=request.specialty
    )


@app.post("/interview/analyze-frame")
async def interview_analyze_frame(frame: UploadFile = File(...)):
    """Computer vision : reçoit une frame JPEG depuis Spring Boot, retourne les métriques comportementales."""
    frame_bytes = await frame.read()
    return analyze_webcam_frame(frame_bytes)


@app.post("/interview/feedback")
def interview_feedback(request: FeedbackRequest):
    return generate_final_feedback(
        specialty=request.specialty,
        answers=request.answers,
        avg_scores=request.avg_scores
    )


# ── Health ────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}