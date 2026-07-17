# ──────────────────────────────────────────────
#  Stage 1 : Build Angular
# ──────────────────────────────────────────────
FROM node:18-alpine AS builder
WORKDIR /app

# Installer les dépendances
COPY package*.json ./
RUN npm ci --silent

# Builder l'application Angular
COPY . .
RUN npm run build -- --configuration=production

# ──────────────────────────────────────────────
#  Stage 2 : Serveur Nginx de production
# ──────────────────────────────────────────────
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Supprimer le contenu par défaut Nginx
RUN rm -rf ./*

# Copier le build Angular
COPY --from=builder /app/dist/hire-vision-ai-front-end ./

# Configuration Nginx pour Angular routing (SPA)
RUN echo 'server { \
    listen 80; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api { \
        proxy_pass http://backend:8086; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s \
    CMD wget -qO- http://localhost:80 || exit 1
