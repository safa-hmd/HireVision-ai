# HireVision AI 🚀

Plateforme intelligente de préparation aux entretiens pour développeurs — Analyse de CV, Matching emploi, Simulation d'entretien, Developer Readiness Score, Roadmap personnalisée.

## Architecture

```
Angular (frontend, port 4200)
        │
        ▼
Spring Boot (backend, port 8086) ──── MySQL (hirevisiondb, port 3306)
        │
        ▼
FastAPI / ai-service (Python, port 8000) ──── Groq LLM
```

| Service | Dossier | Techno | Port |
|---|---|---|---|
| Frontend | `HireVision-ai_FrontEnd/` | Angular 16 + CoreUI | 4200 |
| Backend | `HireVision-ai/` | Spring Boot 3.5 + JWT/OAuth2 | 8086 |
| Service IA | `ai-service/` | FastAPI + scikit-learn + Groq | 8000 |

---

## 🚀 Démarrage rapide (Docker — Recommandé)

```bash
# 1. Copier et configurer les variables d'environnement
cp .env.example .env
# Remplir les valeurs dans .env (GROQ_API_KEY, JWT_SECRET, GOOGLE_CLIENT_*, MAIL_*)

# 2. Démarrer tous les services
docker compose up --build

# Services accessibles :
#   Frontend  : http://localhost:4200
#   Backend   : http://localhost:8086/HireVision
#   Swagger   : http://localhost:8086/HireVision/swagger-ui.html
#   AI Service: http://localhost:8000/docs
```

---

## Prérequis (développement local sans Docker)

- Java 17+ et Maven
- Node.js 18+ et npm
- Python 3.10+
- MySQL 8.0 (base `hirevisiondb`, créée automatiquement)

---

## Installation locale

### 1. Backend (Spring Boot)

```bash
cd HireVision-ai
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Remplir application.properties avec vos identifiants
mvnw.cmd spring-boot:run
```
→ démarre sur `http://localhost:8086`  
→ Swagger UI : `http://localhost:8086/HireVision/swagger-ui.html`

### 2. Service IA (Python)

```bash
cd ai-service
python -m venv venv
venv\Scripts\activate   # Windows
pip install -r requirements.txt
cp .env.example .env
# Renseigner GROQ_API_KEY dans .env
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
→ démarre sur `http://localhost:8000`  
→ Docs interactives : `http://localhost:8000/docs`

### 3. Frontend (Angular)

```bash
cd HireVision-ai_FrontEnd
npm install
npm start
```
→ démarre sur `http://localhost:4200`

---

## ✨ Fonctionnalités

### Candidat
- 📄 **Analyse CV IA** — Extraction de compétences, profil, score global, optimisations proposées
- 🎯 **Job Matching** — Compatibilité CV/offre détaillée par catégories
- 🎤 **Simulation d'entretien** — Questions adaptatives par spécialité, analyse vocale & comportementale
- 📊 **Developer Readiness Score** — Score multi-axe : Backend, Frontend, DB, DevOps, Soft Skills, Interview
- 🗺️ **Roadmap personnalisée** — Plan de carrière hebdomadaire basé sur les lacunes détectées
- 🏅 **Gamification** — Badges et progression
- 🌙 **Dark Mode** — Mode sombre mémorisé
- 🌐 **Multilingue** — Français, Anglais, Arabe

### Admin
- Dashboard analytics, gestion des offres, questions, utilisateurs, abonnements

---

## 🔒 Sécurité

- Tous les secrets sont externalisés en **variables d'environnement** (`${VAR:default}`)
- Les fichiers sensibles (`application.properties`, `.env`) sont dans `.gitignore`
- Les endpoints CV/matching exigent un JWT valide + vérification de propriété
- CORS restreint côté service IA

---

## 🐳 Docker

Chaque service possède son propre `Dockerfile` multi-stage :

| Fichier | Service |
|---|---|
| `HireVision-ai/Dockerfile` | Backend Spring Boot |
| `HireVision-ai_FrontEnd/Dockerfile` | Frontend Angular + Nginx |
| `ai-service/Dockerfile` | Service IA FastAPI |
| `docker-compose.yml` | Orchestration complète |

```bash
docker compose up --build    # Premier démarrage
docker compose up -d         # Mode daemon (arrière-plan)
docker compose down          # Arrêter et supprimer les conteneurs
docker compose down -v       # + Supprimer les volumes (réinitialise la DB)
```

---

## 📚 API Documentation

| URL | Description |
|---|---|
| `http://localhost:8086/HireVision/swagger-ui.html` | Swagger UI Backend |
| `http://localhost:8000/docs` | Swagger UI FastAPI |
| `http://localhost:8086/HireVision/actuator/health` | Health Check Backend |
| `http://localhost:8000/health` | Health Check AI Service |

© 2026 HireVision AI — Tous droits réservés.
