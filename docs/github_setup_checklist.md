# GitHub Setup Checklist

Use this sequence to publish backend repo and onboard team.

## 1) Create remote repo

Repository name suggestion: `nw-trails-backend`

## 2) Push local code

```bash
git add .
git commit -m "Initialize Spring Boot backend with API v1 contract"
git remote add origin <your-repo-url>
git push -u origin main
```

## 3) Add collaborators

- Add all group members as collaborators (Write access)
- Enable branch protection for `main`:
  - require pull request reviews
  - require status checks (`mvn test`)

## 4) Initialize workflow

- Create labels: `auth`, `routes`, `checkin`, `swagger`, `integration`
- Create 4 issues from `docs/team_execution_plan.md`
- Assign one owner per issue

## 5) PR policy

- No direct pushes to `main`
- Keep PRs small (<300 lines when possible)
- Include API contract updates in same PR when interface changes
