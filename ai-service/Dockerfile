# ──────────────────────────────────────────────
#  Stage 1 : Installer les dépendances Python
# ──────────────────────────────────────────────
FROM python:3.11-slim AS base

WORKDIR /app

# Dépendances système nécessaires à OpenCV et pdfplumber
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

# Installer les dépendances Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ──────────────────────────────────────────────
#  Stage 2 : Image de production
# ──────────────────────────────────────────────
FROM base

WORKDIR /app

# Copier le code source
COPY . .

# Variables d'environnement par défaut
ENV GROQ_API_KEY="" \
    GROQ_MODEL="openai/gpt-oss-120b"

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8000/health')" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
