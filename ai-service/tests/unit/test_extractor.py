"""
Tests unitaires purs pour extractor.py.

Ces fonctions parsent du texte -> dict/list, sans I/O. Le seul piège :
`extract_skills` appelle `cosine_sim` (import depuis embeddings.py, donc le
vrai modèle d'embeddings) pour son "filet sémantique" — mais UNIQUEMENT sur
des lignes courtes contenant une virgule/puce (< 60 caractères). On mocke
`extractor.cosine_sim` pour ne jamais dépendre du modèle réel ici : ça rend
les tests rapides et déterministes.
"""
import pytest
from extractor import (
    extract_skills, extract_education, extract_experience,
    extract_projects, extract_languages, extract_certifications,
    extract_name, _find_ambiguous_short_skills,
)


@pytest.fixture(autouse=True)
def no_semantic_fallback(mocker):
    """Neutralise le filet sémantique (étape 3 d'extract_skills) : on veut
    tester la logique de matching par mots-clés indépendamment du modèle ML."""
    mocker.patch("extractor.cosine_sim", return_value=0.0)


# ── extract_skills ──────────────────────────────────────────────────────────

class TestExtractSkills:

    def test_detects_known_technologies(self):
        text = "Compétences: Java, Spring Boot, Docker, Angular, MySQL"
        skills = extract_skills(text)
        assert "Java" in skills
        assert "Docker" in skills
        assert "Angular" in skills

    def test_uppercases_acronym_style_skills(self):
        """c++, c#, .net, api, rest, css, git, html, gcp, aws, jpa doivent
        rester en MAJUSCULES plutôt qu'en Title Case."""
        text = "Compétences: C++, AWS, REST API, HTML, CSS"
        skills = extract_skills(text)
        assert "C++" in skills
        assert "AWS" in skills
        assert "REST" in skills
        assert "API" in skills

    def test_cplusplus_csharp_dotnet_detected_with_trailing_punctuation(self):
        """Régression : ces skills se terminent par un symbole ('+', '#', '.')
        -> `\\b` échouait quand ils étaient suivis d'un autre caractère
        non-alphanumérique (virgule, fin de ligne). Corrigé via des
        lookarounds sur l'alphanumérique (`_skill_boundary_pattern`)."""
        assert "C++" in extract_skills("Langages: C++, Java")
        assert "C++" in extract_skills("Je code en C++ depuis 3 ans.")
        assert "C#" in extract_skills("Compétences: C#, .NET")
        assert ".NET" in extract_skills("Compétences: C#, .NET")

    def test_cplusplus_not_matched_inside_longer_token(self):
        """Le fix ne doit pas réintroduire de faux positif : 'c++' ne doit
        pas matcher à l'intérieur d'un identifiant plus long."""
        assert "C++" not in extract_skills("le langage abc++experimental n'existe pas")

    def test_does_not_duplicate_skill(self):
        text = "Java, Java, JAVA — développeur Java confirmé"
        skills = extract_skills(text)
        assert skills.count("Java") == 1

    def test_ambiguous_short_skill_c_only_in_list_context(self):
        """Bug historique : 'C' matchait 'c'est' à cause du \\b qui traite
        l'apostrophe comme frontière de mot. Ne doit PAS être détecté ici."""
        text = "Ce candidat pense que c'est un bon projet et qu'il faut continuer."
        skills = extract_skills(text)
        assert "C" not in skills

    def test_ambiguous_short_skill_c_detected_as_list_item(self):
        """Mais 'C' DOIT être détecté quand il apparaît comme item de liste."""
        text = "Langages: C, Python, Java"
        skills = extract_skills(text)
        assert "C" in skills

    def test_go_language_detected_in_list(self):
        text = "Compétences techniques:\nGo, Docker, Kubernetes"
        skills = extract_skills(text)
        assert "Go" in skills

    def test_semantic_fallback_adds_unlisted_technology(self, mocker):
        """Quand le filet sémantique matche (mocké ici à une valeur haute),
        une techno absente de SKILLS_LIST (ex: Quarkus) doit être rattachée
        au label de référence le plus proche."""
        mocker.patch("extractor.cosine_sim", return_value=0.9)
        text = "Compétences:\nQuarkus, microservices"
        skills = extract_skills(text)
        # Le mock renvoie 0.9 pour TOUTES les paires -> le premier label de
        # SEMANTIC_SKILL_REFERENCES qui n'est pas déjà trouvé doit apparaître.
        assert len(skills) > 0

    def test_empty_text_returns_empty_list(self):
        assert extract_skills("") == []

    def test_no_false_positive_on_generic_text(self):
        text = "Bonjour, je m'appelle Karim et j'aime le sport et la lecture."
        skills = extract_skills(text)
        assert skills == []


# ── extract_education ───────────────────────────────────────────────────────

class TestExtractEducation:

    def test_extracts_degree_with_dash_separated_institution(self):
        text = (
            "Formation\n"
            "2021-2024\n"
            "Ingénieur en informatique - ESPRIT\n"
            "Expérience\n"
        )
        education = extract_education(text)
        assert len(education) >= 1
        assert "ESPRIT" in education[0]["institution"] or "ESPRIT" in education[0]["degree"]

    def test_no_education_section_returns_empty(self):
        text = "Expérience\n2021-2024\nDéveloppeur, ACME\n"
        assert extract_education(text) == []

    def test_caps_at_five_entries(self):
        lines = ["Formation"]
        for i in range(8):
            lines.append(f"20{i}0-20{i}2")
            lines.append(f"Diplôme {i} - Ecole {i}")
        text = "\n".join(lines) + "\nExpérience\n"
        education = extract_education(text)
        assert len(education) <= 5


