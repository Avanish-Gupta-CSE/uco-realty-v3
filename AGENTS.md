# Agents: read `.brain/` first

This repository's project memory is `.brain/` (git-ignored). Same rules for every agent:

1. **Read first:** `.brain/START_HERE.md` → `.brain/AGENT_RULES.md` → `.brain/01_STATE/CURRENT_STATE.md` + `NEXT_STEPS.md` → last 3 entries of `.brain/01_STATE/SESSION_LOG.md`.
2. **Write back before your final message** of any response that changed something or learned something durable: `CURRENT_STATE.md`, `NEXT_STEPS.md`, a new `SESSION_LOG.md` entry, decisions in `.brain/02_DECISIONS/`, and the `.brain/03_SYSTEM/` docs your change affects (checklist: `.brain/AGENT_RULES.md` §3).
3. The code is the truth; fix notes that disagree with it. Do not push, merge or delete branches unless the owner asks; commit only on request.

See `CLAUDE.md` for the short list of owner rules. If `.brain/` is absent, tell the owner.
