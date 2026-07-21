"""
Tests unitaires pour job_matcher.py.

`match_skills` appelle `best_match` (embeddings réels) pour tout ce qui n'est
pas un match exact après normalisation. On mocke `job_matcher.best_match`
pour contrôler précisément les scores de similarité et tester la logique
des seuils (EXACT_THRESHOLD / PARTIAL_THRESHOLD) de façon déterministe.
"""
from job_matcher import match_skills, normalize, EXACT_THRESHOLD, PARTIAL_THRESHOLD


# ── normalize() : pure, pas besoin de mock ──────────────────────────────────

class TestNormalize:

    def test_known_alias(self):
        assert normalize("Spring Boot") == "spring"
        assert normalize("C++") == "cpp"
        assert normalize("ReactJS") == "react"

    def test_unknown_skill_is_lowercased_and_stripped(self):
        assert normalize("  Kubernetes  ") == "kubernetes"

    def test_case_insensitive(self):
        assert normalize("NODEJS") == "node"


# ── match_skills() : logique de seuils, avec best_match mocké ──────────────

class TestMatchSkills:

    def test_exact_match_after_normalization(self):
        result = match_skills(["Spring Boot"], ["spring"])
        assert result["matched"] == ["spring"]
        assert result["matched_exact"] == 1
        assert result["score"] == 100

    def test_no_job_skills_returns_error(self):
        result = match_skills(["java"], [])
        assert "error" in result

    def test_no_cv_skills_all_missing(self):
        result = match_skills([], ["java", "docker"])
        assert result["missing"] == ["java", "docker"]
        assert result["score"] == 0
        assert result["compatible"] is False

    def test_score_above_exact_threshold_counts_as_matched(self, mocker):
        mocker.patch("job_matcher.best_match", return_value=("nodejs", EXACT_THRESHOLD + 0.01))
        result = match_skills(["nodejs"], ["node"])
        assert result["matched"] == ["node"]
        assert result["matched_partial"] == 0

    def test_score_between_thresholds_counts_as_partial(self, mocker):
        mocker.patch("job_matcher.best_match", return_value=("angular", PARTIAL_THRESHOLD + 0.01))
        result = match_skills(["angular"], ["react"])
        assert result["matched_partial"] == 1
        assert result["partial"][0]["job"] == "react"
        assert result["partial"][0]["cv"] == "angular"

    def test_score_below_partial_threshold_counts_as_missing(self, mocker):
        mocker.patch("job_matcher.best_match", return_value=("java", PARTIAL_THRESHOLD - 0.1))
        result = match_skills(["java"], ["kubernetes"])
        assert result["missing"] == ["kubernetes"]

    def test_compatibility_labels(self, mocker):
        # score >= 70 -> compatible
        mocker.patch("job_matcher.best_match", return_value=("x", 0.99))
        result = match_skills(["docker", "kubernetes", "aws"], ["docker", "kubernetes", "aws"])
        assert result["compatible"] is True
        assert result["label"] == "✅ Compatible"

        # score < 45 -> non compatible
        mocker.patch("job_matcher.best_match", return_value=("x", 0.0))
        result = match_skills(["python"], ["java", "docker", "kubernetes", "aws", "rust"])
        assert result["compatible"] is False
        assert result["label"] == "❌ Non compatible"
