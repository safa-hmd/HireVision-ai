import re
import spacy

nlp = spacy.load("fr_core_news_sm")

SKILLS_LIST = [
    # Backend
    "java", "spring", "spring boot", "python", "php", "symfony",
    "django", "flask", "fastapi", "node", "nodejs", "express",
    ".net", "dotnet", "c#", "go", "rust", "ruby", "rails",
    # Frontend
    "angular", "react", "reactjs", "vue", "typescript", "javascript",
    "html", "css", "bootstrap", "tailwind", "sass", "next.js", "nextjs",
    "nuxt", "svelte", "flutter", "dart",
    # Bases de données
    "mysql", "postgresql", "oracle", "mongodb", "redis", "sqlite",
    "elasticsearch", "firebase", "supabase",
    # DevOps / Cloud
    "docker", "kubernetes", "azure", "aws", "gcp", "jenkins",
    "github actions", "ci/cd", "terraform", "ansible", "nginx", "linux",
    # ML / Data
    "mlflow", "pytorch", "tensorflow", "keras", "pandas", "numpy",
    "scikit-learn", "opencv", "spark", "hadoop", "kafka",
    # Outils
    "git", "github", "gitlab", "jira", "postman", "figma",
    "maven", "gradle", "hibernate", "jpa", "rest", "api",
    "microservices", "agile", "scrum",
    # Autres langages
    "c++", "c", "kotlin", "swift", "matlab", "r",
    "verilog", "fpga", "raspberry pi", "perforce"
]

LEVEL_KEYWORDS = {
    "natif": "Natif", "native": "Natif", "maternelle": "Natif",
    "courant": "Courant", "fluent": "Courant", "bilingue": "Bilingue",
    "good": "Courant", "très bien": "Courant",
    "intermédiaire": "Intermédiaire", "intermediate": "Intermédiaire",
    "débutant": "Débutant", "beginner": "Débutant",
    "notions": "Notions", "basic": "Notions"
}

LANGUAGES = [
    "français", "anglais", "arabe", "allemand", "espagnol", "italien",
    "chinois", "japonais", "arabic", "french", "english", "spanish",
    "german", "italian", "chinese", "japanese", "dutch", "portuguese"
]

SECTION_HEADERS = [
    "education", "formation", "expérience", "experience", "projets",
    "projects", "skills", "compétences", "languages", "langues",
    "activities", "certifications", "achievements", "interests", "loisirs"
]


def extract_skills(text: str) -> list[str]:
    text_lower = text.lower()
    found = []
    for skill in SKILLS_LIST:
        pattern = r'\b' + re.escape(skill) + r'\b'
        if re.search(pattern, text_lower) and skill not in [f.lower() for f in found]:
            # Formatage lisible
            if skill in ["c++", "c#", ".net", "api", "rest", "css", "git", "html", "gcp", "aws", "jpa"]:
                found.append(skill.upper())
            elif skill in ["c"]:
                found.append("C")
            else:
                found.append(skill.title())
    return found


def _find_section_bounds(lines: list[str], section_names: list[str], end_names: list[str]) -> tuple[int, int]:
    start = -1
    end = len(lines)
    for i, line in enumerate(lines):
        line_lower = line.lower().strip()
        if start == -1 and any(name in line_lower for name in section_names):
            if len(line_lower) < 40:  # éviter de matcher dans du texte normal
                start = i
        elif start != -1 and any(name in line_lower for name in end_names):
            if len(line_lower) < 40:
                end = i
                break
    return start, end


