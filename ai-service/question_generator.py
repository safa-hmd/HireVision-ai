from llm_client import call_llm


def generate_interview_questions(
    skills: list[str],
    job_description: str,
    candidate_name: str = "",
    profile: str = ""
) -> list[dict]:

    prompt = f"""Tu es un expert RH et recruteur technique senior.

Génère exactement 20 questions d'entretien professionnelles pour ce candidat :

PROFIL CANDIDAT :
- Nom : {candidate_name or "le candidat"}
- Profil détecté : {profile or "Développeur"}
- Compétences : {', '.join(skills)}

DESCRIPTION DU POSTE :
{job_description}

STRUCTURE OBLIGATOIRE — génère exactement dans cet ordre :
- 2 questions "Présentation personnelle" (qui es-tu, parcours)
- 3 questions "Formation et éducation" (diplôme, école, choix)
- 4 questions "Projets" (tes projets concrets, défis, résultats)
- 6 questions "Compétences techniques pratiques" (code, archi, outils)
- 3 questions "Théorie et concepts" (définitions, principes)
- 2 questions "Motivation et soft skills" (pourquoi ce poste, travail en équipe)

FORMAT DE RÉPONSE — réponds UNIQUEMENT en JSON valide, sans markdown, sans backticks :
[
  {{
    "id": 1,
    "category": "Présentation personnelle",
    "question": "...",
    "difficulty": "easy",
    "tip": "conseil court pour bien répondre"
  }},
  ...
]

Les questions doivent être en français, personnalisées selon les compétences et le poste."""

    try:
        raw_text = call_llm(prompt, timeout=25)

        # Nettoyer le JSON
        raw_text = raw_text.strip()
        if raw_text.startswith("```"):
            raw_text = raw_text.split("```")[1]
            if raw_text.startswith("json"):
                raw_text = raw_text[4:]
        raw_text = raw_text.strip()

        import json
        questions = json.loads(raw_text)
        return questions

    except Exception as e:
        # Fallback si Gemini échoue
        return generate_fallback_questions(skills, job_description)


def generate_fallback_questions(skills: list[str], job_description: str) -> list[dict]:
    top_skills = skills[:5] if skills else ["Java", "Spring Boot"]

    return [
        {"id": 1,  "category": "Présentation personnelle",      "difficulty": "easy",   "tip": "Sois concis et structuré",              "question": "Pouvez-vous vous présenter brièvement et nous parler de votre parcours ?"},
        {"id": 2,  "category": "Présentation personnelle",      "difficulty": "easy",   "tip": "Montre ta motivation",                  "question": "Pourquoi avez-vous choisi l'informatique comme domaine d'études ?"},
        {"id": 3,  "category": "Formation et éducation",        "difficulty": "easy",   "tip": "Cite des projets académiques concrets", "question": "Parlez-nous de votre formation et des projets académiques qui vous ont le plus marqué."},
        {"id": 4,  "category": "Formation et éducation",        "difficulty": "easy",   "tip": "Montre ta curiosité",                   "question": "Quels cours ou modules ont été les plus utiles pour votre développement professionnel ?"},
        {"id": 5,  "category": "Formation et éducation",        "difficulty": "easy",   "tip": "Parle de tes certifications si tu en as","question": "Avez-vous suivi des formations complémentaires ou obtenu des certifications ?"},
        {"id": 6,  "category": "Projets",                       "difficulty": "medium", "tip": "Utilise la méthode STAR",               "question": f"Décrivez votre projet le plus complexe utilisant {top_skills[0] if top_skills else 'Java'}."},
        {"id": 7,  "category": "Projets",                       "difficulty": "medium", "tip": "Montre ton rôle précis",                "question": "Quel a été votre rôle dans vos projets d'équipe ? Comment avez-vous collaboré ?"},
        {"id": 8,  "category": "Projets",                       "difficulty": "medium", "tip": "Parle des problèmes résolus",           "question": "Quel problème technique difficile avez-vous résolu dans un projet ? Comment ?"},
        {"id": 9,  "category": "Projets",                       "difficulty": "hard",   "tip": "Montre l'impact de ton travail",        "question": "Comment avez-vous géré les délais et les priorités dans vos projets ?"},
        {"id": 10, "category": "Compétences techniques pratiques","difficulty": "medium","tip": "Donne des exemples concrets",           "question": f"Comment utilisez-vous {top_skills[1] if len(top_skills) > 1 else 'Spring Boot'} dans vos projets ?"},
        {"id": 11, "category": "Compétences techniques pratiques","difficulty": "hard",  "tip": "Montre ta maîtrise de l'architecture", "question": "Comment structurez-vous une application REST API avec Spring Boot ?"},
        {"id": 12, "category": "Compétences techniques pratiques","difficulty": "hard",  "tip": "Parle de cas réels",                   "question": "Comment gérez-vous l'authentification et l'autorisation dans vos applications ?"},
        {"id": 13, "category": "Compétences techniques pratiques","difficulty": "medium","tip": "Cite des outils concrets",              "question": "Quelle est votre approche pour tester votre code ? Quels outils utilisez-vous ?"},
        {"id": 14, "category": "Compétences techniques pratiques","difficulty": "hard",  "tip": "Parle de CI/CD si tu connais",         "question": "Comment gérez-vous le déploiement et l'intégration continue de vos applications ?"},
        {"id": 15, "category": "Compétences techniques pratiques","difficulty": "medium","tip": "Montre ta rigueur",                    "question": "Comment optimisez-vous les performances de vos requêtes de base de données ?"},
        {"id": 16, "category": "Théorie et concepts",            "difficulty": "hard",   "tip": "Sois précis et donne des exemples",    "question": "Expliquez les principes SOLID et comment vous les appliquez."},
        {"id": 17, "category": "Théorie et concepts",            "difficulty": "hard",   "tip": "Montre ta compréhension profonde",     "question": "Quelle est la différence entre une architecture monolithique et microservices ?"},
        {"id": 18, "category": "Théorie et concepts",            "difficulty": "medium", "tip": "Donne des exemples concrets",          "question": "Expliquez les concepts de base de la programmation orientée objet."},
        {"id": 19, "category": "Motivation et soft skills",      "difficulty": "easy",   "tip": "Sois authentique",                     "question": "Pourquoi postulez-vous à ce poste et qu'est-ce qui vous attire dans notre entreprise ?"},
        {"id": 20, "category": "Motivation et soft skills",      "difficulty": "easy",   "tip": "Montre ton esprit d'équipe",           "question": "Comment gérez-vous les conflits ou les désaccords dans une équipe de développement ?"},
    ]