"""
Module de similarité SÉMANTIQUE — remplace les comparaisons "mot-clé présent / absent"
par une vraie mesure de sens (embeddings de phrases).

Pourquoi ce module existe :
- Avant : "spring boot" == "spring" seulement s'ils sont dans le même dictionnaire d'alias
  écrit à la main. Une compétence non prévue (ex: "NestJS", "Quarkus") n'est reconnue
  par rien.
- Maintenant : on compare le SENS des mots via un modèle d'embeddings pré-entraîné.
  "NestJS" et "Node.js backend framework" auront un score de similarité élevé même
  sans dictionnaire d'alias exhaustif.

Le modèle "paraphrase-multilingual-MiniLM-L12-v2" est petit (~470 Mo), tourne sur CPU,
et comprend le français ET l'anglais (utile car les CV mélangent souvent les deux).
"""
from functools import lru_cache
import numpy as np
from sentence_transformers import SentenceTransformer

_MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2"
_model: SentenceTransformer | None = None


def get_model() -> SentenceTransformer:
    """Charge le modèle une seule fois (singleton) — il est lourd à instancier."""
    global _model
    if _model is None:
        _model = SentenceTransformer(_MODEL_NAME)
    return _model


@lru_cache(maxsize=4096)
def _embed_cached(text: str) -> tuple:
    vec = get_model().encode(text, normalize_embeddings=True)
    return tuple(float(x) for x in vec)


def embed(text: str) -> np.ndarray:
    return np.array(_embed_cached(text.strip().lower()))


def cosine_sim(text_a: str, text_b: str) -> float:
    """Similarité cosinus entre deux textes, dans [-1, 1] (généralement [0, 1] en pratique)."""
    if not text_a or not text_b:
        return 0.0
    va, vb = embed(text_a), embed(text_b)
    return float(np.dot(va, vb))


def best_match(query: str, candidates: list[str]) -> tuple[str | None, float]:
    """Retourne (meilleur candidat, score) parmi une liste, selon similarité sémantique."""
    if not candidates:
        return None, 0.0
    q = embed(query)
    sims = [float(np.dot(q, embed(c))) for c in candidates]
    idx = int(np.argmax(sims))
    return candidates[idx], sims[idx]


def batch_embed(texts: list[str]) -> np.ndarray:
    """Pour embedder plusieurs textes d'un coup (plus rapide que un par un)."""
    model = get_model()
    return model.encode(texts, normalize_embeddings=True)
