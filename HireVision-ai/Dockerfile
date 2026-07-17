# ──────────────────────────────────────────────
#  Stage 1 : Build Spring Boot avec Maven
# ──────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copier les fichiers Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Télécharger les dépendances (layer cache)
RUN ./mvnw dependency:go-offline -B

# Copier le code source et builder
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ──────────────────────────────────────────────
#  Stage 2 : Image de production légère
# ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier le JAR compilé
COPY --from=builder /app/target/*.jar app.jar

# Créer le répertoire uploads
RUN mkdir -p uploads/cvs uploads/profile-pics

# Variables d'environnement (surchargeables au runtime)
ENV PORT=8086 \
    DB_URL=jdbc:mysql://mysql:3306/hirevisiondb?createDatabaseIfNotExist=true&useUnicode=true&serverTimezone=UTC \
    DB_USERNAME=root \
    DB_PASSWORD=root \
    JWT_SECRET=changeMeInProduction! \
    FRONTEND_URL=http://localhost:4200

EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8086/HireVision/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
