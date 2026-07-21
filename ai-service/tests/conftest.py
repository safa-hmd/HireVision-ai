"""
Fixtures partagées pour toute la suite de tests.

Point important : `extractor.py`, `job_matcher.py` et `ml_model.py` font tous
`from embeddings import cosine_sim` (ou `best_match`). Ça copie la référence
de la fonction dans LEUR PROPRE espace de noms au moment de l'import. Donc
patcher `embeddings.cosine_sim` depuis un test n'a AUCUN effet sur ces
modules — il faut patcher `extractor.cosine_sim`, `job_matcher.cosine_sim`,
`ml_model.cosine_sim`, etc. individuellement. C'est un piège classique de
`unittest.mock` avec les imports `from x import y`.
"""
import os
import sys
import pytest

# Le microservice s'attend à tourner depuis son propre dossier (imports
# relatifs du style `from extractor import ...`). On l'ajoute au path pour
# que `pytest` (lancé depuis n'importe où) trouve les modules.
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


@pytest.fixture(autouse=True)
def no_network_by_default(monkeypatch):
    """
    Filet de sécurité : dans TOUS les tests (sauf ceux marqués `@pytest.mark.slow`),
    on s'assure qu'aucune vraie clé Groq n'est présente, pour ne jamais taper
    l'API réelle par accident si un mock est oublié quelque part.
    """
    monkeypatch.delenv("GROQ_API_KEY", raising=False)


@pytest.fixture
def fake_groq_key(monkeypatch):
    """À utiliser explicitement quand un test veut simuler 'clé Groq configurée'."""
    monkeypatch.setenv("GROQ_API_KEY", "fake-key-for-tests")
