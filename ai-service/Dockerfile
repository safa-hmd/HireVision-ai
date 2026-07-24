# Image Python légère
FROM python:3.11-slim

# Éviter les questions interactives pendant apt-get
ENV DEBIAN_FRONTEND=noninteractive

# Dossier de travail dans le container
WORKDIR /app

# Installer les dépendances système nécessaires pour OpenCV / PDF / ML
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copier d'abord requirements.txt pour profiter du cache Docker
COPY requirements.txt .

# Mettre pip à jour + installer les dépendances avec timeout plus long
RUN pip install --upgrade pip setuptools wheel && \
    pip install --default-timeout=1000 --retries=10 --no-cache-dir -r requirements.txt

# Précharger le modèle sentence-transformers DANS L'IMAGE, au build, pas au
# runtime. Avant : le modèle était téléchargé depuis Hugging Face Hub au
# premier appel réel (cold start >60s, souvent plus sans HF_TOKEN), ce qui
# dépassait le readTimeout du RestTemplate côté backend Spring Boot et
# provoquait des 500 sur les premiers uploads de CV.
# Après : le modèle est déjà présent dans le cache HF de l'image, donc le
# préchargement au démarrage du container (voir main.py @app.on_event
# ("startup")) est quasi instantané et ne dépend plus du réseau.
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')"

# Copier le reste du projet
COPY . .

# Exposer le port FastAPI
EXPOSE 8000

# Lancer le serveur FastAPI
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]