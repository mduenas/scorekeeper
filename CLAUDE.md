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

<!-- rtk-instructions v2 -->
# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk cargo build         # Cargo build output
rtk cargo check         # Cargo check output
rtk cargo clippy        # Clippy warnings grouped by file (80%)
rtk tsc                 # TypeScript errors grouped by file/code (83%)
rtk lint                # ESLint/Biome violations grouped (84%)
rtk prettier --check    # Files needing format only (70%)
rtk next build          # Next.js build with route metrics (87%)
```

### Test (60-99% savings)
```bash
rtk cargo test          # Cargo test failures only (90%)
rtk go test             # Go test failures only (90%)
rtk jest                # Jest failures only (99.5%)
rtk vitest              # Vitest failures only (99.5%)
rtk playwright test     # Playwright failures only (94%)
rtk pytest              # Python test failures only (90%)
rtk rake test           # Ruby test failures only (90%)
rtk rspec               # RSpec test failures only (60%)
rtk test <cmd>          # Generic test wrapper - failures only
```

### Git (59-80% savings)
```bash
rtk git status          # Compact status
rtk git log             # Compact log (works with all git flags)
rtk git diff            # Compact diff (80%)
rtk git show            # Compact show (80%)
rtk git add             # Ultra-compact confirmations (59%)
rtk git commit          # Ultra-compact confirmations (59%)
rtk git push            # Ultra-compact confirmations
rtk git pull            # Ultra-compact confirmations
rtk git branch          # Compact branch list
rtk git fetch           # Compact fetch
rtk git stash           # Compact stash
rtk git worktree        # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>    # Compact PR view (87%)
rtk gh pr checks        # Compact PR checks (79%)
rtk gh run list         # Compact workflow runs (82%)
rtk gh issue list       # Compact issue list (80%)
rtk gh api              # Compact API responses (26%)
```

### JavaScript/TypeScript Tooling (70-90% savings)
```bash
rtk pnpm list           # Compact dependency tree (70%)
rtk pnpm outdated       # Compact outdated packages (80%)
rtk pnpm install        # Compact install output (90%)
rtk npm run <script>    # Compact npm script output
rtk npx <cmd>           # Compact npx command output
rtk prisma              # Prisma without ASCII art (88%)
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>           # Tree format, compact (65%)
rtk read <file>         # Code reading with filtering (60%)
rtk grep <pattern>      # Search grouped by file (75%). Format flags (-c, -l, -L, -o, -Z) run raw.
rtk find <pattern>      # Find grouped by directory (70%)
```

### Analysis & Debug (70-90% savings)
```bash
rtk err <cmd>           # Filter errors only from any command
rtk log <file>          # Deduplicated logs with counts
rtk json <file>         # JSON structure without values
rtk deps                # Dependency overview
rtk env                 # Environment variables compact
rtk summary <cmd>       # Smart summary of command output
rtk diff                # Ultra-compact diffs
```

### Infrastructure (85% savings)
```bash
rtk docker ps           # Compact container list
rtk docker images       # Compact image list
rtk docker logs <c>     # Deduplicated logs
rtk kubectl get         # Compact resource list
rtk kubectl logs        # Deduplicated pod logs
```

### Network (65-70% savings)
```bash
rtk curl <url>          # Compact HTTP responses (70%)
rtk wget <url>          # Compact download output (65%)
```

### Meta Commands
```bash
rtk gain                # View token savings statistics
rtk gain --history      # View command history with savings
rtk discover            # Analyze Claude Code sessions for missed RTK usage
rtk proxy <cmd>         # Run command without filtering (for debugging)
rtk init                # Add RTK instructions to CLAUDE.md
rtk init --global       # Add RTK to ~/.claude/CLAUDE.md
```

## Token Savings Overview

| Category | Commands | Typical Savings |
|----------|----------|-----------------|
| Tests | vitest, playwright, cargo test | 90-99% |
| Build | next, tsc, lint, prettier | 70-87% |
| Git | status, log, diff, add, commit | 59-80% |
| GitHub | gh pr, gh run, gh issue | 26-87% |
| Package Managers | pnpm, npm, npx | 70-90% |
| Files | ls, read, grep, find | 60-75% |
| Infrastructure | docker, kubectl | 85% |
| Network | curl, wget | 65-70% |

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->