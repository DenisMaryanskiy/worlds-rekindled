# Worlds Rekindled

Vanilla+ Fabric mod (Minecraft 26.3, Java 25). Build commands are in `README.md`, feature designs in
`docs/` (e.g. `docs/pale-garden.md`).

## Workflow

- Every change (feature, fix, refactor, docs, …) goes in its own branch, never directly on `main`.
- Branch names: `<type>/<task>` in kebab-case, e.g. `feat/add-sticky-piston-recipe`,
  `docs/add-pale-garden-docs`.
- PR titles and commit messages: `<type>: <Task>` with the task capitalized, e.g.
  `feat: Add sticky piston recipe`.
- Types: `feat`, `fix`, `refactor`, `docs`, `test`, `build`, `ci`, `chore`.
- Related small changes (e.g. a docs tweak needed by a feature) can share the feature's branch.
- Every new player-facing string goes into all six lang files (`en_us`, `ru_ru`, `de_de`, `fr_fr`,
  `es_es`, `it_it`), in vanilla's own wording for vanilla things; `TranslationTest` fails otherwise.
- Every PR that changes what a player sees adds itself to `CHANGELOG.md` as the next patch version
  of the current minor version (a new minor version for a big new addition), with its PR number.
- Before pushing, `./gradlew build` must pass (it's the same gate CI runs on every push and PR).
- After a PR is merged, sync `main` and remove its worktree and local branch (plus any other
  worktrees that are no longer needed).

## Code

- Follow best practices: small focused classes, Fabric API over Mixins, Mixins only when there's no
  API hook.
- Write tests when they add confidence, not by default. Game behavior (recipes, mobs, blocks) is
  covered with Fabric GameTests in `src/gametest/` (a separate `vanillareforged-test` mod, run by
  `./gradlew runGameTest` and as part of `build`); plain logic with JUnit.
- Comments only for what the code can't say: edge cases, non-obvious vanilla behavior, workarounds.
  No comments that restate the code.
