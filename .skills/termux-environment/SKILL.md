---
name: termux-environment
description: Guidelines, path resolution, package management, and non-interactive Git authentication setup for Android Termux environments.
---

# 🤖 Termux Environment & Git Authentication Skill

This skill provides comprehensive instructions for operating within an Android Termux environment, managing paths, handling package installations, and executing non-interactive Git credentials setup.

---

## 🔑 1. Non-Interactive Git Credential Management

When performing `git push` or `git fetch` operations in Termux non-interactive shells (such as AI background jobs or automated sub-processes):

1. **Disable Terminal Prompts**:
   ```bash
   export GIT_TERMINAL_PROMPT=0
   ```
2. **Configure GitHub CLI Credential Helper**:
   ```bash
   git config --global credential.helper "!gh auth git-credential"
   ```
3. **Run GitHub CLI Git Setup**:
   ```bash
   gh auth setup-git
   ```

---

## 📂 2. Termux Path Resolution & Guidelines

- **Package Manager & Binaries**: `/data/data/com.termux/files/usr/bin/`
- **User Home Directory**: `/data/data/com.termux/files/home/`
- **Shell Interpreters**: Use `#!/bin/sh` or `/data/data/com.termux/files/usr/bin/bash`. (Avoid `/usr/bin/env bash` on Termux unless `termux-fix-shebang` has been run).
- **Shebang Repair Tool**: Run `termux-fix-shebang <script_path>` when executing scripts created on Linux/macOS.

---

## 🌿 3. Project Branching & Operating Rules

- **`piuu`** (`main` Branch): Production-stable baseline launcher.
- **`zen-piuu`** (`master` Branch): Extension architecture & core planned master branch.
- **No Auto-Push Rule**: Keep code edits local until explicit push instruction from user. Workflow builds are manual-dispatch (`workflow_dispatch`) only.
