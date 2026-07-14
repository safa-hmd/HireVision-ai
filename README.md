# HireVision AI

Plateforme de recrutement assistée par IA : analyse de CV, matching CV ↔ offre d'emploi,
génération de questions d'entretien et analyse des réponses via LLM.

## Architecture

Le projet est composé de 3 services indépendants :

```
Angular (frontend, port 4200)
        │
        ▼
Spring Boot (backend, port 8086) ──── MySQL (hirevisiondb)
        │
        ▼
FastAPI / ai-service (Python, port 8000) ──── Groq (LLM)
```

| Service | Dossier | Techno | Port par défaut |
|---|---|---|---|
| Frontend | `HireVision-ai_FrontEnd/` | Angular | 4200 |
| Backend | `HireVision-ai/` | Spring Boot + Spring Security (JWT/OAuth2) | 8086 |
| Service IA | `ai-service/` | FastAPI, scikit-learn, sentence-transformers, Groq | 8000 |

## Prérequis

- Java 17+ et Maven
- Node.js + npm
- Python 3.10+
- MySQL (base `hirevisiondb`, créée automatiquement au démarrage grâce à `createDatabaseIfNotExist=true`)

## Installation & lancement

### 1. Backend (Spring Boot)

```bash
cd HireVision-ai
cp src/main/resources/application.properties.example src/main/resources/application.properties
# puis remplis application.properties avec tes propres identifiants MySQL, JWT secret, Google OAuth, Gmail
mvn spring-boot:run
```
→ démarre sur `http://localhost:8086`

### 2. Service IA (Python)

```bash
cd ai-service
python -m venv venv
source venv/bin/activate      # Windows : venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# puis renseigne GROQ_API_KEY dans .env
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
→ démarre sur `http://localhost:8000`

### 3. Frontend (Angular)

```bash
cd HireVision-ai_FrontEnd
npm install
ng serve
```
→ démarre sur `http://localhost:4200`

## Sécurité

- Les fichiers uploadés (CV, photos de profil) sont stockés dans `HireVision-ai/uploads/` — **jamais commités** (voir `.gitignore`).
- Les endpoints `/cvs/**` et `/matching-results/**` nécessitent une authentification JWT, avec vérification que l'utilisateur connecté est bien propriétaire de la ressource demandée.
- Ne jamais commiter `application.properties` ni `ai-service/.env` — utiliser les fichiers `.example` fournis comme modèle.
- CORS restreint côté service IA aux origines autorisées (voir `ai-service/main.py`).

## Fonctionnalités principales

- Analyse automatique de CV (extraction de compétences)
- Matching CV ↔ offre d'emploi avec score de compatibilité
- Génération de questions d'entretien via LLM
- Analyse des réponses aux entretiens
