# 🌿 Piuu Launcher — Strict Branching Rules & Nicknames (`BRANCHING_RULES.md`)

> **MANDATORY DIRECTIVE FOR ALL AI AGENTS & DEVELOPERS**: This document establishes non-negotiable branching rules and mandatory nicknames for the Piuu Unified Launcher project. All agents MUST strictly adhere to these rules without exception.

---

## 🏷️ 1. Official Branch Nicknames

| Nickname | Target Git Branch | Purpose & Scope |
| :--- | :--- | :--- |
| **`piuu`** | **`main`** | **Production-Stable Baseline Launcher**: Standard Compose UI components, 4-column Android grid, 2D matrix widget resizing, app shortcut picker, and element context menu removal. |
| **`zen-piuu`** | **`master`** | **Extension Architecture & Core Planned Master**: Extension SDK runtime (`PiuuExtensionPackage`), Electron Desktop Extension Builder (`piuu-studio-desktop`), 1-Tap Theme Transformer Studio, POSIX C Native Core (`libpiuu_core.so`), and Master Architectural Specifications. |

---

## ⛔ 2. Strict Operating Rules for AI Agents

### Rule 1: Single-Branch Context Isolation (NEVER Mix Branches)
* **CRITICAL**: An AI Agent **MUST NOT** attempt to switch between, edit code for, merge, or push to both branches (`piuu` and `zen-piuu`) simultaneously in the same task or conversation turn.
* **Rationale**: Simultaneous dual-branch development causes local working tree pollution, merge conflicts, and invalidates background CI/CD workflow build triggers.

### Rule 2: Explicit Branch Target Confirmation
* Before making code modifications or running `git push`, the AI Agent **MUST** explicitly state or confirm which branch is being targeted:
  * *"Targeting branch `piuu` (`main`)..."* OR
  * *"Targeting branch `zen-piuu` (`master`)..."*
* If the user prompt is ambiguous regarding the target branch, the agent **MUST** ask the user for clarification before executing edits or pushing code.

### Rule 3: Single-Branch Release Alignment
* When pushing release tags or release builds (e.g. `v1.0.0-release`), verify that the tag points to a verified successful CI run on the intended branch segment without altering the parallel branch.

---

## 🛠️ 3. Standard Git Workflow for AI Agents

When working on a assigned task for a specific branch:

1. **Check Out Target Branch Only**:
   ```bash
   # For piuu task:
   git checkout main
   git pull origin main

   # For zen-piuu task:
   git checkout master
   git pull origin master
   ```

2. **Execute Edits & Verify Build**:
   * Perform changes exclusively within the single target branch workspace.
   * Run local lint checks or verify Kotlin syntax.

3. **Commit & Push to Target Remote**:
   ```bash
   # For piuu task:
   git add .
   git commit -m "fix/feat: <description>"
   git push origin main

   # For zen-piuu task:
   git add .
   git commit -m "fix/feat: <description>"
   git push origin master
   ```

4. **Monitor CI/CD Build Silently**:
   * Check status using `gh run list --limit 3`.
