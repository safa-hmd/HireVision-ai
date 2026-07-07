"""
Client LLM partagé — utilise Groq (API compatible OpenAI) à la place de Gemini.

Groq a déprécié llama-3.3-70b-versatile : on utilise donc les modèles
openai/gpt-oss-* recommandés par Groq (voir https://console.groq.com/docs/deprecations).
- openai/gpt-oss-120b : meilleure qualité (par défaut ici)
- openai/gpt-oss-20b  : plus rapide, si tu veux réduire encore la latence
"""
import os
import httpx

GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions"
GROQ_MODEL   = os.getenv("GROQ_MODEL", "openai/gpt-oss-120b")


def call_llm(prompt: str, timeout: int = 30, temperature: float = 0.7) -> str:
    """
    Envoie le prompt à Groq et retourne le texte brut généré.
    Lève une exception (RuntimeError / httpx.*) si la clé est absente ou
    si l'appel échoue — à catcher côté appelant pour déclencher le fallback local.
    """
    if not GROQ_API_KEY:
        raise RuntimeError("GROQ_API_KEY non configurée")

    response = httpx.post(
        GROQ_URL,
        headers={
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type":  "application/json",
        },
        json={
            "model":       GROQ_MODEL,
            "messages":    [{"role": "user", "content": prompt}],
            "temperature": temperature,
        },
        timeout=timeout,
    )
    response.raise_for_status()
    data = response.json()
    return data["choices"][0]["message"]["content"]
