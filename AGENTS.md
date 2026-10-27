# AGENTS.md

Guide for AI coding agents working on Kmais — an Android app that tracks car and motorcycle
maintenance: fuel log, consumption, cost per km and service reminders. It is the Android
twin of [kmais-app-ios](https://github.com/joaoduartemariucio/kmais-app-ios): same screens,
rules, agents and CI gates.

## Commands

```bash
make ci            # everything CI runs, in order — must pass before handing work back
make format        # fix formatting (Spotless + ktlint)
make lint          # layer rules + Android Lint, warnings as errors
make build         # build app + unit tests
make test          # unit tests with coverage
make coverage      # ≥ 80% of changed executable lines must be covered
```

Needs JDK 17+ and the Android SDK (`ANDROID_HOME` or `local.properties`); Gradle downloads the rest.

## Architecture — do not break these

Pragmatic Clean Architecture in one module, packages as layers. Details: [docs/architecture.md](docs/architecture.md).

- `domain/` is **plain Kotlin** (`java.time`, `java.math` only). Entities are immutable
  `data class`es; calculations are pure functions and never read the clock.
- `data/` is the only place that knows Room and WorkManager. Room entities end in `Model`; a
  new one must be added to `KmaisDatabase` **and** to the `presentation_no_data_types` rule in
  `scripts/layer_rules.py`. A schema change bumps the version and ships a migration.
- `presentation/` talks to Domain interfaces only. One ViewModel per screen, state in Compose
  `mutableStateOf`. No Room `Flow` collected in the UI.
- `app/AppDependencies` is the only place that creates concrete types.

## Conventions

- **No comments in Kotlin code.** The why goes in [docs/decisions.md](docs/decisions.md).
- Money is `BigDecimal`; distance and volume are `Double`.
- Form fields are `String` with a `NumberInput.Style`; validation is `List<ValidationIssue>`,
  blocking (red) or warning (orange).
- Every user-facing string goes in `app/src/main/res/values/strings.xml` with its **pt-BR**
  translation in `values-pt-rBR/strings.xml`.
- Coroutines: everything that touches storage is `suspend`; always rethrow `CancellationException`.

## Tests

- JUnit 4 with `kotlin.test` assertions and backtick names that read as sentences.
- Domain calculations in `app/src/test/.../calculations/`, ViewModels in `viewmodels/`, Room
  and WorkManager in `data/` (Robolectric).
- Use the fakes in `fakes/` (`InMemory…Repository`, `FixedDateProvider`, `TestLocalizer`);
  only `data/` tests touch Room.
- New or changed logic needs tests: the CI gate is 80% of changed lines
  (Compose screens and `app/` are excluded — see `.coverageignore`).

## Before handing work back

1. `make ci` passes.
2. `docs/decisions.md` has an entry for any choice a reader could not infer from the code.
3. Never commit, push or create branches unless the user asks.

## Agent workflow

This repo defines four Claude Code subagents in `.claude/agents/` — a car and motorcycle
expert, an Android specialist, a reviewer and an executor. How and why they work together:
[docs/agents.md](docs/agents.md).
