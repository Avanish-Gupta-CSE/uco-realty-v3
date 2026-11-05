# Instructions for AI agents working in this repository

This project keeps its memory in `.brain/` (git-ignored, never shipped). **Before doing anything:**

1. Read `.brain/START_HERE.md`, then `.brain/AGENT_RULES.md`, then `.brain/01_STATE/CURRENT_STATE.md` and `NEXT_STEPS.md`.
2. Check `git status` and `git log -5`. The code is the truth; if a note disagrees with it, fix the note.

**Before you write your final message of every response that changed something or learned something durable**, write back to `.brain/`: update `01_STATE/CURRENT_STATE.md` and `NEXT_STEPS.md`, append an entry to `01_STATE/SESSION_LOG.md`, record decisions in `02_DECISIONS/` and update the affected `03_SYSTEM/` docs (the exact checklist and sync matrix are in `.brain/AGENT_RULES.md` §3). This is how the next agent (or you, after the context window fills up) knows what is happening.

Short version of the owner's rules: explain in plain words and say where code lives; do not push, merge or delete branches unless asked; commit only when told; no mock data in code, SQL only in repositories; bank CSP rules (no inline CSS/JS, element ids); keep tests and docs in sync. Full list: `.brain/AGENT_RULES.md`.

If `.brain/` is missing (fresh clone), say so and ask the owner for it; use `README.md`, `DEPLOYMENT.md`, `TESTING_GUIDE.md` and `scripts/README.md` meanwhile.
