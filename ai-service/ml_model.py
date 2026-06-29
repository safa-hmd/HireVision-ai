import joblib
import os
import pandas as pd

MODEL_PATH = "model.joblib"

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


def load_model():
    if os.path.exists(MODEL_PATH):
        return joblib.load(MODEL_PATH)
    return None


def skill_to_vector(skills: list[str]) -> list[int]:
    skills_lower = [s.lower() for s in skills]
    vector = []
    for col in SKILL_COLUMNS:
        found = False
        for s in skills_lower:
            # Check direct match
            if col in s or s in col:
                found = True
                break
            # Check alias
            alias = SKILL_ALIASES.get(s)
            if alias == col:
                found = True
                break
        vector.append(1 if found else 0)
    return vector


def predict_profile(skills: list[str]) -> dict:
    model = load_model()
    if model is None:
        return {"profile": "Non déterminé", "confidence": 0, "global_score": 0, "skill_scores": {}}

    vector = skill_to_vector(skills)
    df = pd.DataFrame([vector], columns=SKILL_COLUMNS)

    prediction = model.predict(df)[0]
    proba = model.predict_proba(df)[0]
    confidence = round(max(proba) * 100)

    # Score par skill selon le profil détecté
    scores = {}
    profile_weights = {
        "Backend":   ["java", "spring", "python", "php", "node", "csharp", "dotnet", "cpp"],
        "Frontend":  ["angular", "react", "vue", "typescript", "javascript", "flutter", "dart"],
        "FullStack": ["java", "spring", "angular", "react", "python", "mysql", "typescript"],
        "DevOps":    ["docker", "kubernetes", "azure", "aws", "cicd", "linux"],
        "MLOps":     ["python", "mlflow", "docker", "kubernetes", "aws", "azure"]
    }

    core_skills = profile_weights.get(prediction, [])

    for skill in skills:
        skill_lower = skill.lower()
        normalized = SKILL_ALIASES.get(skill_lower, skill_lower)
        if any(core in skill_lower or core in normalized for core in core_skills):
            scores[skill] = min(95, 75 + confidence // 5)
        elif any(col in skill_lower for col in ["docker", "kubernetes", "aws", "azure", "cicd"]):
            scores[skill] = 80
        elif any(col in skill_lower for col in ["git", "github", "agile", "scrum", "rest", "api"]):
            scores[skill] = 75
        else:
            scores[skill] = 65

    global_score = round(sum(scores.values()) / len(scores)) if scores else 0

    return {
        "profile": prediction,
        "confidence": confidence,
        "global_score": global_score,
        "skill_scores": scores
    }