# AGENTS.md - ExactUploadFixer

Agent-neutral startup router for ExactUploadFixer. Root `ENGINEERING.md` and root `AGENTS.md` remain authoritative for safety, verification, deletion, scope, and truthfulness.

## Startup Sequence

1. Read `C:\Workspace\ENGINEERING.md`.
2. Read `C:\Workspace\AGENTS.md`.
3. Read `C:\Workspace\Project_Android\PROJECT_CONTEXT.md`.
4. Read `PROJECT_CONTEXT.md` in this directory.
5. Read `QA_CHECKLIST.md` when release or workflow behavior is in scope.

## Local Rules

- `PROJECT_CONTEXT.md` is the canonical app-local context for all agents.
- Preserve image math, EXIF handling, billing flavors, and signing safety.
- Store releases require explicit user approval.

## Verification

Use Gradle commands in `PROJECT_CONTEXT.md`; report skipped signing/store checks honestly.
