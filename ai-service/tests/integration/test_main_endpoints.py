"""
Tests d'intégration pour main.py, via `TestClient` (pas de vrai serveur uvicorn).

Principe : on laisse FastAPI faire son vrai travail (routing, validation
Pydantic, sérialisation JSON) mais on mocke tout ce qui sort du processus
ou charge un modèle lourd :
  - extraction PDF (pdfplumber)
  - extraction CV / matching / ML (déjà couverts unitairement ailleurs)
  - appels Gemini/Groq (call_llm)
  - appel GitHub (analyze_github_profile)

Piège à connaître : `analyze_github` fait un import LOCAL
(`from github_analyzer import analyze_github_profile`) DANS le corps de
l'endpoint. Donc patcher `main.analyze_github_profile` ne sert à rien —
il faut patcher `github_analyzer.analyze_github_profile` directement.
"""
import io
import json
import pytest
from fastapi.testclient import TestClient

import main


@pytest.fixture
def client():
    return TestClient(main.app)


# ── /health ──────────────────────────────────────────────────────────────────

class TestHealthEndpoint:

    def test_health_returns_ok_status(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "ok"
        assert "groq_configured" in body

    def test_health_reflects_groq_key_presence(self, client, mocker):
        mocker.patch("main.GROQ_API_KEY", "fake-key")
        response = client.get("/health")
        assert response.json()["groq_configured"] is True


# ── /analyze ──────────────────────────────────────────────────────────────────

class TestAnalyzeEndpoint:

    def _mock_pipeline(self, mocker, skills=None):
        """Mocke toute la chaîne d'extraction pour isoler le routing/format
        de réponse de l'endpoint (la logique de chaque fonction est déjà
        testée unitairement dans test_extractor.py / test_ml_model.py)."""
        mocker.patch("main.extract_text_from_pdf", return_value="texte du cv")
        mocker.patch("main.extract_skills", return_value=skills or ["Java", "Docker"])
        mocker.patch("main.extract_education", return_value=[])
        mocker.patch("main.extract_experience", return_value=[])
        mocker.patch("main.extract_projects", return_value=[])
        mocker.patch("main.extract_languages", return_value=[])
        mocker.patch("main.extract_certifications", return_value=[])
        mocker.patch("main.predict_profile", return_value={
            "profile": "Backend", "confidence": 82, "global_score": 75,
            "skill_scores": {"Java": 90, "Docker": 70},
        })

    def test_analyze_returns_expected_shape(self, client, mocker):
        self._mock_pipeline(mocker)
        mocker.patch("main.GEMINI_API_KEY", None)  # force le mode fallback (sans appel Gemini)

        response = client.post(
            "/analyze",
            files={"file": ("cv.pdf", io.BytesIO(b"%PDF-1.4 fake"), "application/pdf")},
        )
        assert response.status_code == 200
        body = response.json()
        assert body["skills"] == ["Java", "Docker"]
        assert body["profile"] == "Backend"
        assert body["confidence"] == 82
        assert "summary" in body
        assert "optimization_suggestions" in body

    def test_analyze_fallback_summary_when_no_groq_key(self, client, mocker):
        """Sans clé Groq configurée, `generate_cv_analysis_insights` doit
        renvoyer le résumé de secours déterministe (pas d'appel réseau)."""
        self._mock_pipeline(mocker, skills=["Python"])
        mocker.patch("main.GEMINI_API_KEY", None)

        response = client.post(
            "/analyze",
            files={"file": ("cv.pdf", io.BytesIO(b"%PDF-1.4 fake"), "application/pdf")},
        )
        body = response.json()
        assert "Backend" in body["summary"]
        assert "75/100" in body["summary"] or "75" in body["summary"]

    def test_analyze_uses_llm_when_groq_key_configured(self, client, mocker):
        """Avec une clé configurée, l'insight doit venir du JSON renvoyé par
        call_llm (mocké ici) plutôt que du fallback."""
        self._mock_pipeline(mocker)
        mocker.patch("main.GEMINI_API_KEY", "fake-key")
        mocker.patch("main.GROQ_API_KEY", "fake-key")
        mocker.patch("main.call_llm", return_value=json.dumps({
            "summary": "Résumé généré par le LLM.",
            "proposed_summary": "Dev Backend Java/Docker.",
            "optimization_suggestions": ["Ajouter un lien GitHub."],
            "strengths": ["Java"],
            "weaknesses": ["Peu de certifications"],
            "recommendations": ["Faire un projet Kubernetes."],
        }))

        response = client.post(
            "/analyze",
            files={"file": ("cv.pdf", io.BytesIO(b"%PDF-1.4 fake"), "application/pdf")},
        )
        body = response.json()
        assert body["summary"] == "Résumé généré par le LLM."

    def test_analyze_missing_file_returns_422(self, client):
        response = client.post("/analyze")
        assert response.status_code == 422


# ── /match-job ────────────────────────────────────────────────────────────────

class TestMatchJobEndpoint:

    def test_match_job_delegates_to_match_skills(self, client, mocker):
        mocker.patch("main.match_skills", return_value={
            "matched": ["java"], "missing": [], "score": 100, "compatible": True,
        })
        response = client.post("/match-job", json={
            "cv_skills": ["Java"], "job_skills": ["Java"],
        })
        assert response.status_code == 200
        assert response.json()["compatible"] is True

    def test_match_job_invalid_payload_returns_422(self, client):
        response = client.post("/match-job", json={"cv_skills": ["Java"]})  # job_skills manquant
        assert response.status_code == 422


# ── /interview/specialties ────────────────────────────────────────────────────

class TestInterviewSpecialties:

    def test_returns_list_with_known_ids(self, client):
        response = client.get("/interview/specialties")
        assert response.status_code == 200
        ids = [s["id"] for s in response.json()]
        assert "java" in ids


# ── /interview/next-question ─────────────────────────────────────────────────

class TestInterviewNextQuestion:

    def test_no_remaining_questions_returns_finished(self, client):
        response = client.post("/interview/next-question", json={
            "specialty_id": "java",
            "current_score": 80,
            "asked_ids": [1, 2],
            "all_questions": [
                {"id": 1, "difficulty": "easy"},
                {"id": 2, "difficulty": "hard"},
            ],
        })
        assert response.status_code == 200
        assert response.json() == {"finished": True, "question": None}

    def test_high_score_prefers_hard_question(self, client):
        response = client.post("/interview/next-question", json={
            "specialty_id": "java",
            "current_score": 90,
            "asked_ids": [],
            "all_questions": [
                {"id": 1, "difficulty": "easy"},
                {"id": 2, "difficulty": "hard"},
            ],
        })
        body = response.json()
        assert body["finished"] is False
        assert body["question"]["difficulty"] == "hard"


# ── /interview/cache ──────────────────────────────────────────────────────────

class TestInterviewCache:

    def test_clear_single_specialty_cache(self, client):
        main._question_cache["java"] = {"pool": [], "expires": 9999999999}
        response = client.delete("/interview/cache/java")
        assert response.status_code == 200
        assert "java" not in main._question_cache

    def test_clear_all_cache(self, client):
        main._question_cache["java"] = {"pool": [], "expires": 9999999999}
        main._question_cache["angular"] = {"pool": [], "expires": 9999999999}
        response = client.delete("/interview/cache")
        assert response.status_code == 200
        assert main._question_cache == {}


# ── /interview/analyze-frame (computer vision) ───────────────────────────────

class TestAnalyzeFrameEndpoint:

    def test_undecodable_frame_returns_default_metrics(self, client, mocker):
        """cv2.imdecode() renvoie None sur des bytes non-JPEG valides
        -> doit retomber sur les métriques par défaut, jamais planter."""
        mocker.patch("main.cv2.imdecode", return_value=None)
        response = client.post(
            "/interview/analyze-frame",
            files={"frame": ("frame.jpg", io.BytesIO(b"not a real jpeg"), "image/jpeg")},
        )
        assert response.status_code == 200
        body = response.json()
        assert body["face_detected"] is False


# ── /analyze-github ───────────────────────────────────────────────────────────

class TestAnalyzeGithubEndpoint:

    def test_delegates_to_github_analyzer(self, client, mocker):
        """Import local dans l'endpoint -> on patche github_analyzer
        directement, pas main.analyze_github_profile (qui n'existe pas
        tant que la fonction n'a pas été appelée une première fois)."""
        mocker.patch(
            "github_analyzer.analyze_github_profile",
            return_value={"username": "safa-dev", "public_repos": 12},
        )
        response = client.get("/analyze-github", params={"username": "safa-dev"})
        assert response.status_code == 200
        assert response.json()["username"] == "safa-dev"

    def test_missing_username_returns_422(self, client):
        response = client.get("/analyze-github")
        assert response.status_code == 422
