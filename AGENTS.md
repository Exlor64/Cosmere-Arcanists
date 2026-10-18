# Cosmere Arcanists development instructions

## Start of each project session

- Read `HANDOFF.md` before changing code or making design decisions. Check the actual source and current GitHub state for details that may have changed.
- Target Minecraft 1.21.1, NeoForge 21.1.256, Java 21. Author is Ex.
- Preserve the user's accepted decisions in the handoff. Do not silently substitute real-world metallurgy for the agreed Cosmere recipes.

## Handoff maintenance — user requirement

The user explicitly requested a maintained handoff after every change or project chat, to support future chats and consistent development.

- Update `HANDOFF.md` as part of every meaningful code, resource, configuration, command, or design change, and before finishing a project chat.
- For a discussion-only chat, record new decisions, unresolved questions, and the next action. If nothing changed, refresh the latest-session entry with a brief status rather than inventing progress.
- Keep the document an accurate current-state summary. Replace stale sections; do not accumulate conflicting historical instructions or repeat the entire conversation.
- Include the current version/date, implemented versus planned features, user constraints, controls, relevant configuration, migrations/network compatibility, verification evidence, known limitations, and outstanding work.
- Record tests actually run and their results. Separate automated checks, user-reported behavior, and untested client/multiplayer behavior. Never describe pending checks as passed.
- Include handoff updates in the same commit as changes when practical. A later verification-only handoff update can record the final tested commit/run without creating an endless self-referencing commit cycle.
- Keep `README.md` consistent with current user-facing behavior. It may contain historical release notes; `HANDOFF.md` must identify what is current.

## Implementation and validation

- Powers are persistent player attributes, not tags or legacy grant booleans. Existing data must migrate without regranting a power that was set to zero.
- Keep common code independent of client-only classes. Validate powers, targets, fuel, and forces on the server.
- Preserve aluminum exclusion, configurable feature gates, no forced chunk loading, and saved data for disabled features.
- Avoid regressing additive client impulses by replacing client velocity with measured walking displacement.
- Use tests appropriate to the change. Java 21 commands: `./gradlew build --no-daemon` and `./gradlew runGameTestServer --no-daemon` (Windows: `gradlew.bat`). Documentation-only changes need consistency/link/diff checks, not another full game build.
- User has authorized editing and pushing this repository. Follow any newer user directions about branches or publication.
