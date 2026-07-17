import httpx
import re

async def analyze_github_profile(username: str) -> dict:
    headers = {
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "HireVision-AI-App"
    }
    
    async with httpx.AsyncClient() as client:
        # 1. Fetch user profile
        try:
            profile_res = await client.get(f"https://api.github.com/users/{username}", headers=headers, timeout=10)
            if profile_res.status_code == 404:
                return {"error": "GitHub user not found"}
            profile_res.raise_for_status()
            profile_data = profile_res.json()
        except Exception as e:
            return {"error": f"Failed to retrieve GitHub profile: {str(e)}"}
            
        # 2. Fetch user repositories
        try:
            repos_res = await client.get(f"https://api.github.com/users/{username}/repos?per_page=50&sort=updated", headers=headers, timeout=10)
            repos_res.raise_for_status()
            repos = repos_res.json()
        except Exception as e:
            return {"error": f"Failed to retrieve GitHub repositories: {str(e)}"}

        total_repos = profile_data.get("public_repos", 0)
        followers = profile_data.get("followers", 0)
        
        # Analyze top repositories (up to 5 most active/recent ones)
        starred_count = 0
        languages_dict = {}
        has_dockerfile = False
        has_workflows = False
        has_tests = False
        has_readme = False
        
        repos_to_check = repos[:5] # Check top 5 repositories
        
        for repo in repos:
            starred_count += repo.get("stargazers_count", 0)
            lang = repo.get("language")
            if lang:
                languages_dict[lang] = languages_dict.get(lang, 0) + 1
        
        # Sort languages
        sorted_languages = sorted(languages_dict.items(), key=lambda x: x[1], reverse=True)
        top_languages = [l[0] for l in sorted_languages[:4]]
        
        # Check files in top repositories via search or content API
        for r in repos_to_check:
            repo_name = r.get("name")
            owner = username
            
            # Retrieve root contents to inspect key files/folders
            try:
                contents_res = await client.get(f"https://api.github.com/repos/{owner}/{repo_name}/contents", headers=headers, timeout=5)
                if contents_res.status_code == 200:
                    files = [f.get("name", "").lower() for f in contents_res.json() if isinstance(f, dict)]
                    
                    if "dockerfile" in files:
                        has_dockerfile = True
                        
                    if "readme.md" in files:
                        has_readme = True
                        
                    # Check for test folder or files
                    if any("test" in f or "spec" in f for f in files):
                        has_tests = True
                        
                    # Check for workflows folder (.github)
                    if ".github" in files:
                        # Try to check if workflows exist inside
                        wf_res = await client.get(f"https://api.github.com/repos/{owner}/{repo_name}/contents/.github/workflows", headers=headers, timeout=5)
                        if wf_res.status_code == 200:
                            has_workflows = True
            except Exception:
                pass # Ignore individual repo failures to avoid breaking the scanner
                
        # 3. Calculate GitHub Developer Score (out of 100)
        # Criteria:
        # - Repository quantity & activity (up to 20 pts)
        # - Follower and star engagement (up to 10 pts)
        # - Presence of detailed READMEs (up to 20 pts)
        # - Presence of Dockerfiles (up to 15 pts)
        # - Presence of CI/CD configs/workflows (up to 15 pts)
        # - Presence of tests (up to 20 pts)
        
        score = 30 # Base score for having a profile
        
        # Repo quantity score (max 20)
        repo_points = min(20, total_repos * 1.5)
        score += repo_points
        
        # Engagement score (max 10)
        engagement_points = min(10, (starred_count * 2) + (followers * 0.5))
        score += engagement_points
        
        # Best practices checks
        if has_readme:
            score += 15
        if has_dockerfile:
            score += 10
        if has_workflows:
            score += 10
        if has_tests:
            score += 15
            
        score = min(100, round(score))
        
        # Recommendations
        recommendations = []
        if not has_readme:
            recommendations.append("Ajouter un fichier README.md détaillé pour présenter vos projets principales.")
        if not has_dockerfile:
            recommendations.append("Ajouter un Dockerfile dans vos dépôts pour faciliter le déploiement et la conteneurisation.")
        if not has_workflows:
            recommendations.append("Mettre en place des pipelines CI/CD automatisés avec GitHub Actions (.github/workflows/).")
        if not has_tests:
            recommendations.append("Ajouter des dossiers de tests unitaires (dossier 'test/' ou fichiers '*.spec.*') pour prouver la stabilité de votre code.")
        if total_repos < 5:
            recommendations.append("Publier plus de projets personnels pour enrichir votre portfolio de développeur.")
            
        if not recommendations:
            recommendations.append("Excellent profil GitHub ! Votre code respecte toutes les bonnes pratiques modernes.")

        return {
            "username": username,
            "profile_url": profile_data.get("html_url"),
            "avatar_url": profile_data.get("avatar_url"),
            "total_repos": total_repos,
            "followers": followers,
            "stars": starred_count,
            "top_languages": top_languages,
            "score": score,
            "has_dockerfile": has_dockerfile,
            "has_workflows": has_workflows,
            "has_tests": has_tests,
            "has_readme": has_readme,
            "recommendations": recommendations
        }
