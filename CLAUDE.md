# Vanilla Reforged

Vanilla+ Fabric mod (Minecraft 26.3, Java 25). Build commands are in `README.md`, feature designs in
`docs/` (e.g. `docs/pale-garden.md`).

## Workflow

- Every change (feature, fix, refactor, docs, …) goes in its own branch, never directly on `main`.
- Branch names: `<type>/<task>` in kebab-case, e.g. `feat/add-sticky-piston-recipe`,
  `docs/add-pale-garden-docs`.
- PR titles: `<type>: <Task>` with the task capitalized, e.g. `feat: Add sticky piston recipe`.
- Types: `feat`, `fix`, `refactor`, `docs`, `test`, `build`, `ci`, `chore`.
- Before pushing, `./gradlew build` must pass (it's the same gate CI runs on every push and PR).

## Code

- Follow best practices: small focused classes, Fabric API over Mixins, Mixins only when there's no
  API hook.
- Write tests when they add confidence, not by default. Game behavior (recipes, mobs, blocks) is
  best covered with Fabric GameTests; plain logic with JUnit. Set up the test infrastructure with
  the first feature that needs it.
- Comments only for what the code can't say: edge cases, non-obvious vanilla behavior, workarounds.
  No comments that restate the code.
