# Development instructions used for this project

- Follow `IMPLEMENTATION_PLAN.md` and the supplied brief in `docs/source/`.
- Build one Spring Boot REST application. Keep the data model and business rules explainable.
- Keep HTTP, DTO, service, entity, repository, jobs, configuration, error, and calculation code in their existing packages.
- Use PostgreSQL transactions and row locks for seat allocation. Never rely on a Java-only lock.
- Keep secrets out of source control. Use the environment variables in `.env.example`.
- Write focused unit and integration tests for core behavior, especially concurrent holds.
- Do not add a frontend, deployment files, containers, microservices, or advanced authentication.
- Do not initialize Git, commit, push, or create a pull request; the project owner handles these.
- Record assumptions and exact local run steps in `README.md`.
