# TerraFirmaMon fork (1.20.1, `tfm-1.20.1` branch)

A small fork of Create: Bits 'n' Bobs 0.0.41 for the [TerraFirmaMon](https://github.com/n0es/terrafirmamon)
modpack, which only uses the cogwheel chain drive. TerraFirmaGreg replaces vanilla chain and Create's
cogwheels, so the stock chain drive had nothing to attach to. Released as `0.0.41-tfm.N`.

## Changes

- **Chains from a tag.** Any item in `bits_n_bobs:cogwheel_chains` (default: `minecraft:chain`) builds a
  chain drive. The loop remembers which item it was built from, refunds that item and draws its texture
  (`<ns>:textures/block/<path>.png`, vanilla's UV layout).
- **Addon cogwheels from a tag.** Cogwheels in `bits_n_bobs:chain_drive_cogwheels`, bare or encased, are
  swapped for generic chain cogwheels whose block entity records the original block state, so they
  render, collide, connect, drop and revert as the original.
- **Attached blocks.** Blocks in `bits_n_bobs:chain_drive_attachments` (e.g. mechanical pumps) join a
  loop without being replaced, so they keep working; `RotationPropagatorChainMixin` links them in. A loop
  needs at least one real cogwheel.
- **Greate compat** (optional at runtime): chain cogwheels report the replaced cogwheel's tier, so Greate
  still caps network capacity, and render Greate's per-material models.
- **Wrench on the chain**, like Create's chain conveyor: holding a wrench outlines the chain segment you
  look at; sneak + use takes the loop down and returns the chains.
- **Placement preview** shows the real path the chain would take, not a line pair per cogwheel pair.
- Chains are only consumed once the path is validated.

Network protocol version is 4, so it won't connect to stock 0.0.41.

## Building

`./gradlew build` with JDK 17. Greate and GTCEu come from the Modrinth maven as compile-only dependencies.

Licensed MIT, like upstream (Cake, Kipti, NormalGuy, Astral, Spydnel).
