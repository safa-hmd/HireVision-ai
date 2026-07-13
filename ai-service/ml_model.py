"""
Classification de profil — version SÉMANTIQUE (zero-shot par embeddings).

Pourquoi ce changement :
- L'ancienne version encodait les compétences en vecteur binaire (23 colonnes
  fixes) puis demandait à un RandomForest entraîné sur... 44 lignes de tenter
  de généraliser sur 5 classes. Avec si peu de données, le modèle mémorise le
  dataset plus qu'il n'apprend une vraie règle -> ses prédictions ne sont pas
  fiables sur de nouveaux CV.
- Le vecteur binaire faisait aussi du matching par sous-chaîne bugué
  ("c" in "react" -> vrai) : une compétence "C" activait à tort la colonne
  "react", "csharp", etc.

Nouvelle approche : on ne dépend plus d'un dictionnaire de colonnes figées.
On décrit chaque PROFIL par une phrase de référence, puis on compare le sens
des compétences du candidat à ces phrases via des embeddings. Ça fonctionne
dès le premier jour, sans dataset d'entraînement, et reconnaît des technologies
jamais vues explicitement (ex: "Quarkus" est proche sémantiquement de "Java
backend" même si "Quarkus" n'apparaît dans aucune liste écrite à la main).

Le RandomForest entraîné (train_model.py / model.joblib) reste disponible et
est utilisé en signal SECONDAIRE, uniquement pour affiner la confiance quand
il est d'accord avec la prédiction sémantique — jamais comme unique source de
vérité tant que le dataset n'a pas une taille raisonnable (voir train_model.py).
"""
import os
import joblib
import numpy as np
import pandas as pd
from embeddings import embed, cosine_sim

MODEL_PATH = "model.joblib"

# ── Colonnes conservées pour compatibilité avec model.joblib existant ───────
SKILL_COLUMNS = [
    "java", "spring", "angular", "react", "vue", "python", "php",
    "docker", "kubernetes", "azure", "aws", "mysql", "mongodb",
    "typescript", "javascript", "node", "cicd", "mlflow",
    "cpp", "csharp", "dart", "flutter", "dotnet"
]

SKILL_ALIASES = {
    "c++": "cpp", "c#": "csharp", ".net": "dotnet",
    "ci/cd": "cicd", "reactjs": "react", "nodejs": "node",
    "next.js": "node", "spring boot": "spring",
    "scikit-learn": "python", "pandas": "python",
    "pytorch": "python", "tensorflow": "python",
    "keras": "python", "flask": "python",
    "django": "python", "fastapi": "python",
    "github actions": "cicd", "jenkins": "cicd"
}

# ── Descriptions de référence par profil : le coeur de la classification ───
# IMPORTANT : ne JAMAIS avoir uniquement des profils IT ici, sinon le modèle
# est structurellement obligé de faire rentrer un CV non-IT (mécanique,
# électrique, civil, business...) dans une case informatique. On garde une
# liste ouverte, et surtout un seuil de confiance qui déclenche un profil
# générique plutôt qu'un faux positif IT.
PROFILE_DESCRIPTIONS = {
    "Backend": "développeur backend : Java, Spring Boot, Python, PHP, Node.js, "
               "C#, .NET, API REST, bases de données, architecture serveur",
    "Frontend": "développeur frontend : Angular, React, Vue, TypeScript, "
                "JavaScript, HTML, CSS, interfaces utilisateur, Flutter, Dart",
    "FullStack": "développeur fullstack : backend Java Spring et frontend "
                 "Angular React, bases de données, API REST, applications complètes",
    "DevOps": "ingénieur DevOps : Docker, Kubernetes, CI/CD, cloud AWS Azure, "
              "infrastructure, automatisation des déploiements",
    "MLOps": "ingénieur MLOps machine learning : Python, MLflow, entraînement "
             "de modèles, Docker, Kubernetes, déploiement de modèles ML en production",
    "Mécanique": "ingénieur génie mécanique : conception mécanique, CAO SolidWorks "
                 "CATIA, résistance des matériaux, thermique, transmission de puissance, "
                 "engrenages, maintenance industrielle",
    "Électrique": "ingénieur génie électrique et électrotechnique : automates "
                  "programmables, GRAFCET, variateurs de vitesse, moteurs électriques, "
                  "circuits de puissance, électronique de commande",
    "Civil": "ingénieur génie civil : béton armé, structures, chantier, "
             "topographie, AutoCAD, dimensionnement de bâtiments",
    "Business": "gestion, business, finance, marketing, comptabilité, "
                "management de projet, ressources humaines",
}

CATEGORY_HINTS = {
    "Backend": ["java", "spring", "python", "php", "node", "csharp", "dotnet", "cpp", "mysql", "mongodb"],
    "Frontend": ["angular", "react", "vue", "typescript", "javascript", "flutter", "dart"],
    "DevOps": ["docker", "kubernetes", "azure", "aws", "cicd"],
    "MLOps": ["python", "mlflow", "docker", "kubernetes", "aws", "azure"],
    "Mécanique": ["solidworks", "catia", "autocad", "rdm", "engrenages", "thermique"],
    "Électrique": ["grafcet", "automates", "siemens", "schneider", "variateur", "moteur"],
    "Civil": ["autocad", "béton", "structures", "topographie"],
    "Business": ["gestion", "finance", "marketing", "management", "rh"],
}

