# CLAUDE.md - ExactUploadFixer

Agent-neutral startup router for ExactUploadFixer. Root `ENGINEERING.md` and root `CLAUDE.md` for general protocols. Refer to root `AGENTS.md` for Gemini-specific overrides.

## Startup Sequence

1. Read this file (`CLAUDE.md`) — project-local agent guidance.
2. Read `PROJECT_CONTEXT.md` in this directory — directory map and invariants.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md` — workspace-wide context.
4. Read `C:\Workspace\Project_Android\CLAUDE.md` — behavioral rules and Android patterns.
5. Review `C:\Workspace\Project_Android\tasks\lessons.md` if it exists.
6. Read `QA_CHECKLIST.md` when release or workflow behavior is in scope.

## Local Rules

- `PROJECT_CONTEXT.md` is the canonical app-local context for all agents.
- Preserve image math, EXIF handling, billing flavors, and signing safety.
- Store releases require explicit user approval.

## Verification

Use Gradle commands in `PROJECT_CONTEXT.md`; report skipped signing/store checks honestly.
