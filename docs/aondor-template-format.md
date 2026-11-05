# AonDor drawing test

Only **Light, Force and Heat** ship in this test. Their main outlines stay angular,
with small circular marks. Exact shapes and stroke starts/directions:

![Three Aon stroke guide](aondor-test-guide.svg)

## Trying the interface

Grant development access as an operator:

```mcfunction
/attribute <player> cosmerearcanists:aondor_strength base set 1
```

Select AonDor, then hold primary use-power (G) or secondary (H). Left-drag
inside the large central tile; strokes and previews have a soft white halo and bright white core; each mouse release ends a stroke. Use **Lock Aon**
for authoritative server recognition. Successful recognition locks the drawing,
shows its name, recognition mode and potency. Further drawing cannot change it.
**Edit Aon** unlocks; **Reset** clears; **Undo / Ctrl+Z** reverses strokes, moving,
confirmed placement, locking, unlocking and reset (up to 100 edits per screen).
An unfinished stroke or placement is cancelled first. Drag the unlocked tile title
to reposition it. Releasing the power key closes without casting.

A matching luminous projection floats1.5blocks ahead of the player while the screen
is open. The artist sees ink update each frame; tracking players receive snapshots
at most4times/second. It follows facing, respects terrain depth, and disappears on
close (remote timeout30ticks). It is cosmetic and emits no actual world light.

Recognition normalizes translation and uniform scale, preserving proportions and
orientation. The precise pass checks stroke order/direction, path, endpoints,
corners and relative lengths. A separate symmetric combined-ink comparison ignores
order, direction and pen lifts. Clear shape-only matches show **75% potency** by
default; precise matches show **100%**. Shape ambiguity is rejected even if stroke
order favors one candidate. Potency is a test readout: no live spell is executed.

A clear recognized drawing unlocks the server-owned learned palette, including a
shape-only match. Scroll previews learned names/shapes; right-click anchors a
shape, mouse distance controls size, left-click confirms and requests recognition.
Right-click/Escape cancels unfinished placement. Proper palette templates give
normal potency. Locked drawings must be unlocked before replacement.

Knowledge survives saves/relogs/death. Draft drawings and lock state survive
closing/reopening in the same client/player/dimension; undo history is per screen,
not persisted. Unknown patterns cannot be fetched as palette stamps. No modifiers
are exposed; server validation rejects argument-bearing drawings for this test.
Future typed/nested clockwise expressions, standalone default effects, item/block
inscriptions and Elantrian regional acquisition remain planned.

Config `cosmerearcanists-aondor-server.toml` includes ordered recognition metrics,
`maximumShapeError=.065`, `minimumShapeGap=.018`, `shapeOnlyPotency=.75`.
Changing config in existing worlds follows normal server-config rules.

## Defining simplified Aons

Future templates should specify numbered strokes and arrows. Templates
belong in a datapack at:

```text
data/<namespace>/aondor/aons/<name>.json
```

The resource path becomes the Aon's ID. The following is a **format example only**,
not a real or approved Aon; it is deliberately outside runtime resources:

```json
{
  "name": "Format example only",
  "kind": "argument",
  "strokes": [
    [[0.0, 0.0], [1.0, 0.0]],
    [[1.0, 0.0], [1.0, 1.0]]
  ]
}
```

`kind` is `effect` or `argument`. Coordinates are normalized inside the symbol's
own drawing area: x increases rightward, y downward. Array order is stroke order;
point order is drawing direction. Recognition tolerates translation, uniform size
changes and input sample density. Precise recognition preserves stroke order and
direction; shape fallback ignores both and pen lifts. Neither pass auto-rotates
or mirrors the drawing. Aspect ratio is preserved. Ambiguous matches are rejected.

Templates reload with datapacks. Each check is limited to17glyphs (one effect plus
16arguments),16strokes per glyph,48transmitted points per stroke, and3000points total.
The UI resamples completed strokes to24points, preserving order/direction. The server
loads at most256templates. These are implementation limits, not final language rules.

Server settings: `cosmerearcanists-aondor-server.toml`, including enablement,
maximumRecognitionError(default0.12), minimumRecognitionGap(default0.025),
minimumGlyphExtent(default0.01), pathWeight(0.55), endpointWeight(0.25),
cornerWeight(0.1) and relativeStrokeLengthWeight(0.1). Uniform drawing size is
normalized away; relative stroke lengths, endpoints and signed turns help
distinguish similar shapes while retaining order/direction. Known IDs are bounded
to1024per player; at most256active definitions appear in a library snapshot.
Matching client/server protocol38 is required. Sel's acquisition, regional power
attenuation, live effect evaluator, argument types, costs and cross-world behavior
remain unimplemented and must be designed separately.
