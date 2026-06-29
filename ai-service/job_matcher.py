from difflib import SequenceMatcher

SKILL_ALIASES = {
    "c++": "cpp", "c#": "csharp", ".net": "dotnet",
    "ci/cd": "cicd", "reactjs": "react", "nodejs": "node",
    "spring boot": "spring", "next.js": "nextjs",
    "js": "javascript", "ts": "typescript", "k8s": "kubernetes",
    "github actions": "cicd"
}

def normalize(skill: str) -> str:
    s = skill.lower().strip()
    return SKILL_ALIASES.get(s, s)

def fuzzy_match(a: str, b: str) -> float:
    return SequenceMatcher(None, a, b).ratio()

def match_skills(cv_skills: list, job_skills: list) -> dict:
    cv_normalized  = [normalize(s) for s in cv_skills]
    job_normalized = [normalize(s) for s in job_skills]

    matched  = []
    missing  = []
    partial  = []

    for job_skill in job_normalized:
        if job_skill in cv_normalized:
            matched.append(job_skill)
            continue

        partial_found = False
        for cv_skill in cv_normalized:
            if job_skill in cv_skill or cv_skill in job_skill:
                partial.append({"job": job_skill, "cv": cv_skill})
                partial_found = True
                break

        if partial_found:
            continue

        best_score = 0
        best_match = ""
        for cv_skill in cv_normalized:
            score = fuzzy_match(job_skill, cv_skill)
            if score > best_score:
                best_score = score
                best_match = cv_skill

        if best_score >= 0.80:
            partial.append({"job": job_skill, "cv": best_match})
        else:
            missing.append(job_skill)

    total         = len(job_normalized)
    exact_count   = len(matched)
    partial_count = len(partial)

    if total == 0:
        return {"error": "Aucune compétence job fournie"}

    weighted_score = (exact_count + partial_count * 0.6) / total
    score_percent  = round(weighted_score * 100)

    if score_percent >= 70:
        compatible = True
        label      = "✅ Compatible"
        message    = "Profil fortement compatible avec ce poste."
    elif score_percent >= 45:
        compatible = True
        label      = "⚠️ Partiellement compatible"
        message    = "Profil compatible mais quelques compétences manquent."
    else:
        compatible = False
        label      = "❌ Non compatible"
        message    = "Trop de compétences manquantes pour ce poste."

    return {
        "score":           score_percent,
        "compatible":      compatible,
        "label":           label,
        "message":         message,
        "total_required":  total,
        "matched_exact":   exact_count,
        "matched_partial": partial_count,
        "missing_count":   len(missing),
        "matched":         matched,
        "partial":         partial,
        "missing":         missing
    }