# ── Ajouter dans main.py (Python FastAPI) ───────────────────────────────────
# Ce endpoint reçoit une frame JPEG depuis Spring Boot et analyse :
#   - contact visuel (eye contact) via OpenCV Haar cascades
#   - posture via analyse de la position du visage dans le frame
#   - engagement via score composite

import cv2
import numpy as np
from fastapi import UploadFile, File
from fastapi.responses import JSONResponse

# Haar cascades (inclus dans opencv-python, pas besoin d'installation extra)
FACE_CASCADE  = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
EYE_CASCADE   = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_eye.xml')

def analyze_webcam_frame(frame_bytes: bytes) -> dict:
    """
    Analyse une frame JPEG et retourne des métriques comportementales.
    Utilise OpenCV (disponible sans GPU).
    """
    try:
        # Decode image
        nparr  = np.frombuffer(frame_bytes, np.uint8)
        frame  = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            return _default_metrics()

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        h, w = frame.shape[:2]

        # ── Détection visage ──
        faces = FACE_CASCADE.detectMultiScale(
            gray, scaleFactor=1.1, minNeighbors=5, minSize=(60, 60)
        )

        if len(faces) == 0:
            # No face detected → poor eye contact, low engagement
            return {
                "eye_contact": 40,
                "posture":     50,
                "engagement":  45,
                "face_detected": False,
                "tips": [
                    "Rapprochez-vous de la caméra",
                    "Assurez-vous d'être bien éclairé",
                    "Centrez votre visage dans le cadre"
                ]
            }

        # Use largest face
        fx, fy, fw, fh = max(faces, key=lambda f: f[2] * f[3])

        # ── Eye contact score ──
        # Based on: face position relative to frame center + eyes detected
        face_cx = fx + fw / 2
        face_cy = fy + fh / 2
        center_offset_x = abs(face_cx - w / 2) / (w / 2)   # 0=perfect, 1=edge
        center_offset_y = abs(face_cy - h / 2) / (h / 2)

        face_roi_gray = gray[fy:fy+fh, fx:fx+fw]
        eyes          = EYE_CASCADE.detectMultiScale(face_roi_gray, scaleFactor=1.1, minNeighbors=5)
        eyes_detected = len(eyes) >= 2

        eye_contact = int(
            100
            - (center_offset_x * 25)   # penalty for off-center horizontally
            - (center_offset_y * 15)   # penalty for off-center vertically
            + (10 if eyes_detected else -15)   # bonus/penalty for eye detection
        )
        eye_contact = max(30, min(100, eye_contact))

        # ── Posture score ──
        # Based on face size (too close/far = bad) + vertical position
        face_area_ratio = (fw * fh) / (w * h)
        if 0.06 <= face_area_ratio <= 0.30:
            posture_size = 90    # ideal size
        elif face_area_ratio < 0.04:
            posture_size = 55    # too far
        elif face_area_ratio > 0.45:
            posture_size = 60    # too close
        else:
            posture_size = 75

        # Vertical position (face should be in upper-center zone)
        vert_score = 90 if face_cy < h * 0.55 else 70

        posture = int((posture_size + vert_score) / 2)
        posture = max(30, min(100, posture))

        # ── Engagement score ──
        engagement = int((eye_contact * 0.6) + (posture * 0.4))
        engagement = max(40, min(100, engagement))

        # ── Dynamic tips ──
        tips = []
        if eye_contact < 65:
            tips.append("Regardez directement la caméra pour un meilleur contact visuel")
        if posture < 65:
            tips.append("Ajustez votre position — centrez votre visage dans le cadre")
        if not eyes_detected:
            tips.append("Améliorez l'éclairage pour que vos yeux soient bien visibles")
        if len(tips) == 0:
            tips.append("Excellent contact visuel — continuez ainsi !")

        return {
            "eye_contact":    eye_contact,
            "posture":        posture,
            "engagement":     engagement,
            "face_detected":  True,
            "eyes_detected":  eyes_detected,
            "tips":           tips
        }

    except Exception as e:
        return _default_metrics()


def _default_metrics() -> dict:
    return {
        "eye_contact":   75,
        "posture":       70,
        "engagement":    78,
        "face_detected": False,
        "tips": ["Maintenez le contact visuel avec la caméra"]
    }