import os
import httpx
import json
import random
import time
from threading import Lock

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

# ─────────────────────────────────────────────
# Cache anti-répétition : évite d'appeler Gemini
# à chaque session et mélange les questions
# ─────────────────────────────────────────────
_question_cache: dict = {}   # { specialty_id: { "pool": [...], "expires": float } }
_cache_lock = Lock()
CACHE_TTL = 1800  # 30 minutes

SPECIALTIES = {
    "java":    {"title": "Programmation Java",    "description": "Java Core, POO, Collections, Multithreading",        "level": "Intermédiaire", "duration": 45, "count": 15},
    "spring":  {"title": "Spring Boot",           "description": "REST API, Injection de dépendances, JPA",            "level": "Avancé",        "duration": 60, "count": 20},
    "angular": {"title": "Framework Angular",     "description": "Composants, Services, RxJS, State Management",       "level": "Intermédiaire", "duration": 45, "count": 15},
    "ml":      {"title": "IA / Machine Learning", "description": "Algorithmes ML, Réseaux de neurones, Deep Learning", "level": "Avancé",        "duration": 60, "count": 18},
    "devops":  {"title": "DevOps",                "description": "Docker, Kubernetes, CI/CD, Cloud",                   "level": "Intermédiaire", "duration": 45, "count": 15},
    "rh":      {"title": "Entretien RH",          "description": "Questions comportementales, Soft skills",            "level": "Débutant",      "duration": 30, "count": 10},
}

