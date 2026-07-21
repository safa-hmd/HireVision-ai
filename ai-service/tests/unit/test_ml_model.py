"""
Tests unitaires pour ml_model.py.

`predict_profile` combine deux signaux : la similarité sémantique
(`_semantic_profile_scores`, embeddings réels) et le RandomForest optionnel
(`model.joblib`). On mocke `_semantic_profile_scores` directement pour
tester la logique de décision (seuil MIN_RAW_SIMILARITY, fusion avec le RF)
sans dépendre du modèle d'embeddings réel ni du contenu exact de model.joblib.
"""
from ml_model import predict_profile, skill_to_vector, MIN_RAW_SIMILARITY, SKILL_COLUMNS


# ── skill_to_vector() : pure, corrige le bug historique de sous-chaîne ──────

class TestSkillToVector:

    def test_exact_match_sets_column_to_one(self):
        vector = skill_to_vector(["java", "docker"])
        assert vector[SKILL_COLUMNS.index("java")] == 1
        assert vector[SKILL_COLUMNS.index("docker")] == 1

    def test_short_skill_c_does_not_false_positive_on_react(self):
        """Bug historique corrigé : 'C' seul ne doit plus activer la colonne
        'react' (l'ancien code faisait `col in s or s in col` -> 'c' in 'react')."""
        vector = skill_to_vector(["c"])
        assert vector[SKILL_COLUMNS.index("react")] == 0

    def test_alias_is_normalized(self):
        vector = skill_to_vector(["Spring Boot", "C#"])
        assert vector[SKILL_COLUMNS.index("spring")] == 1
        assert vector[SKILL_COLUMNS.index("csharp")] == 1

    def test_unknown_skill_activates_nothing(self):
        vector = skill_to_vector(["solidworks"])
        assert sum(vector) == 0


# ── predict_profile() : logique de décision, RF désactivé pour isoler le test

class TestPredictProfile:

    def test_empty_skills_returns_non_determine(self):
        result = predict_profile([])
        assert result["profile"] == "Non déterminé"
        assert result["confidence"] == 0

    def test_below_min_similarity_returns_generic_profile(self, mocker):
        """Garde-fou anti-faux-positif IT : si même le meilleur score est
        trop faible, ne JAMAIS forcer un label IT sur un CV non-IT."""
        mocker.patch("ml_model.load_rf_model", return_value=None)
        mocker.patch(
            "ml_model._semantic_profile_scores",
            return_value=(
                {"Backend": MIN_RAW_SIMILARITY - 0.05, "Frontend": 0.1},
                {"Backend": 0.6, "Frontend": 0.4},
            ),
        )
        result = predict_profile(["solidworks", "catia"])
        assert result["profile"] == "Profil non catégorisé"
        assert result.get("is_generic") is True

    def test_above_min_similarity_returns_matched_profile(self, mocker):
        mocker.patch("ml_model.load_rf_model", return_value=None)
        mocker.patch(
            "ml_model._semantic_profile_scores",
            return_value=(
                {"Backend": 0.7, "Frontend": 0.2},
                {"Backend": 0.8, "Frontend": 0.2},
            ),
        )
        result = predict_profile(["java", "spring"])
        assert result["profile"] == "Backend"
        assert 0 <= result["confidence"] <= 100
        assert set(result["skill_scores"].keys()) == {"java", "spring"}

    def test_rf_agreement_boosts_confidence(self, mocker):
        """Quand le RandomForest est d'accord avec la prédiction sémantique,
        la confiance finale doit être renforcée (jamais > 99%)."""
        mocker.patch(
            "ml_model._semantic_profile_scores",
            return_value=(
                {"Backend": 0.7, "Frontend": 0.1},
                {"Backend": 0.8, "Frontend": 0.2},
            ),
        )
        fake_rf = mocker.Mock()
        fake_rf.predict.return_value = ["Backend"]
        fake_rf.predict_proba.return_value = [[0.9, 0.1]]
        mocker.patch("ml_model.load_rf_model", return_value=fake_rf)

        result = predict_profile(["java"])
        assert result["profile"] == "Backend"
        assert result["confidence"] <= 99

    def test_rf_disagreement_keeps_semantic_prediction(self, mocker):
        """En cas de désaccord, la méthode sémantique doit primer (le RF est
        entraîné sur un dataset trop petit pour être fiable seul)."""
        mocker.patch(
            "ml_model._semantic_profile_scores",
            return_value=(
                {"Backend": 0.7, "Frontend": 0.1},
                {"Backend": 0.8, "Frontend": 0.2},
            ),
        )
        fake_rf = mocker.Mock()
        fake_rf.predict.return_value = ["Frontend"]
        fake_rf.predict_proba.return_value = [[0.2, 0.8]]
        mocker.patch("ml_model.load_rf_model", return_value=fake_rf)

        result = predict_profile(["java"])
        assert result["profile"] == "Backend"

    def test_rf_exception_is_swallowed_and_ignored(self, mocker):
        """Le RF est purement optionnel : une exception ne doit jamais faire
        planter predict_profile."""
        mocker.patch(
            "ml_model._semantic_profile_scores",
            return_value=({"Backend": 0.7}, {"Backend": 1.0}),
        )
        fake_rf = mocker.Mock()
        fake_rf.predict.side_effect = Exception("boom")
        mocker.patch("ml_model.load_rf_model", return_value=fake_rf)

        result = predict_profile(["java"])
        assert result["profile"] == "Backend"
