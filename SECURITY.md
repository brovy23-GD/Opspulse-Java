# Security policy

OpsPulse is a portfolio demonstration, not an authenticated production monitoring service.

- Do **not** expose the application to the public Internet without authentication, authorization, TLS and rate limiting.
- The PostgreSQL password in `compose.yml` is a **local-only example**, not an acceptable production secret.
- Use environment variables or a secret manager for actual credentials; never commit `.env` or tokens.
- Endpoints accept manual health observations. The application **does not** actively poll services.
- Report security concerns privately to the repository maintainer rather than sharing secrets in issues.
