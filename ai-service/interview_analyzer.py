import os
import httpx
import json

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

SPECIALTIES = {
    "java":    {"title": "Programmation Java",    "description": "Java Core, POO, Collections, Multithreading",        "level": "Intermédiaire", "duration": 45, "count": 15},
    "spring":  {"title": "Spring Boot",           "description": "REST API, Injection de dépendances, JPA",            "level": "Avancé",        "duration": 60, "count": 20},
    "angular": {"title": "Framework Angular",     "description": "Composants, Services, RxJS, State Management",       "level": "Intermédiaire", "duration": 45, "count": 15},
    "ml":      {"title": "IA / Machine Learning", "description": "Algorithmes ML, Réseaux de neurones, Deep Learning", "level": "Avancé",        "duration": 60, "count": 18},
    "devops":  {"title": "DevOps",                "description": "Docker, Kubernetes, CI/CD, Cloud",                   "level": "Intermédiaire", "duration": 45, "count": 15},
    "rh":      {"title": "Entretien RH",          "description": "Questions comportementales, Soft skills",            "level": "Débutant",      "duration": 30, "count": 10},
}

FALLBACK_QUESTIONS = {
    "java": [
        {"id":1,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Sois concis","question":"Parlez-moi de vous et de votre expérience en Java."},
        {"id":2,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Montre ta motivation","question":"Pourquoi avez-vous choisi Java comme langage principal ?"},
        {"id":3,"category":"Théorie","difficulty":"medium","time_suggested":90,"tip":"Cite les 4 piliers","question":"Expliquez les 4 piliers de la POO en Java."},
        {"id":4,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Donne des exemples","question":"Quelle est la différence entre une interface et une classe abstraite ?"},
        {"id":5,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Parle de synchronized","question":"Comment gérez-vous le multithreading en Java ?"},
        {"id":6,"category":"Technique","difficulty":"medium","time_suggested":120,"tip":"ArrayList vs LinkedList","question":"Expliquez les Collections en Java et quand utiliser ArrayList vs LinkedList."},
        {"id":7,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"try-with-resources","question":"Comment gérez-vous les exceptions en Java ? Types checked vs unchecked ?"},
        {"id":8,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Streams et lambdas","question":"Expliquez les nouveautés Java 8 : Stream API, lambdas, Optional."},
        {"id":9,"category":"Technique","difficulty":"medium","time_suggested":90,"tip":"equals et hashCode","question":"Pourquoi override equals() et hashCode() ensemble ?"},
        {"id":10,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Parle du GC","question":"Comment fonctionne la gestion mémoire et le Garbage Collector en Java ?"},
        {"id":11,"category":"Projets","difficulty":"medium","time_suggested":120,"tip":"Méthode STAR","question":"Décrivez votre projet Java le plus complexe."},
        {"id":12,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"Parle des défis","question":"Quel problème technique Java difficile avez-vous résolu ?"},
        {"id":13,"category":"Projets","difficulty":"easy","time_suggested":90,"tip":"Cite les outils","question":"Quels outils utilisez-vous pour tester votre code Java ?"},
        {"id":14,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Sois authentique","question":"Comment restez-vous à jour avec les nouvelles versions de Java ?"},
        {"id":15,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Esprit d'équipe","question":"Comment gérez-vous un désaccord technique avec un collègue ?"},
    ],
    "spring": [
        {"id":1,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Sois concis","question":"Présentez-vous et décrivez votre expérience avec Spring Boot."},
        {"id":2,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Montre ta motivation","question":"Pourquoi Spring Boot plutôt qu'un autre framework ?"},
        {"id":3,"category":"Théorie","difficulty":"medium","time_suggested":90,"tip":"IoC et DI","question":"Expliquez l'injection de dépendances et l'inversion de contrôle dans Spring."},
        {"id":4,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Parle des annotations","question":"Quelles sont les différences entre @Component, @Service, @Repository et @Controller ?"},
        {"id":5,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Parle des niveaux","question":"Comment fonctionne la gestion des transactions dans Spring ?"},
        {"id":6,"category":"Technique","difficulty":"medium","time_suggested":120,"tip":"REST best practices","question":"Comment structurez-vous une REST API avec Spring Boot ?"},
        {"id":7,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"JPA relations","question":"Comment gérez-vous les relations entre entités avec Spring Data JPA ?"},
        {"id":8,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"JWT ou OAuth","question":"Comment implémentez-vous la sécurité avec Spring Security ?"},
        {"id":9,"category":"Technique","difficulty":"medium","time_suggested":90,"tip":"application.yml","question":"Comment gérez-vous les configurations dans Spring Boot ?"},
        {"id":10,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Tests unitaires","question":"Comment testez-vous vos APIs Spring Boot ?"},
        {"id":11,"category":"Projets","difficulty":"medium","time_suggested":120,"tip":"Méthode STAR","question":"Décrivez votre projet Spring Boot le plus complexe."},
        {"id":12,"category":"Projets","difficulty":"hard","time_suggested":120,"tip":"Défis techniques","question":"Quel défi d'architecture avez-vous résolu avec Spring ?"},
        {"id":13,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"CI/CD","question":"Comment avez-vous déployé vos applications Spring Boot ?"},
        {"id":14,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Montre ta veille","question":"Comment vous tenez-vous à jour sur l'écosystème Spring ?"},
        {"id":15,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Travail d'équipe","question":"Décrivez votre expérience de travail en équipe sur un projet Spring Boot."},
        {"id":16,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Microservices","question":"Avez-vous travaillé avec une architecture microservices Spring Cloud ?"},
        {"id":17,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Lazy vs Eager","question":"Quelle est la différence entre FetchType.LAZY et EAGER en JPA ?"},
        {"id":18,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"Performance","question":"Comment avez-vous optimisé les performances de vos APIs ?"},
        {"id":19,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Soft skills","question":"Comment gérez-vous les deadlines serrées dans un projet ?"},
        {"id":20,"category":"Technique","difficulty":"medium","time_suggested":90,"tip":"Monitoring","question":"Quels outils utilisez-vous pour monitorer vos applications Spring ?"},
    ],
    "angular": [
        {"id":1,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Sois concis","question":"Présentez-vous et parlez de votre expérience Angular."},
        {"id":2,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Motivation","question":"Pourquoi Angular plutôt que React ou Vue ?"},
        {"id":3,"category":"Théorie","difficulty":"medium","time_suggested":90,"tip":"Architecture","question":"Expliquez l'architecture d'une application Angular (modules, composants, services)."},
        {"id":4,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"RxJS","question":"Quelle est la différence entre Observable et Promise en Angular ?"},
        {"id":5,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Change Detection","question":"Comment fonctionne le Change Detection dans Angular ?"},
        {"id":6,"category":"Technique","difficulty":"medium","time_suggested":120,"tip":"Lifecycle hooks","question":"Expliquez les lifecycle hooks Angular et quand les utiliser."},
        {"id":7,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Lazy loading","question":"Comment implémentez-vous le lazy loading dans Angular ?"},
        {"id":8,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Guards","question":"Comment protégez-vous vos routes avec Angular Guards ?"},
        {"id":9,"category":"Technique","difficulty":"medium","time_suggested":90,"tip":"Forms","question":"Quelle est la différence entre Template-driven et Reactive Forms ?"},
        {"id":10,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"State management","question":"Comment gérez-vous l'état dans une application Angular complexe ?"},
        {"id":11,"category":"Projets","difficulty":"medium","time_suggested":120,"tip":"Méthode STAR","question":"Décrivez votre projet Angular le plus complexe."},
        {"id":12,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"Performance","question":"Comment avez-vous optimisé les performances d'une app Angular ?"},
        {"id":13,"category":"Projets","difficulty":"easy","time_suggested":90,"tip":"Testing","question":"Comment testez-vous vos composants Angular ?"},
        {"id":14,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Veille","question":"Comment suivez-vous les nouvelles versions d'Angular ?"},
        {"id":15,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Collaboration","question":"Comment collaborez-vous avec les développeurs backend ?"},
    ],
    "devops": [
        {"id":1,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Sois concis","question":"Présentez-vous et décrivez votre expérience DevOps."},
        {"id":2,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Motivation","question":"Pourquoi avez-vous choisi le DevOps ?"},
        {"id":3,"category":"Théorie","difficulty":"medium","time_suggested":90,"tip":"Concepts","question":"Expliquez les principes du CI/CD et leur importance."},
        {"id":4,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Docker vs VM","question":"Quelle est la différence entre Docker et une machine virtuelle ?"},
        {"id":5,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Orchestration","question":"Comment fonctionne Kubernetes ? Expliquez Pods, Services, Deployments."},
        {"id":6,"category":"Technique","difficulty":"medium","time_suggested":120,"tip":"Dockerfile","question":"Comment créez-vous et optimisez-vous un Dockerfile ?"},
        {"id":7,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Pipeline","question":"Décrivez un pipeline CI/CD que vous avez mis en place."},
        {"id":8,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Cloud","question":"Avec quels services cloud avez-vous travaillé ? (AWS/Azure/GCP)"},
        {"id":9,"category":"Technique","difficulty":"medium","time_suggested":90,"tip":"Monitoring","question":"Quels outils de monitoring et logging utilisez-vous ?"},
        {"id":10,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"IaC","question":"Avez-vous utilisé Terraform ou Ansible pour l'Infrastructure as Code ?"},
        {"id":11,"category":"Projets","difficulty":"medium","time_suggested":120,"tip":"STAR","question":"Décrivez un projet DevOps dont vous êtes le plus fier."},
        {"id":12,"category":"Projets","difficulty":"hard","time_suggested":120,"tip":"Incident","question":"Comment avez-vous géré un incident de production ?"},
        {"id":13,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"Sécurité","question":"Comment gérez-vous la sécurité dans vos pipelines ?"},
        {"id":14,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Veille","question":"Comment restez-vous à jour sur les outils DevOps ?"},
        {"id":15,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Collaboration","question":"Comment collaborez-vous avec les équipes Dev et Ops ?"},
    ],
    "ml": [
        {"id":1,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Sois concis","question":"Présentez-vous et décrivez votre expérience en ML/IA."},
        {"id":2,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Motivation","question":"Pourquoi avez-vous choisi le Machine Learning ?"},
        {"id":3,"category":"Théorie","difficulty":"medium","time_suggested":90,"tip":"Définitions","question":"Quelle est la différence entre supervised, unsupervised et reinforcement learning ?"},
        {"id":4,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Bias-Variance","question":"Expliquez le tradeoff biais-variance et l'overfitting."},
        {"id":5,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"Algorithmes","question":"Comparez Random Forest, SVM et Neural Networks."},
        {"id":6,"category":"Technique","difficulty":"medium","time_suggested":120,"tip":"Preprocessing","question":"Comment préparez-vous et nettoyez-vous un dataset ?"},
        {"id":7,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Metrics","question":"Quelles métriques utilisez-vous pour évaluer un modèle de classification ?"},
        {"id":8,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Deep Learning","question":"Expliquez comment fonctionne un réseau de neurones."},
        {"id":9,"category":"Technique","difficulty":"medium","time_suggested":90,"tip":"Tools","question":"Quels frameworks ML utilisez-vous ? (sklearn, PyTorch, TensorFlow)"},
        {"id":10,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Deployment","question":"Comment déployez-vous un modèle ML en production ?"},
        {"id":11,"category":"Projets","difficulty":"medium","time_suggested":120,"tip":"STAR","question":"Décrivez votre projet ML le plus impactant."},
        {"id":12,"category":"Projets","difficulty":"hard","time_suggested":120,"tip":"Challenges","question":"Quel problème de données difficile avez-vous résolu ?"},
        {"id":13,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"MLOps","question":"Comment gérez-vous le versioning de vos modèles ?"},
        {"id":14,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Veille","question":"Comment suivez-vous les avancées en IA/ML ?"},
        {"id":15,"category":"Comportemental","difficulty":"easy","time_suggested":60,"tip":"Éthique","question":"Comment abordez-vous l'éthique dans vos projets ML ?"},
        {"id":16,"category":"Théorie","difficulty":"hard","time_suggested":90,"tip":"NLP","question":"Expliquez comment fonctionnent les transformers et l'attention mechanism."},
        {"id":17,"category":"Technique","difficulty":"hard","time_suggested":120,"tip":"Optimization","question":"Comment optimisez-vous les hyperparamètres d'un modèle ?"},
        {"id":18,"category":"Projets","difficulty":"medium","time_suggested":90,"tip":"Data pipeline","question":"Comment construisez-vous un pipeline de données robuste ?"},
    ],
    "rh": [
        {"id":1,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Sois concis et structuré","question":"Pouvez-vous vous présenter en 2-3 minutes ?"},
        {"id":2,"category":"Présentation","difficulty":"easy","time_suggested":60,"tip":"Montre ta motivation","question":"Pourquoi postulez-vous à ce poste ?"},
        {"id":3,"category":"Soft skills","difficulty":"easy","time_suggested":90,"tip":"Méthode STAR","question":"Décrivez une situation où vous avez dû travailler sous pression."},
        {"id":4,"category":"Soft skills","difficulty":"easy","time_suggested":90,"tip":"Exemple concret","question":"Comment gérez-vous les conflits au sein d'une équipe ?"},
        {"id":5,"category":"Soft skills","difficulty":"easy","time_suggested":90,"tip":"Sois honnête","question":"Quelles sont vos principales forces et faiblesses ?"},
        {"id":6,"category":"Motivation","difficulty":"easy","time_suggested":60,"tip":"Recherche sur l'entreprise","question":"Que savez-vous de notre entreprise et pourquoi voulez-vous nous rejoindre ?"},
        {"id":7,"category":"Soft skills","difficulty":"medium","time_suggested":90,"tip":"Leadership","question":"Décrivez une situation où vous avez pris une initiative."},
        {"id":8,"category":"Soft skills","difficulty":"easy","time_suggested":60,"tip":"Adaptabilité","question":"Comment vous adaptez-vous aux changements dans un environnement de travail ?"},
        {"id":9,"category":"Motivation","difficulty":"easy","time_suggested":60,"tip":"Objectifs clairs","question":"Où vous voyez-vous dans 5 ans ?"},
        {"id":10,"category":"Soft skills","difficulty":"easy","time_suggested":60,"tip":"Sois authentique","question":"Qu'est-ce qui vous motive le plus dans votre travail quotidien ?"},
    ]
}


def get_specialties() -> list:
    return [{"id": k, **v} for k, v in SPECIALTIES.items()]


def generate_questions_by_specialty(specialty_id: str) -> list:
    spec = SPECIALTIES.get(specialty_id)
    if not spec:
        return FALLBACK_QUESTIONS.get("java", [])

    prompt = f"""Tu es un recruteur expert. Génère exactement {spec['count']} questions d'entretien pour :
Spécialité : {spec['title']}
Description : {spec['description']}
Niveau : {spec['level']}

Réponds UNIQUEMENT en JSON valide sans markdown ni backticks :
[
  {{
    "id": 1,
    "category": "Présentation",
    "question": "...",
    "difficulty": "easy",
    "tip": "conseil court",
    "time_suggested": 60
  }}
]"""

    try:
        response = httpx.post(
            f"{GEMINI_URL}?key={GEMINI_API_KEY}",
            json={"contents": [{"parts": [{"text": prompt}]}]},
            timeout=60
        )
        data = response.json()
        raw = data["candidates"][0]["content"]["parts"][0]["text"].strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"): raw = raw[4:]
        return json.loads(raw.strip())
    except:
        return FALLBACK_QUESTIONS.get(specialty_id, FALLBACK_QUESTIONS["java"])


def analyze_voice_response(transcript: str, question: str, specialty: str) -> dict:
    if not GEMINI_API_KEY:
        return {
            "score_technique": 70, "score_communication": 72,
            "score_confiance": 68, "score_global": 70,
            "niveau": "Bien", "points_forts": "Réponse structurée",
            "points_ameliorer": "Donnez plus d'exemples concrets",
            "reponse_ideale": "Une réponse avec des exemples pratiques et concrets."
        }

    prompt = f"""Évalue cette réponse d'entretien :
Question : {question}
Spécialité : {specialty}
Transcription : {transcript}

Réponds UNIQUEMENT en JSON :
{{
  "score_technique": <0-100>,
  "score_communication": <0-100>,
  "score_confiance": <0-100>,
  "score_global": <0-100>,
  "niveau": "<Excellent|Bien|Moyen|Insuffisant>",
  "points_forts": "...",
  "points_ameliorer": "...",
  "reponse_ideale": "..."
}}"""

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
    except:
        return {
            "score_technique": 65, "score_communication": 70,
            "score_confiance": 68, "score_global": 68,
            "niveau": "Bien", "points_forts": "Réponse reçue et analysée",
            "points_ameliorer": "Développez davantage avec des exemples",
            "reponse_ideale": "Une réponse structurée avec la méthode STAR."
        }


def generate_final_feedback(specialty: str, answers: list, avg_scores: dict) -> dict:
    if not GEMINI_API_KEY:
        score = avg_scores.get("global", 70)
        return {
            "titre": "Excellent travail ! 🎉" if score >= 75 else "Bon effort !",
            "message_global": "Votre performance a été analysée par notre IA.",
            "points_forts": ["Bonne maîtrise technique", "Communication claire", "Réponses structurées"],
            "axes_amelioration": ["Approfondissez les concepts avancés", "Donnez plus d'exemples concrets"],
            "conseil_final": "Continuez à pratiquer régulièrement !",
            "recommandation": "En bonne voie" if score >= 60 else "Préparation supplémentaire recommandée"
        }

    prompt = f"""Génère un rapport final d'entretien :
Spécialité : {specialty}
Score technique : {avg_scores.get('technique', 0)}%
Score communication : {avg_scores.get('communication', 0)}%
Score confiance : {avg_scores.get('confiance', 0)}%
Questions répondues : {len(answers)}

Réponds UNIQUEMENT en JSON :
{{
  "titre": "...",
  "message_global": "...",
  "points_forts": ["...", "...", "..."],
  "axes_amelioration": ["...", "...", "..."],
  "conseil_final": "...",
  "recommandation": "<Prêt pour l'entretien|En bonne voie|Préparation supplémentaire recommandée>"
}}"""

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
    except:
        return {
            "titre": "Analyse complète !",
            "message_global": "Votre entretien a été analysé.",
            "points_forts": ["Bonne participation", "Réponses cohérentes"],
            "axes_amelioration": ["Plus d'exemples concrets"],
            "conseil_final": "Pratiquez régulièrement !",
            "recommandation": "En bonne voie"
        }