def extract_education(text: str) -> list[dict]:
    education = []
    lines = [l.strip() for l in text.split("\n")]

    start, end = _find_section_bounds(
        lines,
        ["education", "formation", "parcours académique", "études"],
        ["experience", "expérience", "projets", "projects", "skills",
         "compétences", "activities", "certifications"]
    )

    if start == -1:
        # fallback : chercher par année + institution
        for i, line in enumerate(lines):
            year = re.search(r'(20\d{2}|19\d{2})\s*[-–]\s*(20\d{2}|19\d{2})', line)
            edu_kw = ["university", "université", "school", "institute", "esprit",
                      "ensi", "insat", "iset", "enit", "engineering", "preparatory"]
            if year and any(kw in line.lower() for kw in edu_kw):
                education.append({
                    "degree": line[year.end():].strip()[:80] or line.strip()[:80],
                    "institution": line.strip()[:80],
                    "period": year.group(0)
                })
        return education[:5]

    section_lines = lines[start:end]
    i = 0
    while i < len(section_lines):
        line = section_lines[i]
        year = re.search(r'(20\d{2}|19\d{2})\s*[-–]\s*(20\d{2}|19\d{2}|présent|present)', line, re.IGNORECASE)
        if year:
            period = year.group(0)
            rest = line[year.end():].strip()

            degree = ""
            institution = ""

            if "-" in rest or "–" in rest:
                parts = re.split(r'\s[-–]\s', rest, maxsplit=1)
                degree = parts[0].strip()
                institution = parts[1].strip() if len(parts) > 1 else ""
            elif rest:
                degree = rest
                if i + 1 < len(section_lines):
                    institution = section_lines[i + 1].strip()

            if not degree and i + 1 < len(section_lines):
                degree = section_lines[i + 1].strip()

            if len(degree) > 3:
                education.append({
                    "degree": degree[:80],
                    "institution": institution[:80] if institution else "Non spécifié",
                    "period": period
                })
        i += 1

    return education[:5]


def extract_experience(text: str) -> list[dict]:
    experience = []
    lines = [l.strip() for l in text.split("\n")]

    start, end = _find_section_bounds(
        lines,
        ["experience", "expérience", "professional experience", "work experience"],
        ["projets", "projects", "education", "formation", "skills",
         "compétences", "activities", "certifications", "loisirs"]
    )

    if start == -1:
        return []

    section_lines = lines[start:end]
    i = 0
    while i < len(section_lines):
        line = section_lines[i]

        # Pattern date : 12-02-2024 ou 2022-2024 ou 02/2022
        date_match = re.search(
            r'(\d{2}-\d{2}-\d{4}|\d{4}\s*[-–]\s*\d{4}|\d{2}/\d{4})',
            line, re.IGNORECASE
        )

        if date_match:
            period = date_match.group(0)
            rest = line[date_match.end():].strip()

            title = ""
            company = ""
            description = ""

            # Chercher titre et compagnie
            if rest:
                if "," in rest:
                    parts = rest.split(",", 1)
                    title = parts[0].strip()
                    company = parts[1].strip()
                else:
                    title = rest

            # Ligne suivante = company si pas trouvé
            if not company and i + 1 < len(section_lines):
                next_line = section_lines[i + 1]
                if not re.search(r'\d{4}', next_line):
                    company = next_line.strip()

            # Description
            if i + 2 < len(section_lines):
                desc_line = section_lines[i + 2]
                if desc_line.startswith(("∠", "•", "-", "→", ">")):
                    description = desc_line.lstrip("∠•-→> ").strip()
                else:
                    description = desc_line.strip()

            if title and len(title) > 3:
                if not any(e["title"] == title[:80] for e in experience):
                    experience.append({
                        "title": title[:80],
                        "company": company[:60] if company else "Non spécifié",
                        "period": period,
                        "description": description[:150]
                    })
        i += 1

    return experience[:5]


def extract_languages(text: str) -> list[dict]:
    text_lower = text.lower()
    found = []
    for lang in LANGUAGES:
        if re.search(r'\b' + re.escape(lang) + r'\b', text_lower):
            level = "Non spécifié"
            idx = text_lower.find(lang)
            context = text_lower[max(0, idx - 40):idx + 80]
            for key, value in LEVEL_KEYWORDS.items():
                if key in context:
                    level = value
                    break
            display = lang.capitalize()
            if display not in [l["language"] for l in found]:
                found.append({"language": display, "level": level})
    return found


def extract_certifications(text: str) -> list[str]:
    certs = []
    cert_keywords = [
        "aws certified", "oracle certified", "microsoft certified",
        "google cloud", "kubernetes administrator", "cka", "ckad",
        "pmp", "scrum master", "cisco", "ccna", "azure certified",
        "certification", "certificat", "certified"
    ]
    lines = text.split("\n")
    for line in lines:
        if any(kw in line.lower() for kw in cert_keywords):
            c = line.strip().lstrip("∠•-→> ")
            if len(c) > 5:
                certs.append(c[:100])
    return certs[:6]


def extract_name(text: str) -> str:
    lines = [l.strip() for l in text.split("\n") if l.strip()]
    if lines:
        first = lines[0]
        if len(first.split()) <= 4 and len(first) < 50:
            return first
    return ""