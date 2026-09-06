# Bonsai Saplings - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side. Vanilla clients need nothing. Version targets live in `gradle.properties`
(Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; shears, breaking, and catching a pot up |
| `Bonsai.java` | Keeping what is in a pot and what stands in it saying the same thing |
| `BonsaiShape.java` | The forms a little tree can take |
| `BonsaiSpecies.java` | What a potted sapling is made of once it has grown up |
| `BonsaiTree.java` | The little tree standing in the pot |
| `mixin/FlowerPotMixin.java` | Noticing a pot's contents changing, and making a pot with a tree in it act full |
| `mixin/FlowerPotAccessor.java` | Which blocks a pot will take, which vanilla keeps to itself |