# ─────────────────────────────────────────────
# POOL ÉTENDU — 3× plus de questions que nécessaire
# pour garantir la variété entre les sessions
# ─────────────────────────────────────────────
QUESTION_POOL = {
    "java": [
        {"id":1,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Sois concis",              "question":"Parlez-moi de vous et de votre expérience en Java."},
        {"id":2,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Montre ta motivation",     "question":"Pourquoi avez-vous choisi Java comme langage principal ?"},
        {"id":3,  "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"Cite les 4 piliers",       "question":"Expliquez les 4 piliers de la POO en Java."},
        {"id":4,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Donne des exemples",       "question":"Quelle est la différence entre une interface et une classe abstraite ?"},
        {"id":5,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"synchronized",             "question":"Comment gérez-vous le multithreading en Java ?"},
        {"id":6,  "category":"Technique",      "difficulty":"medium", "time_suggested":120, "tip":"ArrayList vs LinkedList",  "question":"Expliquez les Collections en Java et quand utiliser ArrayList vs LinkedList."},
        {"id":7,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"try-with-resources",       "question":"Comment gérez-vous les exceptions en Java ? Types checked vs unchecked ?"},
        {"id":8,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Streams et lambdas",       "question":"Expliquez les nouveautés Java 8 : Stream API, lambdas, Optional."},
        {"id":9,  "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"equals et hashCode",       "question":"Pourquoi override equals() et hashCode() ensemble ?"},
        {"id":10, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Parle du GC",              "question":"Comment fonctionne la gestion mémoire et le Garbage Collector en Java ?"},
        {"id":11, "category":"Projets",        "difficulty":"medium", "time_suggested":120, "tip":"Méthode STAR",             "question":"Décrivez votre projet Java le plus complexe."},
        {"id":12, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Parle des défis",          "question":"Quel problème technique Java difficile avez-vous résolu ?"},
        {"id":13, "category":"Projets",        "difficulty":"easy",   "time_suggested":90,  "tip":"Cite les outils",          "question":"Quels outils utilisez-vous pour tester votre code Java ?"},
        {"id":14, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Sois authentique",         "question":"Comment restez-vous à jour avec les nouvelles versions de Java ?"},
        {"id":15, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Esprit d'équipe",          "question":"Comment gérez-vous un désaccord technique avec un collègue ?"},
        {"id":16, "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"JVM interne",              "question":"Comment fonctionne la JVM ? Compilation vs interprétation bytecode."},
        {"id":17, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Design patterns",          "question":"Quels design patterns avez-vous utilisé en Java ? Donnez des exemples concrets."},
        {"id":18, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Immutabilité",             "question":"Qu'est-ce qu'un objet immutable en Java et pourquoi l'utiliser ?"},
        {"id":19, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"== vs equals",             "question":"Quelle est la différence entre == et equals() en Java ? Donnez un exemple où ça poserait problème."},
        {"id":20, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Async",                    "question":"Expliquez CompletableFuture et la programmation asynchrone en Java."},
        {"id":21, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Clean code",               "question":"Comment organisez-vous votre code Java pour le rendre maintenable ?"},
        {"id":22, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Curiosité",                "question":"Quel concept Java avez-vous appris récemment qui vous a surpris ?"},
        {"id":23, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Generics",                 "question":"Expliquez les Generics en Java et donnez un exemple d'utilisation avancée."},
        {"id":24, "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"Énumérations",             "question":"Quelles sont les différences entre une enum et une constante static final en Java ?"},
        {"id":25, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Réflexion",                "question":"Qu'est-ce que la réflexion (Reflection) en Java ? Quand l'utiliser ?"},
        {"id":26, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Architecture",             "question":"Décrivez comment vous avez conçu l'architecture d'un projet Java complexe."},
        {"id":27, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Méthodologie",             "question":"Quelle est votre approche quand vous tombez sur un bug difficile à reproduire ?"},
        {"id":28, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Serialisation",            "question":"Comment fonctionne la sérialisation en Java ? Quels sont ses risques ?"},
        {"id":29, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Méthodes fonctionnelles",  "question":"Quelle est la différence entre map(), flatMap() et filter() dans les Streams Java ?"},
        {"id":30, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Performance",              "question":"Comment profilez-vous et optimisez-vous une application Java lente ?"},
    ],
    "spring": [
        {"id":1,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Sois concis",              "question":"Présentez-vous et décrivez votre expérience avec Spring Boot."},
        {"id":2,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Motivation",               "question":"Pourquoi Spring Boot plutôt qu'un autre framework ?"},
        {"id":3,  "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"IoC et DI",                "question":"Expliquez l'injection de dépendances et l'inversion de contrôle dans Spring."},
        {"id":4,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Annotations",              "question":"Différences entre @Component, @Service, @Repository et @Controller ?"},
        {"id":5,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Transactions",             "question":"Comment fonctionne la gestion des transactions dans Spring ?"},
        {"id":6,  "category":"Technique",      "difficulty":"medium", "time_suggested":120, "tip":"REST best practices",      "question":"Comment structurez-vous une REST API avec Spring Boot ?"},
        {"id":7,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"JPA relations",            "question":"Comment gérez-vous les relations entre entités avec Spring Data JPA ?"},
        {"id":8,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"JWT ou OAuth",             "question":"Comment implémentez-vous la sécurité avec Spring Security ?"},
        {"id":9,  "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"application.yml",          "question":"Comment gérez-vous les configurations dans Spring Boot ?"},
        {"id":10, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Tests unitaires",          "question":"Comment testez-vous vos APIs Spring Boot ?"},
        {"id":11, "category":"Projets",        "difficulty":"medium", "time_suggested":120, "tip":"Méthode STAR",             "question":"Décrivez votre projet Spring Boot le plus complexe."},
        {"id":12, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Architecture",             "question":"Quel défi d'architecture avez-vous résolu avec Spring ?"},
        {"id":13, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"CI/CD",                    "question":"Comment avez-vous déployé vos applications Spring Boot ?"},
        {"id":14, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Veille",                   "question":"Comment vous tenez-vous à jour sur l'écosystème Spring ?"},
        {"id":15, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Travail d'équipe",         "question":"Décrivez votre expérience de travail en équipe sur un projet Spring Boot."},
        {"id":16, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Microservices",            "question":"Avez-vous travaillé avec une architecture microservices Spring Cloud ?"},
        {"id":17, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Lazy vs Eager",            "question":"Différence entre FetchType.LAZY et EAGER en JPA ?"},
        {"id":18, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Performance",              "question":"Comment avez-vous optimisé les performances de vos APIs ?"},
        {"id":19, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Soft skills",              "question":"Comment gérez-vous les deadlines serrées dans un projet ?"},
        {"id":20, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Monitoring",               "question":"Quels outils utilisez-vous pour monitorer vos applications Spring ?"},
        {"id":21, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Scopes",                   "question":"Expliquez les différents scopes des beans Spring : Singleton, Prototype, Request, Session."},
        {"id":22, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Cache",                    "question":"Comment implémentez-vous le caching avec Spring Cache et @Cacheable ?"},
        {"id":23, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Validation",               "question":"Comment gérez-vous la validation des entrées avec Bean Validation en Spring ?"},
        {"id":24, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Async",                    "question":"Comment utilisez-vous @Async et les futures dans Spring Boot ?"},
        {"id":25, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Incident",                 "question":"Décrivez un bug en production Spring Boot que vous avez résolu sous pression."},
        {"id":26, "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"AOP",                      "question":"Qu'est-ce que l'AOP (Aspect Oriented Programming) dans Spring ? Donnez un exemple."},
        {"id":27, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"WebSocket",                "question":"Comment implémentez-vous des WebSockets avec Spring Boot ?"},
        {"id":28, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Pagination",               "question":"Comment gérez-vous la pagination et le tri avec Spring Data ?"},
        {"id":29, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Code review",              "question":"Comment abordez-vous les code reviews dans votre équipe ?"},
        {"id":30, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"N+1",                      "question":"Expliquez le problème N+1 en JPA et comment le résoudre avec @EntityGraph ou JOIN FETCH."},
    ],
    "angular": [
        {"id":1,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Sois concis",              "question":"Présentez-vous et parlez de votre expérience Angular."},
        {"id":2,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Motivation",               "question":"Pourquoi Angular plutôt que React ou Vue ?"},
        {"id":3,  "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"Architecture",             "question":"Expliquez l'architecture d'une application Angular (modules, composants, services)."},
        {"id":4,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"RxJS",                     "question":"Différence entre Observable et Promise en Angular ?"},
        {"id":5,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Change Detection",         "question":"Comment fonctionne le Change Detection dans Angular ?"},
        {"id":6,  "category":"Technique",      "difficulty":"medium", "time_suggested":120, "tip":"Lifecycle hooks",          "question":"Expliquez les lifecycle hooks Angular et quand les utiliser."},
        {"id":7,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Lazy loading",             "question":"Comment implémentez-vous le lazy loading dans Angular ?"},
        {"id":8,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Guards",                   "question":"Comment protégez-vous vos routes avec Angular Guards ?"},
        {"id":9,  "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Forms",                    "question":"Différence entre Template-driven et Reactive Forms ?"},
        {"id":10, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"State management",         "question":"Comment gérez-vous l'état dans une application Angular complexe ?"},
        {"id":11, "category":"Projets",        "difficulty":"medium", "time_suggested":120, "tip":"Méthode STAR",             "question":"Décrivez votre projet Angular le plus complexe."},
        {"id":12, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Performance",              "question":"Comment avez-vous optimisé les performances d'une app Angular ?"},
        {"id":13, "category":"Projets",        "difficulty":"easy",   "time_suggested":90,  "tip":"Testing",                  "question":"Comment testez-vous vos composants Angular ?"},
        {"id":14, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Veille",                   "question":"Comment suivez-vous les nouvelles versions d'Angular ?"},
        {"id":15, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Collaboration",            "question":"Comment collaborez-vous avec les développeurs backend ?"},
        {"id":16, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Directives",               "question":"Différence entre une directive structurelle et une directive d'attribut en Angular ?"},
        {"id":17, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Interceptors",             "question":"Comment utilisez-vous les HTTP Interceptors dans Angular pour gérer les tokens ?"},
        {"id":18, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Pipes",                    "question":"Qu'est-ce qu'un Pipe Angular ? Créez un exemple de pipe personnalisé."},
        {"id":19, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Standalone",               "question":"Qu'est-ce que les Standalone Components en Angular 17+ ? Avantages ?"},
        {"id":20, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Signal",                   "question":"Expliquez les Signals en Angular 17+ et comment ils remplacent certains usages de RxJS."},
        {"id":21, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Architecture",             "question":"Comment avez-vous structuré les modules d'un grand projet Angular ?"},
        {"id":22, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"i18n",                     "question":"Comment gérez-vous l'internationalisation (i18n) dans Angular ?"},
        {"id":23, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Débogage",                 "question":"Quelle est votre approche quand un composant Angular ne se met pas à jour ?"},
        {"id":24, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"SSR",                      "question":"Qu'est-ce que Angular Universal ? Pourquoi utiliser le Server-Side Rendering ?"},
        {"id":25, "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"DI Tokens",                "question":"Expliquez le système d'injection de dépendances d'Angular et les InjectionToken."},
        {"id":26, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Refacto",                  "question":"Avez-vous migré une application AngularJS ou Angular vers une version plus récente ?"},
        {"id":27, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"WebWorker",                "question":"Quand et comment utiliseriez-vous des Web Workers dans Angular ?"},
        {"id":28, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"UX",                      "question":"Comment prenez-vous en compte l'accessibilité dans vos développements Angular ?"},
        {"id":29, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"OnPush",                   "question":"Expliquez la stratégie ChangeDetectionStrategy.OnPush et quand l'utiliser."},
        {"id":30, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Resolver",                 "question":"À quoi sert un Route Resolver en Angular ? Donnez un exemple d'utilisation."},
    ],
    "ml": [
        {"id":1,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Sois concis",              "question":"Présentez-vous et décrivez votre expérience en ML/IA."},
        {"id":2,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Motivation",               "question":"Pourquoi avez-vous choisi le Machine Learning ?"},
        {"id":3,  "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"Définitions",              "question":"Différence entre supervised, unsupervised et reinforcement learning ?"},
        {"id":4,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Bias-Variance",            "question":"Expliquez le tradeoff biais-variance et l'overfitting."},
        {"id":5,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Algorithmes",              "question":"Comparez Random Forest, SVM et Neural Networks."},
        {"id":6,  "category":"Technique",      "difficulty":"medium", "time_suggested":120, "tip":"Preprocessing",            "question":"Comment préparez-vous et nettoyez-vous un dataset ?"},
        {"id":7,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Metrics",                  "question":"Quelles métriques utilisez-vous pour évaluer un modèle de classification ?"},
        {"id":8,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Deep Learning",            "question":"Expliquez comment fonctionne un réseau de neurones."},
        {"id":9,  "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Tools",                    "question":"Quels frameworks ML utilisez-vous ? (sklearn, PyTorch, TensorFlow)"},
        {"id":10, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Deployment",               "question":"Comment déployez-vous un modèle ML en production ?"},
        {"id":11, "category":"Projets",        "difficulty":"medium", "time_suggested":120, "tip":"STAR",                     "question":"Décrivez votre projet ML le plus impactant."},
        {"id":12, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Challenges",               "question":"Quel problème de données difficile avez-vous résolu ?"},
        {"id":13, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"MLOps",                    "question":"Comment gérez-vous le versioning de vos modèles ?"},
        {"id":14, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Veille",                   "question":"Comment suivez-vous les avancées en IA/ML ?"},
        {"id":15, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Éthique",                  "question":"Comment abordez-vous l'éthique dans vos projets ML ?"},
        {"id":16, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"NLP",                      "question":"Expliquez comment fonctionnent les transformers et l'attention mechanism."},
        {"id":17, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Hyperparams",              "question":"Comment optimisez-vous les hyperparamètres d'un modèle ?"},
        {"id":18, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Data pipeline",            "question":"Comment construisez-vous un pipeline de données robuste ?"},
        {"id":19, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"CNN",                      "question":"Expliquez l'architecture d'un CNN et pourquoi les convolutions fonctionnent pour les images."},
        {"id":20, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Transfer learning",        "question":"Qu'est-ce que le transfer learning ? Donnez un exemple concret."},
        {"id":21, "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"Regularization",           "question":"Expliquez L1 vs L2 regularization et quand choisir l'une ou l'autre."},
        {"id":22, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Feature engineering",      "question":"Quelles techniques de feature engineering avez-vous utilisées dans vos projets ?"},
        {"id":23, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Production",               "question":"Quels défis avez-vous rencontrés en déployant un modèle ML en production ?"},
        {"id":24, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"GAN",                      "question":"Expliquez le principe des GANs (Generative Adversarial Networks)."},
        {"id":25, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Evaluation",               "question":"Différence entre precision, recall, F1-score et AUC-ROC ?"},
        {"id":26, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Collaboration",            "question":"Comment expliquez-vous un modèle ML à un non-technicien ?"},
        {"id":27, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Monitoring",               "question":"Comment monitorer un modèle ML en production pour détecter le data drift ?"},
        {"id":28, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"RLHF",                     "question":"Qu'est-ce que le RLHF (Reinforcement Learning from Human Feedback) ? Pourquoi les LLMs l'utilisent ?"},
        {"id":29, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"RAG",                      "question":"Expliquez l'architecture RAG (Retrieval Augmented Generation) et ses avantages."},
        {"id":30, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Biais",                    "question":"Comment détectez-vous et corrigez-vous les biais dans vos datasets ?"},
    ],
    "devops": [
        {"id":1,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Sois concis",              "question":"Présentez-vous et décrivez votre expérience DevOps."},
        {"id":2,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Motivation",               "question":"Pourquoi avez-vous choisi le DevOps ?"},
        {"id":3,  "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"CI/CD",                    "question":"Expliquez les principes du CI/CD et leur importance."},
        {"id":4,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"Docker vs VM",             "question":"Différence entre Docker et une machine virtuelle ?"},
        {"id":5,  "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"K8s",                      "question":"Comment fonctionne Kubernetes ? Expliquez Pods, Services, Deployments."},
        {"id":6,  "category":"Technique",      "difficulty":"medium", "time_suggested":120, "tip":"Dockerfile",               "question":"Comment créez-vous et optimisez-vous un Dockerfile ?"},
        {"id":7,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Pipeline",                 "question":"Décrivez un pipeline CI/CD que vous avez mis en place."},
        {"id":8,  "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Cloud",                    "question":"Avec quels services cloud avez-vous travaillé ? (AWS/Azure/GCP)"},
        {"id":9,  "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Monitoring",               "question":"Quels outils de monitoring et logging utilisez-vous ?"},
        {"id":10, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"IaC",                      "question":"Avez-vous utilisé Terraform ou Ansible pour l'Infrastructure as Code ?"},
        {"id":11, "category":"Projets",        "difficulty":"medium", "time_suggested":120, "tip":"STAR",                     "question":"Décrivez un projet DevOps dont vous êtes le plus fier."},
        {"id":12, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Incident",                 "question":"Comment avez-vous géré un incident de production ?"},
        {"id":13, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Sécurité",                 "question":"Comment gérez-vous la sécurité dans vos pipelines ?"},
        {"id":14, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Veille",                   "question":"Comment restez-vous à jour sur les outils DevOps ?"},
        {"id":15, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Collaboration",            "question":"Comment collaborez-vous avec les équipes Dev et Ops ?"},
        {"id":16, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Helm",                     "question":"Qu'est-ce que Helm ? Comment gérez-vous les déploiements Kubernetes avec Helm Charts ?"},
        {"id":17, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"GitOps",                   "question":"Expliquez le principe GitOps. Comment ArgoCD ou Flux fonctionnent ?"},
        {"id":18, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Docker Compose",           "question":"Différence entre Docker Compose et Kubernetes ? Quand utiliser l'un ou l'autre ?"},
        {"id":19, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Zero-downtime",            "question":"Comment mettez-vous en place un déploiement zero-downtime (blue/green ou canary) ?"},
        {"id":20, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Secrets",                  "question":"Comment gérez-vous les secrets et variables sensibles dans vos pipelines CI/CD ?"},
        {"id":21, "category":"Théorie",        "difficulty":"medium", "time_suggested":90,  "tip":"SRE",                      "question":"Quelle est la différence entre DevOps et SRE (Site Reliability Engineering) ?"},
        {"id":22, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Service mesh",             "question":"Qu'est-ce qu'un service mesh (Istio, Linkerd) ? Dans quel cas l'utiliser ?"},
        {"id":23, "category":"Projets",        "difficulty":"medium", "time_suggested":90,  "tip":"Rollback",                 "question":"Comment gérez-vous les rollbacks en cas de déploiement raté ?"},
        {"id":24, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"Postmortem",               "question":"Comment rédigez-vous un postmortem après un incident de production ?"},
        {"id":25, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Observabilité",            "question":"Expliquez les 3 piliers de l'observabilité : logs, métriques, traces."},
        {"id":26, "category":"Théorie",        "difficulty":"hard",   "time_suggested":90,  "tip":"FinOps",                   "question":"Comment optimisez-vous les coûts cloud dans un environnement multi-services ?"},
        {"id":27, "category":"Technique",      "difficulty":"hard",   "time_suggested":120, "tip":"Multi-cloud",              "question":"Comment gérez-vous une infrastructure multi-cloud ? Quels défis cela pose ?"},
        {"id":28, "category":"Projets",        "difficulty":"hard",   "time_suggested":120, "tip":"Migration",                "question":"Avez-vous migré une application monolithique vers des microservices ? Comment ?"},
        {"id":29, "category":"Comportemental", "difficulty":"easy",   "time_suggested":60,  "tip":"On-call",                  "question":"Comment gérez-vous une alerte critique à 3h du matin ?"},
        {"id":30, "category":"Technique",      "difficulty":"medium", "time_suggested":90,  "tip":"Registry",                 "question":"Comment gérez-vous un registry Docker privé et la sécurisation des images ?"},
    ],
    "rh": [
        {"id":1,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Concis et structuré",      "question":"Pouvez-vous vous présenter en 2-3 minutes ?"},
        {"id":2,  "category":"Présentation",   "difficulty":"easy",   "time_suggested":60,  "tip":"Motivation",               "question":"Pourquoi postulez-vous à ce poste ?"},
        {"id":3,  "category":"Soft skills",    "difficulty":"easy",   "time_suggested":90,  "tip":"Méthode STAR",             "question":"Décrivez une situation où vous avez dû travailler sous pression."},
        {"id":4,  "category":"Soft skills",    "difficulty":"easy",   "time_suggested":90,  "tip":"Exemple concret",          "question":"Comment gérez-vous les conflits au sein d'une équipe ?"},
        {"id":5,  "category":"Soft skills",    "difficulty":"easy",   "time_suggested":90,  "tip":"Sois honnête",             "question":"Quelles sont vos principales forces et faiblesses ?"},
        {"id":6,  "category":"Motivation",     "difficulty":"easy",   "time_suggested":60,  "tip":"Recherche l'entreprise",   "question":"Que savez-vous de notre entreprise et pourquoi voulez-vous nous rejoindre ?"},
        {"id":7,  "category":"Soft skills",    "difficulty":"medium", "time_suggested":90,  "tip":"Leadership",               "question":"Décrivez une situation où vous avez pris une initiative."},
        {"id":8,  "category":"Soft skills",    "difficulty":"easy",   "time_suggested":60,  "tip":"Adaptabilité",             "question":"Comment vous adaptez-vous aux changements dans un environnement de travail ?"},
        {"id":9,  "category":"Motivation",     "difficulty":"easy",   "time_suggested":60,  "tip":"Objectifs clairs",         "question":"Où vous voyez-vous dans 5 ans ?"},
        {"id":10, "category":"Soft skills",    "difficulty":"easy",   "time_suggested":60,  "tip":"Authenticité",             "question":"Qu'est-ce qui vous motive le plus dans votre travail quotidien ?"},
        {"id":11, "category":"Soft skills",    "difficulty":"medium", "time_suggested":90,  "tip":"Gestion du temps",         "question":"Comment priorisez-vous vos tâches quand tout est urgent ?"},
        {"id":12, "category":"Soft skills",    "difficulty":"easy",   "time_suggested":60,  "tip":"Communication",            "question":"Comment communiquez-vous une mauvaise nouvelle à votre équipe ?"},
        {"id":13, "category":"Motivation",     "difficulty":"easy",   "time_suggested":60,  "tip":"Croissance",               "question":"Qu'est-ce qui vous a poussé à choisir la technologie comme domaine ?"},
        {"id":14, "category":"Soft skills",    "difficulty":"medium", "time_suggested":90,  "tip":"Gestion erreur",           "question":"Parlez-moi d'une erreur professionnelle et ce que vous en avez appris."},
        {"id":15, "category":"Soft skills",    "difficulty":"easy",   "time_suggested":60,  "tip":"Autonomie",                "question":"Préférez-vous travailler en équipe ou seul ? Pourquoi ?"},
        {"id":16, "category":"Motivation",     "difficulty":"easy",   "time_suggested":60,  "tip":"Salaire",                  "question":"Quelles sont vos prétentions salariales et pourquoi ?"},
        {"id":17, "category":"Soft skills",    "difficulty":"medium", "time_suggested":90,  "tip":"Feedback",                 "question":"Comment réagissez-vous face à une critique de votre travail ?"},
        {"id":18, "category":"Soft skills",    "difficulty":"easy",   "time_suggested":60,  "tip":"Télétravail",              "question":"Comment gérez-vous votre productivité en télétravail ?"},
        {"id":19, "category":"Motivation",     "difficulty":"easy",   "time_suggested":60,  "tip":"Disponibilité",            "question":"Quelle est votre disponibilité et avez-vous une période de préavis ?"},
        {"id":20, "category":"Soft skills",    "difficulty":"medium", "time_suggested":90,  "tip":"Innovation",               "question":"Donnez un exemple où vous avez proposé une idée qui a amélioré un processus."},
    ],
}


def get_specialties() -> list:
    return [{"id": k, **v} for k, v in SPECIALTIES.items()]


def _normalize_specialty_id(specialty_id: str) -> str:
    """
    Normalise l'ID de spécialité reçu (espaces, casse, alias courants)
    pour éviter un retour silencieux vers 'java' à cause d'un ID mal formé.
    """
    if not specialty_id:
        return specialty_id

    key = specialty_id.strip().lower().replace(" ", "").replace("-", "").replace("_", "")

    aliases = {
        "java": "java",
        "springboot": "spring",
        "spring": "spring",
        "angular": "angular",
        "frameworkangular": "angular",
        "frontend": "angular",
        "frontenddeveloper": "angular",
        "ml": "ml",
        "machinelearning": "ml",
        "ia": "ml",
        "devops": "devops",
        "rh": "rh",
        "hr": "rh",
    }
    return aliases.get(key, specialty_id)


def generate_questions_by_specialty(specialty_id: str, excluded_questions: list = None) -> list:
    """
    Génère les questions en garantissant la variété :
    1. Essaie Gemini avec un seed de variation
    2. Fallback sur le pool local avec sélection aléatoire équilibrée
    3. Exclut les questions déjà posées à cet utilisateur (si fourni)
    """
    original_id = specialty_id
    specialty_id = _normalize_specialty_id(specialty_id)
    spec = SPECIALTIES.get(specialty_id)
    if not spec:
        # IMPORTANT : on ne retombe plus silencieusement sur "java" sans le signaler.
        # On log clairement l'ID reçu pour identifier le vrai bug côté Angular/Spring Boot.
        print(f"[WARN] specialty_id inconnu reçu par le service Python : '{original_id}' "
              f"— attendu un de {list(SPECIALTIES.keys())}. Vérifie ce qu'Angular envoie.")
        return QUESTION_POOL.get("java", [])[:15]

    count = spec["count"]
    excluded_questions = excluded_questions or []

    # ── Essai Gemini avec seed de variation ──────────────────────────────────
    if GEMINI_API_KEY:
        variation_seed = int(time.time()) % 500  # varie toutes les ~8 min
        prompt = f"""Tu es un recruteur expert. Génère exactement {count} questions d'entretien UNIQUES pour :
Spécialité : {spec['title']}
Description : {spec['description']}
Niveau : {spec['level']}
Variation #{variation_seed} — génère des questions DIFFÉRENTES des versions précédentes.

RÈGLES :
- Pas de questions trop basiques ou répétitives
- Mélange : théorie, pratique, situation réelle, projet
- Au moins 2 questions sur des cas concrets vécus
- Réponds UNIQUEMENT en JSON valide sans markdown ni backticks :
[{{"id":1,"category":"Présentation","question":"...","difficulty":"easy","tip":"conseil court","time_suggested":60}}]"""

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
            questions = json.loads(raw.strip())
            if questions and len(questions) >= count // 2:
                return _shuffle_and_reindex(questions[:count])
        except Exception:
            pass

    # ── Fallback : pool local étendu ─────────────────────────────────────────
    return _select_from_pool(specialty_id, count, excluded_questions)


def _select_from_pool(specialty_id: str, count: int, excluded: list) -> list:
    """Sélectionne count questions depuis le pool avec distribution équilibrée."""
    specialty_id = _normalize_specialty_id(specialty_id)
    pool = QUESTION_POOL.get(specialty_id)
    if pool is None:
        print(f"[WARN] Aucun pool de questions pour '{specialty_id}', fallback Java.")
        pool = QUESTION_POOL["java"]

    # Exclure les questions déjà posées à cet utilisateur
    if excluded:
        pool = [q for q in pool if q["question"] not in excluded]

    rng = random.Random(time.time())  # seed différent à chaque appel
    shuffled = pool.copy()
    rng.shuffle(shuffled)

    # Distribution équilibrée par difficulté
    easy   = [q for q in shuffled if q["difficulty"] == "easy"]
    medium = [q for q in shuffled if q["difficulty"] == "medium"]
    hard   = [q for q in shuffled if q["difficulty"] == "hard"]

    n_easy   = max(2, count // 4)
    n_hard   = max(2, count // 4)
    n_medium = count - n_easy - n_hard

    selected = (easy[:n_easy] + medium[:n_medium] + hard[:n_hard])[:count]

    # Si pas assez, compléter avec ce qui reste
    if len(selected) < count:
        remaining = [q for q in shuffled if q not in selected]
        selected += remaining[:count - len(selected)]

    rng.shuffle(selected)
    return _shuffle_and_reindex(selected)


def _shuffle_and_reindex(questions: list) -> list:
    """Ré-indexe les ids après mélange."""
    result = []
    for i, q in enumerate(questions):
        q_copy = dict(q)
        q_copy["id"] = i + 1
        result.append(q_copy)
    return result


def get_next_question_adaptive(current_score: float, asked_ids: list, all_questions: list) -> dict | None:
    """
    Difficulté progressive : retourne la prochaine question selon le score actuel.
    Si score >= 75 → préfère les hard
    Si score >= 50 → préfère les medium
    Sinon         → préfère les easy
    """
    remaining = [q for q in all_questions if q.get("id") not in asked_ids]
    if not remaining:
        return None

    if current_score >= 75:
        preferred = "hard"
    elif current_score >= 50:
        preferred = "medium"
    else:
        preferred = "easy"

    preferred_qs = [q for q in remaining if q.get("difficulty") == preferred]
    if preferred_qs:
        return random.choice(preferred_qs)

    return random.choice(remaining)


import base64
import re

# Détecte aussi les sons prolongés type "euhhh", "aaa", "mmm", "hmmm"
# en plus de la liste de mots fixes (la liste seule ratait "aa", "euhh", etc.)
_HESITATION_SOUND_RE = re.compile(
    r"^(e+u*h+|h+m+|a+h*|m+h*m*|b+e*n+|o+h+)$", re.IGNORECASE
)


def compute_final_score(
    score_technique: float,
    score_communication: float,
    score_confiance: float,
    hesitation_ratio: float = 0.0,
    eye_contact: float = 75,
    posture: float = 75,
    engagement: float = 75,
    score_stress_vocal: float | None = None,
) -> dict:
    """
    Combine le contenu (justesse technique via Gemini), la voix
    (hésitations, ton) et la webcam (regard, posture, engagement)
    en UN score global + UN score de stress cohérents.

    C'était la pièce manquante : avant, rien ne combinait tout ça.
    Le "score_technique" arrive déjà de l'évaluation Gemini du contenu
    (est-ce que la réponse est correcte techniquement).
    """
    # Score global : le contenu compte le plus (60%),
    # la communication et la webcam ajustent le reste.
    score_global = (
        score_technique * 0.55
        + score_communication * 0.20
        + score_confiance * 0.10
        + eye_contact * 0.08
        + posture * 0.07
    )
    score_global = max(0, min(100, int(score_global)))

    # Score de stress : basé sur les hésitations vocales réelles,
    # le manque de contact visuel/engagement, et le ton (si Gemini l'a estimé).
    stress_from_hesitation = min(100, hesitation_ratio * 250)
    stress_from_engagement = max(0, 100 - engagement)
    if score_stress_vocal is not None:
        stress = stress_from_hesitation * 0.35 + stress_from_engagement * 0.25 + score_stress_vocal * 0.40
    else:
        stress = stress_from_hesitation * 0.55 + stress_from_engagement * 0.45
    stress = max(0, min(100, int(stress)))

    if stress < 30:
        stress_label = "Faible"
    elif stress < 60:
        stress_label = "Modéré"
    else:
        stress_label = "Élevé"

    return {
        "score_global": score_global,
        "score_stress": stress,
        "stress_label": stress_label,
    }


def analyze_voice_response(
    transcript: str,
    question: str,
    specialty: str,
    audio_bytes: bytes | None = None,
    audio_mime: str | None = None,
) -> dict:
    """
    Analyse la réponse vocale avec métriques locales + Gemini.
    Détecte les hésitations, la longueur, et enrichit le prompt Gemini.
    Si `audio_bytes` est fourni, l'audio réel est envoyé à Gemini en plus
    du texte transcrit, pour juger le ton, le rythme et les hésitations
    réelles (pas seulement les mots-clés du texte).
    """
    # ── Analyse locale NLP simple ────────────────────────────────────────────
    words = transcript.split() if transcript else []
    word_count = len(words)

    hesitation_words = {"euh", "hmm", "hm", "ben", "alors", "donc", "voilà",
                        "bah", "enfin", "genre", "en fait", "du coup"}
    hesitations = 0
    for w in words:
        cleaned = w.lower().strip(".,!?;:")
        if cleaned in hesitation_words or _HESITATION_SOUND_RE.match(cleaned):
            hesitations += 1
    hesitation_ratio = hesitations / max(word_count, 1)

    # Score confiance basé sur les hésitations (0 hésis = 100, beaucoup = pénalisé)
    local_confidence = max(40, 100 - int(hesitation_ratio * 300))

    # Score longueur : réponse trop courte pénalisée
    if word_count < 10:
        length_penalty = 30
    elif word_count < 30:
        length_penalty = 15
    else:
        length_penalty = 0

    if not GEMINI_API_KEY or not transcript.strip():
        base_score = max(40, local_confidence - length_penalty)
        return {
            "score_technique":     base_score,
            "score_communication": max(40, base_score - 5),
            "score_confiance":     local_confidence,
            "score_global":        base_score,
            "niveau":              "Bien" if base_score >= 65 else "Moyen",
            "points_forts":        "Réponse enregistrée",
            "points_ameliorer":    "Réduisez les hésitations et développez davantage" if hesitations > 3 else "Donnez plus d'exemples concrets",
            "reponse_ideale":      "Une réponse structurée avec la méthode STAR.",
            "analyse_vocale": {
                "nb_mots": word_count,
                "hesitations": hesitations,
                "conseil_immediat": "Parlez plus lentement et avec plus de confiance" if hesitations > 3 else "Bon rythme de parole"
            }
        }

    audio_instructions = ""
    if audio_bytes:
        audio_instructions = """
Un extrait audio de la réponse est joint à ce message.
Écoute-le et juge en plus :
- le rythme réel de la parole (trop rapide / trop lent / naturel)
- les hésitations réelles entendues (silences, "euh", bafouillages), pas seulement le texte
- le ton de la voix (confiant, hésitant, stressé, monocorde...)
Utilise ça pour ajuster score_confiance et score_communication."""

    prompt = f"""Évalue cette réponse d'entretien technique :
Question : {question}
Spécialité : {specialty}
Transcription : {transcript}

Contexte analyse locale :
- Nombre de mots : {word_count}
- Hésitations détectées (texte) : {hesitations} (euh, hmm, ben, donc...)
- Score confiance estimé localement : {local_confidence}/100
{audio_instructions}

Tiens compte de ces métriques dans ton évaluation.
Réponds UNIQUEMENT en JSON valide :
{{
  "score_technique": <0-100>,
  "score_communication": <0-100>,
  "score_confiance": <0-100>,
  "score_global": <0-100>,
  "score_stress_vocal": <0-100, 100 = très stressé d'après le ton/rythme/hésitations>,
  "niveau": "<Excellent|Bien|Moyen|Insuffisant>",
  "points_forts": "...",
  "points_ameliorer": "...",
  "reponse_ideale": "...",
  "analyse_vocale": {{
    "nb_mots": {word_count},
    "hesitations": {hesitations},
    "conseil_immediat": "conseil de 1 phrase sur la communication"
  }}
}}"""

    try:
        parts = [{"text": prompt}]
        if audio_bytes:
            parts.append({
                "inline_data": {
                    "mime_type": audio_mime or "audio/webm",
                    "data": base64.b64encode(audio_bytes).decode("utf-8"),
                }
            })

        response = httpx.post(
            f"{GEMINI_URL}?key={GEMINI_API_KEY}",
            json={"contents": [{"parts": parts}]},
            timeout=45
        )
        data = response.json()
        raw = data["candidates"][0]["content"]["parts"][0]["text"].strip()
        if raw.startswith("```"):
            raw = raw.split("```")[1]
            if raw.startswith("json"): raw = raw[4:]
        return json.loads(raw.strip())
    except Exception:
        return {
            "score_technique":     65, "score_communication": 70,
            "score_confiance":     local_confidence, "score_global": 68,
            "niveau":              "Bien",
            "points_forts":        "Réponse reçue et analysée",
            "points_ameliorer":    "Réduisez les hésitations" if hesitations > 3 else "Développez davantage avec des exemples",
            "reponse_ideale":      "Une réponse structurée avec la méthode STAR.",
            "analyse_vocale": {
                "nb_mots": word_count,
                "hesitations": hesitations,
                "conseil_immediat": "Parlez avec plus d'assurance"
            }
        }


def generate_final_feedback(specialty: str, answers: list, avg_scores: dict) -> dict:
    """Génère le rapport final avec plan d'apprentissage personnalisé."""
    score = avg_scores.get("global", 0) or avg_scores.get("technique", 0)

    if not GEMINI_API_KEY:
        return _fallback_feedback(score, specialty)

    # Résumé des réponses pour contexte
    answers_summary = []
    for i, a in enumerate(answers[:5]):  # max 5 pour le prompt
        q_text = a.get("question", {}).get("question", "") if isinstance(a.get("question"), dict) else str(a.get("question", ""))
        score_q = a.get("evaluation", {}).get("score_global", 0) if isinstance(a.get("evaluation"), dict) else 0
        answers_summary.append(f"Q{i+1} ({score_q}/100): {q_text[:80]}")

    prompt = f"""Génère un rapport final d'entretien complet et personnalisé :
Spécialité : {specialty}
Score technique : {avg_scores.get('technique', 0)}%
Score communication : {avg_scores.get('communication', 0)}%
Score confiance : {avg_scores.get('confiance', 0)}%
Score global : {score}%
Questions répondues : {len(answers)}

Aperçu des réponses :
{chr(10).join(answers_summary)}

Réponds UNIQUEMENT en JSON valide :
{{
  "titre": "...",
  "message_global": "message personnalisé de 2-3 phrases",
  "points_forts": ["point 1", "point 2", "point 3"],
  "axes_amelioration": ["axe 1", "axe 2", "axe 3"],
  "conseil_final": "conseil motivant et actionnable",
  "recommandation": "<Prêt pour l'entretien|En bonne voie|Préparation supplémentaire recommandée>",
  "plan_apprentissage": [
    {{"semaine": 1, "theme": "...", "ressource": "...", "action": "exercice concret à faire"}},
    {{"semaine": 2, "theme": "...", "ressource": "...", "action": "exercice concret à faire"}},
    {{"semaine": 3, "theme": "...", "ressource": "...", "action": "exercice concret à faire"}}
  ]
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
    except Exception:
        return _fallback_feedback(score, specialty)


def _fallback_feedback(score: float, specialty: str) -> dict:
    if score >= 75:
        titre = "Excellent travail ! 🎉"
        reco  = "Prêt pour l'entretien"
        msg   = f"Vous avez démontré une très bonne maîtrise de {specialty}. Continuez ainsi !"
    elif score >= 50:
        titre = "Bon effort !"
        reco  = "En bonne voie"
        msg   = f"Vous avez de bonnes bases en {specialty}. Quelques points à approfondir."
    else:
        titre = "À améliorer"
        reco  = "Préparation supplémentaire recommandée"
        msg   = f"Continuez à pratiquer {specialty} régulièrement pour progresser."

    return {
        "titre":             titre,
        "message_global":    msg,
        "points_forts":      ["Participation active", "Communication globalement claire", "Réponses cohérentes"],
        "axes_amelioration": ["Approfondissez les concepts techniques avancés", "Donnez plus d'exemples concrets", "Structurez vos réponses avec la méthode STAR"],
        "conseil_final":     "La pratique régulière d'entretiens simulés est le meilleur moyen de progresser !",
        "recommandation":    reco,
        "plan_apprentissage": [
            {"semaine": 1, "theme": f"Bases de {specialty}", "ressource": "Documentation officielle", "action": "Lire et résumer les concepts clés"},
            {"semaine": 2, "theme": "Projets pratiques",     "ressource": "GitHub / Exercices",       "action": "Coder un mini-projet en 48h"},
            {"semaine": 3, "theme": "Entretiens simulés",    "ressource": "HireVision AI",            "action": "Refaire 3 simulations d'entretien"}
        ]
    }