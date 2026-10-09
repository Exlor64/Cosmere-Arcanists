# Project handoff
Agreed scope: large Cosmere mod; Minecraft 1.21.1 NeoForge; author Ex. Overworld is a separate planet. Start with Era 2 Scadrial in a normal Overworld-like environment and its three Metallic Arts. Starting powers random by default; support choice and ancestry modes. Every major implemented feature must have disable/behavior controls.

Initial foundation is command-based, server-authoritative, and stores origin data in namespaced player persistent NBT with schema 1. Clone event copies data after death/end return. Natural powers and later Hemalurgic grants must remain separate. No client dependencies in common code.

Only human origins currently exist. Ancestry uses a configurable Scadrian-human multiplier, not a genealogical simulation. Preserve unknown saved identifiers and disabled-feature data. Future ability access must apply config gates dynamically.

Build verification: local Gradle download blocked by execution environment network restrictions; GitHub Actions builds with JDK 21. Check CI before considering this foundation compiled. No in-game test performed.

Next: verify CI, then implement origin UI/networking and meaningful automated assignment/persistence tests before magic effects. See README for commands and current limitations.