# ── extract_experience ──────────────────────────────────────────────────────

class TestExtractExperience:

    def test_extracts_title_company_period(self):
        text = (
            "Experience\n"
            "02/2023 Développeur Full Stack, ACME Corp\n"
            "Suite\n"
            "- Développement d'API REST\n"
            "Projets\n"
        )
        experience = extract_experience(text)
        assert len(experience) == 1
        assert experience[0]["period"] == "02/2023"
        assert "ACME Corp" in experience[0]["company"]

    def test_no_experience_section_returns_empty(self):
        text = "Formation\n2021-2024\nESPRIT\n"
        assert extract_experience(text) == []


# ── extract_projects ─────────────────────────────────────────────────────────

class TestExtractProjects:

    def test_extracts_project_title_and_description(self):
        text = (
            "Projets\n"
            "StreetLeague — plateforme sportive full-stack 2024-2025\n"
            "plateforme de gestion de tournois avec IA de recommandation\n"
            "Formation\n"
        )
        projects = extract_projects(text)
        assert len(projects) == 1
        assert "StreetLeague" in projects[0]["title"]
        assert projects[0]["description"] == "plateforme de gestion de tournois avec IA de recommandation"
        assert projects[0]["period"] == "2024-2025"

    def test_header_line_alone_is_not_treated_as_a_project(self):
        """Régression : la ligne d'en-tête ('Projets') ne doit plus jamais
        apparaître elle-même comme une entrée de projet."""
        text = "Projets\nStreetLeague 2024-2025\ndescription du projet\nFormation\n"
        projects = extract_projects(text)
        assert all(p["title"].lower() != "projets" for p in projects)

    def test_multiple_projects_are_not_split_by_description_line(self):
        """Régression : une ligne de description consommée ne doit plus être
        retraitée comme un nouveau titre de projet au tour suivant."""
        text = (
            "Projets\n"
            "Projet Alpha 2023-2024\n"
            "gestion de stock avec Django\n"
            "Projet Beta 2024-2025\n"
            "chatbot avec Rasa\n"
            "Formation\n"
        )
        projects = extract_projects(text)
        titles = [p["title"] for p in projects]
        assert len(projects) == 2
        assert any("Alpha" in t for t in titles)
        assert any("Beta" in t for t in titles)

    def test_no_projects_section_returns_empty(self):
        text = "Formation\n2021-2024\nESPRIT\n"
        assert extract_projects(text) == []


# ── extract_languages ────────────────────────────────────────────────────────

class TestExtractLanguages:

    def test_detects_language_and_level(self):
        text = "Langues: Français (natif), Anglais (courant)"
        languages = extract_languages(text)
        by_name = {l["language"]: l["level"] for l in languages}
        assert by_name.get("Français") == "Natif"
        assert by_name.get("Anglais") == "Courant"

    def test_three_languages_on_one_line_do_not_cross_contaminate(self):
        """Régression : la fenêtre de contexte ne doit plus englober le
        niveau d'une langue précédente sur la même ligne."""
        text = "Langues: Français (natif), Anglais (courant), Espagnol (notions)"
        languages = extract_languages(text)
        by_name = {l["language"]: l["level"] for l in languages}
        assert by_name.get("Français") == "Natif"
        assert by_name.get("Anglais") == "Courant"
        assert by_name.get("Espagnol") == "Notions"

    def test_single_language_with_level_is_correct(self):
        """Sur une ligne isolée (pas de contamination possible), le niveau est bon."""
        text = "Anglais (courant)"
        languages = extract_languages(text)
        assert languages[0]["level"] == "Courant"

    def test_language_without_level_is_unspecified(self):
        text = "Langues: Allemand"
        languages = extract_languages(text)
        assert languages[0]["level"] == "Non spécifié"

    def test_no_language_mentioned(self):
        text = "Compétences: Java, Docker"
        assert extract_languages(text) == []


# ── extract_certifications ───────────────────────────────────────────────────

class TestExtractCertifications:

    def test_detects_certification_keyword(self):
        text = "Certifications\nAWS Certified Solutions Architect\n"
        certs = extract_certifications(text)
        assert any("AWS Certified" in c for c in certs)

    def test_caps_at_six_entries(self):
        text = "\n".join([f"Certification niveau {i}" for i in range(10)])
        certs = extract_certifications(text)
        assert len(certs) <= 6


# ── extract_name ─────────────────────────────────────────────────────────────

class TestExtractName:

    def test_extracts_first_short_line_as_name(self):
        text = "Karim Ben Ali\nDéveloppeur Full Stack\n"
        assert extract_name(text) == "Karim Ben Ali"

    def test_long_first_line_is_not_treated_as_name(self):
        text = "Développeur Full Stack passionné par les nouvelles technologies web\nExpérience\n"
        assert extract_name(text) == ""

    def test_empty_text_returns_empty_name(self):
        assert extract_name("") == ""


# ── _find_ambiguous_short_skills (fonction interne, testée directement) ─────

class TestFindAmbiguousShortSkills:

    def test_finds_r_language_as_list_item(self):
        assert "r" in _find_ambiguous_short_skills("langages: r, python")

    def test_does_not_confuse_r_inside_a_word(self):
        assert "r" not in _find_ambiguous_short_skills("bonjour à tous")