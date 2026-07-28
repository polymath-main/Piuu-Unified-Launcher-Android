# Custom Agent Instructions for Piuu Unified Launcher

## 1. Automated Tracking and Memory (MANDATORY)
- **Session & Change Updates:** At the end of every session, or whenever major architectural or feature changes happen, you MUST update this `AGENTS.md` file (or a designated changelog) to record the current state of the project, new features implemented, and any new project-specific rules or context learned.
- **Maintain Context:** Use this file to persist long-term memory across sessions. If you learn something new about the user's preferences, project structure, or encounter bugs that needed specific fixes, document it here so future agent sessions are aware.

## 2. Project Context & Current Memories
- **Name:** Piuu-Unified-Launcher-Android
- **Stack:** Android, Kotlin, Jetpack Compose, Material Design 3.
- **Recent History Note:** The project previously experienced a severe Git index corruption and was successfully restored via a hard reset from the GitHub origin (`https://github.com/polymath-main/Piuu-Unified-Launcher-Android.git`). The local Git index may still exhibit corruption (`fatal: unknown index entry format`, `non-monotonic index`), so be cautious when using Git commands locally.
- **Session Memory:** In the current session, the workspace was corrupted, resulting in lost features. The codebase was restored from the GitHub remote to bring back the missing implementations. All conversation history for this recovery session was exported to `docs/conversation_history.md`.

## 3. Codebase Features & Architecture
- **Floating Overlay Service:** Implements an overlay service and associated user preferences.
- **App Drawer:** Features customization, app listing, and performance optimizations.
- **Marketplace SDK Bridge:** Includes a bridge for marketplace integrations and extended app features. Now dynamically renders theme color previews (swatches) parsing JSON payloads.
- **Metrics Dashboard Refinement:** Replaced static dummy data (screen time, launches, app progress) with dynamically calculated metrics derived from real application usage.
- **Search System Actions:** Wired up `systemActions` so they properly execute actions like opening the Marketplace, Metrics Dashboard, and toggling Wi-Fi state when tapped in the search overlay.
- **AI-Powered Global Search:** Interconnected the search bar query listener to a background Gemini model integration in `AiEngine`, which processes the input intent and seamlessly updates `appResults` in the search overlay (native). We also brought this same async Gemini suggest mechanism over to the Web-based HybridWebView frontend, combining semantic matches and lexical matches seamlessly.
- **Animations:** Added `Modifier.animateItemPlacement` (Compose Foundation) for native list interactions. Over in the web UI, injected a flutter-smooth `springIn` `@keyframes` animation for `.app-item` appearance and scaled `.sortable-ghost` items slightly (0.95x) when dragged.
- **Theming & UI:** Comprehensive theme support including dynamic dark mode, Material 3 layouts, and launcher-specific UI components (e.g., home screen grids, dashboard modals).
- **Architecture:** Follows modern Android development practices (Kotlin Coroutines, Flow, ViewModels, Room for persistence if applicable).

## 4. General Workflow Rules
- Always ensure changes are stable and compile successfully before concluding a task.
- Maintain strict Material 3 design guidelines and the established app architecture.
- Do not modify or remove existing features unless explicitly requested.
- Prioritize reading this document and `docs/conversation_history.md` when initiating new sessions to understand the project's background.
