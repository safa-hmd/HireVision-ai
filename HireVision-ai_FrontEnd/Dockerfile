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
    location /HireVision/ { \
        proxy_pass http://hirevision-backend/HireVision/; \
        proxy_set_header Host hirevision-backend; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
        proxy_set_header X-Forwarded-Host $host; \
        proxy_read_timeout 300s; \
        proxy_connect_timeout 75s; \
        proxy_send_timeout 300s; \
    } \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf 



EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s \
    CMD wget -qO- http://127.0.0.1:80 || exit 1