# En dessous de ce seuil de similarité cosinus BRUTE (pas la pseudo-proba
# normalisée après division par la somme), on considère qu'aucun profil ne
# correspond vraiment et on renvoie un profil générique plutôt qu'une
# fausse certitude ("DevOps" pour un CV mécanique, par exemple).
MIN_RAW_SIMILARITY = 0.28


def load_rf_model():
    """Charge le RandomForest si présent (signal secondaire optionnel)."""
    if os.path.exists(MODEL_PATH):
        try:
            return joblib.load(MODEL_PATH)
        except Exception:
            return None
    return None


def skill_to_vector(skills: list[str]) -> list[int]:
    """
    Conservé pour le RandomForest existant, mais CORRIGÉ :
    l'ancien code faisait `col in s or s in col`, ce qui matchait
    "c" avec "react" (car la lettre "c" est une sous-chaîne de "react").
    Désormais on exige une égalité EXACTE après normalisation par alias,
    sauf pour les colonnes de 4+ caractères où un "contient" à sens unique
    (skill contient col) reste acceptable et sûr.
    """
    skills_normalized = []
    for s in skills:
        s_low = s.lower().strip()
        skills_normalized.append(SKILL_ALIASES.get(s_low, s_low))

    vector = []
    for col in SKILL_COLUMNS:
        found = any(
            s == col or (len(col) >= 4 and col in s)
            for s in skills_normalized
        )
        vector.append(1 if found else 0)
    return vector


def _semantic_profile_scores(skills: list[str]) -> tuple[dict, dict]:
    """Similarité cosinus entre le profil de compétences du candidat et
    chaque description de référence. Retourne (scores_bruts, scores_normalisés).
    Les scores bruts servent à détecter le cas "aucun profil ne correspond
    vraiment" ; les scores normalisés servent à départager entre profils
    quand il y a un vrai signal."""
    if not skills:
        raw = {p: 0.0 for p in PROFILE_DESCRIPTIONS}
        return raw, raw

    skills_text = ", ".join(skills)
    raw = {
        profile: max(0.0, cosine_sim(skills_text, description))
        for profile, description in PROFILE_DESCRIPTIONS.items()
    }
    total = sum(raw.values()) or 1e-9
    normalized = {p: v / total for p, v in raw.items()}
    return raw, normalized


def predict_profile(skills: list[str]) -> dict:
    if not skills:
        return {"profile": "Non déterminé", "confidence": 0, "global_score": 0, "skill_scores": {}}

    # ── 1) Prédiction sémantique (source principale, fonctionne sans entraînement) ──
    raw_scores, semantic_scores = _semantic_profile_scores(skills)
    semantic_profile = max(semantic_scores, key=semantic_scores.get)
    semantic_confidence = semantic_scores[semantic_profile]

    # Garde-fou : si même le meilleur profil a une similarité brute trop
    # faible, aucun des profils connus ne décrit vraiment ce candidat.
    # On refuse de forcer un label IT et on renvoie un profil générique
    # qui laisse le reste du pipeline (recommandations d'entretien, etc.)
    # basculer sur un mode "custom" au lieu d'un mode IT câblé en dur.
    if raw_scores[semantic_profile] < MIN_RAW_SIMILARITY:
        return {
            "profile": "Profil non catégorisé",
            "confidence": round(raw_scores[semantic_profile] * 100),
            "global_score": 60,
            "skill_scores": {s: 60 for s in skills},
            "is_generic": True,
        }

    # ── 2) Signal secondaire du RandomForest, si le modèle est chargeable ──
    rf_model = load_rf_model()
    final_profile = semantic_profile
    final_confidence = semantic_confidence

    if rf_model is not None:
        try:
            vector = skill_to_vector(skills)
            df = pd.DataFrame([vector], columns=SKILL_COLUMNS)
            rf_pred = rf_model.predict(df)[0]
            rf_proba = max(rf_model.predict_proba(df)[0])

            if rf_pred == semantic_profile:
                # Les deux méthodes sont d'accord -> on renforce la confiance.
                final_confidence = min(0.99, (semantic_confidence + rf_proba) / 2 + 0.15)
            # Si elles sont en désaccord, on privilégie la méthode sémantique
            # (le RF est entraîné sur un dataset encore trop petit pour trancher).
        except Exception:
            pass  # le RF reste purement optionnel

    confidence_pct = round(final_confidence * 100)

    # ── 3) Score par compétence : similarité sémantique compétence <-> profil ──
    core_skills = CATEGORY_HINTS.get(final_profile, [])
    scores = {}
    for skill in skills:
        skill_lower = skill.lower()
        sim_to_profile = cosine_sim(skill, PROFILE_DESCRIPTIONS[final_profile])
        base = 60 + round(sim_to_profile * 35)  # échelle ~60-95 selon pertinence sémantique
        if any(core in skill_lower for core in core_skills):
            base = max(base, 80)
        scores[skill] = max(50, min(97, base))

    global_score = round(sum(scores.values()) / len(scores)) if scores else 0

    return {
        "profile": final_profile,
        "confidence": confidence_pct,
        "global_score": global_score,
        "skill_scores": scores
    }