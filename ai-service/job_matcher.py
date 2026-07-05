"""
Matching CV <-> Offre d'emploi — version SÉMANTIQUE.

Avant : "job_skill in cv_skill or cv_skill in job_skill" (sous-chaîne brute).
   Bug concret : une compétence "R" (le langage) matchait "React" ou "Spring"
   juste parce que la lettre "r" apparaît dans ces mots.

Maintenant : on compare les compétences par similarité d'embeddings. Deux
compétences qui ne partagent aucune lettre en commun mais qui veulent dire
la même chose (ex: "Spring Boot" / "Spring Framework", "GCP" / "Google Cloud")
sont reconnues comme proches. Et une compétence courte comme "R" n'aura une
similarité forte qu'avec... "R" ou des textes qui parlent vraiment du langage R.
"""
from embeddings import cosine_sim, best_match

SKILL_ALIASES = {
    "c++": "cpp", "c#": "csharp", ".net": "dotnet",
    "ci/cd": "cicd", "reactjs": "react", "nodejs": "node",
    "spring boot": "spring", "next.js": "nextjs",
    "js": "javascript", "ts": "typescript", "k8s": "kubernetes",
    "github actions": "cicd"
}

# Seuils calibrés empiriquement sur le modèle multilingue MiniLM.
# À ajuster si vous changez de modèle d'embeddings.
EXACT_THRESHOLD = 0.97     # quasi identique (alias, casse, pluriel...)
PARTIAL_THRESHOLD = 0.62   # même famille technologique / sens proche


def normalize(skill: str) -> str:
    s = skill.lower().strip()
    return SKILL_ALIASES.get(s, s)


def match_skills(cv_skills: list, job_skills: list) -> dict:
    cv_normalized = [normalize(s) for s in cv_skills]
    job_normalized = [normalize(s) for s in job_skills]

    if not job_normalized:
        return {"error": "Aucune compétence job fournie"}

    matched, partial, missing = [], [], []

    for job_skill in job_normalized:
        # Match exact après normalisation (le plus fiable, ne coûte rien)
        if job_skill in cv_normalized:
            matched.append(job_skill)
            continue

        if not cv_normalized:
            missing.append(job_skill)
            continue

        best_cv_skill, score = best_match(job_skill, cv_normalized)

        if score >= EXACT_THRESHOLD:
            matched.append(job_skill)
        elif score >= PARTIAL_THRESHOLD:
            partial.append({"job": job_skill, "cv": best_cv_skill, "similarity": round(score, 2)})
        else:
            missing.append(job_skill)

    total = len(job_normalized)
    exact_count = len(matched)
    partial_count = len(partial)

    weighted_score = (exact_count + partial_count * 0.6) / total
    score_percent = round(weighted_score * 100)

    if score_percent >= 70:
        compatible, label, message = True, "✅ Compatible", "Profil fortement compatible avec ce poste."
    elif score_percent >= 45:
        compatible, label, message = True, "⚠️ Partiellement compatible", "Profil compatible mais quelques compétences manquent."
    else:
        compatible, label, message = False, "❌ Non compatible", "Trop de compétences manquantes pour ce poste."

    return {
        "score": score_percent,
        "compatible": compatible,
        "label": label,
        "message": message,
        "total_required": total,
        "matched_exact": exact_count,
        "matched_partial": partial_count,
        "missing_count": len(missing),
        "matched": matched,
        "partial": partial,
        "missing": missing
    }
