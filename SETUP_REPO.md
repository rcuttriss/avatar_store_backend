# Setting Up Backend Repository

This repository contains the Spring Boot backend for the Avatar Store application.

## Initial Setup Complete

✅ Git repository initialized
✅ Initial commit created
✅ .gitignore configured
✅ .gitattributes configured

## Next Steps: Push to Remote Repository

1. **Create a new repository on GitHub/GitLab/etc.**
   - Go to your Git hosting service
   - Create a new repository (e.g., `avatar-store-backend`)
   - Do NOT initialize with README, .gitignore, or license

2. **Add remote and push:**
   ```bash
   git remote add origin <your-repo-url>
   git branch -M main
   git push -u origin main
   ```

3. **Example:**
   ```bash
   git remote add origin https://github.com/yourusername/avatar-store-backend.git
   git branch -M main
   git push -u origin main
   ```

## Repository Structure

```
backend/
├── .git/
├── .gitignore
├── .gitattributes
├── README.md
├── pom.xml
└── src/
    └── main/
        ├── java/com/avatarstore/
        └── resources/
```

## Important Notes

- The `target/` directory is ignored (Maven build output)
- Environment variables in `application.properties` should be set via environment variables or `.env` file (not committed)
- Database schema is in `src/main/resources/db/`

