# Tests — microservice IA HireVision

## Installer

```bash
pip install -r requirements.txt -r requirements-test.txt
```

## Lancer

```bash
pytest                          # tout
pytest tests/unit                # unitaire seulement (rapide, aucun modèle chargé)
pytest tests/integration         # endpoints FastAPI (mockés, pas de vraie API)
pytest -k test_analyze_skills    # un test précis
pytest --tb=short                # trace raccourcie
```

Tous les tests fournis tournent **sans clé Groq réelle et sans télécharger
le modèle d'embeddings** : tout ce qui appelle Gemini/Groq, GitHub, ou
`sentence-transformers` est mocké. Aucun coût, aucune dépendance réseau,
résultats déterministes.

## Structure

```
tests/
├── conftest.py                  # fixtures partagées (neutralise GROQ_API_KEY par défaut)
├── unit/
│   ├── test_extractor.py        # parsing regex du texte de CV
│   ├── test_job_matcher.py      # logique de seuils de matching CV/offre
│   └── test_ml_model.py         # logique de décision du profil (RF + sémantique)
└── integration/
    └── test_main_endpoints.py   # endpoints FastAPI via TestClient
```

## ⚠️ Bug critique corrigé pendant l'écriture de ces tests

`main.py` référençait `VoiceAnalyzeRequest` et `FeedbackRequest` comme
annotations de type sur `/interview/analyze-voice` et `/interview/feedback`,
mais **ces deux classes n'étaient définies nulle part** dans le fichier.
Sans `from __future__ import annotations`, Python évalue les annotations
immédiatement à la définition de la fonction → ça provoquait un
`NameError` dès l'import de `main.py`, empêchant **tout le service de
démarrer**. Les classes Pydantic manquantes ont été ajoutées à côté de
`JobMatchRequest`/`NextQuestionRequest`.

`main.py` définit **deux fois** `@app.get("/health")` (ligne ~62 et
ligne ~584, avec des réponses différentes). FastAPI garde la première
route enregistrée : la deuxième définition est du code mort actuellement
inatteignable. À voir si l'une des deux versions doit être supprimée.

## ✅ 3 bugs corrigés dans `extractor.py` (trouvés en écrivant ces tests)

1. **`extract_skills`** : le regex `\bc\+\+\b` (idem `c#`, `.net`) ne matchait
   jamais, car `\b` échoue quand le skill se termine par un symbole suivi
   d'un autre caractère non-alphanumérique. Corrigé avec des lookarounds
   dédiés (`_skill_boundary_pattern`).
2. **`extract_projects`** : la ligne d'en-tête de section ("Projets")
   devenait un faux projet, et chaque ligne de description était retraitée
   comme un nouveau titre au tour suivant. Corrigé (en-tête ignorée + saut
   d'index quand une description est consommée).
3. **`extract_languages`** : sur une ligne avec plusieurs langues
   ("Français (natif), Anglais (courant)"), la fenêtre de contexte trop
   large faisait hériter la deuxième langue du niveau de la première.
   Corrigé (on ne regarde plus qu'en avant, jusqu'à la virgule suivante).

Les tests de régression correspondants sont dans `test_extractor.py`
(`test_cplusplus_csharp_dotnet_detected_with_trailing_punctuation`,
`test_header_line_alone_is_not_treated_as_a_project`,
`test_multiple_projects_are_not_split_by_description_line`,
`test_three_languages_on_one_line_do_not_cross_contaminate`).

## Pourquoi ces mocks précis ?

`extractor.py`, `job_matcher.py` et `ml_model.py` font tous
`from embeddings import cosine_sim` (ou `best_match`) — un import qui
copie la fonction dans leur propre espace de noms. Il faut donc mocker
`extractor.cosine_sim`, `job_matcher.best_match`, etc. **individuellement
dans chaque module**, pas `embeddings.cosine_sim` — patcher ce dernier
n'aurait aucun effet sur le code déjà importé ailleurs.

## Prochaines étapes possibles

- Ajouter des tests pour `interview_analyzer.py` (génération de questions,
  `analyze_voice_response`, `generate_final_feedback`) — mêmes mocks de
  `call_llm` à prévoir.
- Ajouter un test d'intégration marqué `@pytest.mark.slow` qui charge le
  vrai modèle d'embeddings une fois, pour vérifier que l'intégration
  réelle fonctionne (à exclure du CI rapide avec `pytest -m "not slow"`).