# HireVision AI — Plateforme de Coach de Carrière

## Structure du projet

```
AI_Career_Coach_Platform/
│
├── index.html                    ← Point d'entrée (redirige vers login)
│
├── login/                        ← Authentification
│   ├── login.css                 ← Styles partagés auth
│   ├── login.js                  ← Logique auth partagée
│   ├── login.html                ← Page de connexion
│   ├── register.html             ← Inscription multi-étapes (3 étapes)
│   ├── forgot-password.html      ← Mot de passe oublié
│   └── reset-password.html       ← Réinitialisation du mot de passe
│
├── frontoffice/                  ← Espace candidat
│   ├── css/
│   │   └── style.css             ← Styles candidat
│   ├── js/
│   │   └── app.js                ← Logique JS candidat (tabs, charts, utils)
│   └── html/
│       ├── index.html            ← Dashboard candidat
│       ├── cv-analysis.html      ← Analyse CV par IA
│       ├── job-matching.html     ← Matching offres d'emploi
│       ├── interview-prep.html   ← Sélection catégorie entretien
│       ├── interview-session.html← Session entretien live (webcam + IA)
│       ├── feedback.html         ← Rapport de feedback post-entretien
│       ├── video-replay.html     ← Replay vidéo avec annotations IA
│       ├── career-roadmap.html   ← Plan de carrière personnalisé
│       └── profile.html          ← Profil & paramètres candidat
│
└── backoffice/                   ← Espace administrateur
    ├── css/
    │   └── style.css             ← Styles admin
    ├── js/
    │   └── app.js                ← Logique JS admin (charts, tables, utils)
    └── html/
        ├── index.html            ← Tableau de bord admin
        ├── users.html            ← Gestion des utilisateurs
        ├── user-detail.html      ← Détail d'un utilisateur
        ├── interviews.html       ← Gestion des entretiens
        ├── analytics.html        ← Analytics & statistiques
        ├── questions.html        ← Banque de questions
        ├── subscriptions.html    ← Abonnements & revenus
        └── settings.html         ← Paramètres plateforme
```

## Technologies utilisées

- **HTML5 / CSS3 / JavaScript** — Vanilla, aucune dépendance framework
- **Chart.js 4.4.1** — Graphiques et visualisations
- **Lucide Icons 0.383** — Icônes SVG légères
- **Google Fonts** — Syne (titres) + DM Sans (corps)

## Démarrage rapide

1. Ouvrir `index.html` dans un navigateur
2. La page redirige automatiquement vers `login/login.html`
3. Utiliser les boutons **démo** pour accéder rapidement :
   - **Candidat** → frontoffice
   - **Admin** → backoffice

## Comptes de démonstration

| Rôle      | Email                    | Mot de passe |
|-----------|--------------------------|-------------|
| Candidat  | jean@exemple.com         | demo1234    |
| Admin     | admin@hirevision.ai      | demo1234    |
