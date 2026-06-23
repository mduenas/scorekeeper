# Scorekeeper — Claude Code Instructions

## Project Overview

Kotlin Multiplatform (KMP) app targeting Android and iOS using Compose Multiplatform. Architecture: MVVM with shared `commonMain` code, platform-specific implementations in `androidMain` and `iosMain`.

Key directories:
- `composeApp/src/commonMain/` — shared business logic, models, viewmodels, screens
- `composeApp/src/androidMain/` — Android platform implementations
- `composeApp/src/iosMain/` — iOS platform implementations
- `iosApp/` — Xcode project wrapper

---

## CodeGraph — Code Exploration

This project has a CodeGraph index. **Use codegraph tools instead of grep/find for symbol lookups.**

| When you need to... | Use this tool |
|---|---|
| Find a class, function, or property by name | `codegraph_search` |
| Get context relevant to a task | `codegraph_context` |
| See what calls a function | `codegraph_callers` |
| See what a function calls | `codegraph_callees` |
| Understand the blast radius of a change | `codegraph_impact` |
| Read a symbol's full source + metadata | `codegraph_node` |
| Check index health | `codegraph_status` |

**Rules:**
- Always try `codegraph_search` before opening files to locate a symbol.
- Before editing a function, run `codegraph_impact` to understand what else might break.
- The index auto-updates on every git commit via a post-commit hook. If you add files outside of a commit, run `codegraph sync` manually.

---

## SimpleMem — Long-Term Memory

This project has a dedicated SimpleMem memory user (isolated from other projects). The MCP server runs locally on port 8765 using `qwen2.5:7b` for inference and `nomic-embed-text` for embeddings — no cloud APIs involved.

| When you need to... | Use this tool |
|---|---|
| Store a decision, finding, or context | `memory_add` |
| Recall past decisions or context | `memory_query` |
| Browse stored facts | `memory_retrieve` |
| Check memory health | `memory_stats` |

**When to store memories:**
- Architecture decisions and the reasoning behind them
- Bugs found and how they were fixed
- Non-obvious patterns or constraints in the codebase
- Anything you'd have to re-derive next session

**When to query memory:**
- At the start of a session before making changes to an area you've worked in before
- When a bug or behavior seems familiar
- Before adding something that might already exist

**Rules:**
- Query before you search: `memory_query` first, then `codegraph_search` if you need code.
- Store decisions at end of significant work, not just facts already in the code.
- If the SimpleMem server is down, start it: `launchctl load ~/Library/LaunchAgents/com.markduenas.simplemem.plist`
