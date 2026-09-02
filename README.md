# Bonsai Saplings

A Fabric mod that turns a sapling in a flower pot into a little tree.

## What This Mod Does

Put a sapling in a pot and it grows, at pot size. Cut it back with shears and it comes up as a
different shape.

## The Shapes

Not a vanilla tree shrunk down. A vanilla tree is shaped by what a tree does when nobody is
managing it, and shrinking one gives you a small unmanaged tree, which is a shrub. These are shaped
the way a bonsai is shaped - by somebody deciding where the trunk leans and where the foliage is
allowed to sit - which is why they read as deliberate at a size where a real oak reads as a blob.

Six forms, near enough the classical ones:

- **Upright** - straight, foliage in tiers.
- **Curved** - a trunk that changes its mind twice on the way up.
- **Leaning** - the weight carried out over the lean.
- **Cascade** - over the edge and down past the rim.
- **Windswept** - everything blown to one side.
- **Twin** - two trunks off one root, one clearly the elder.

Each is turned a random quarter turn, and shears always pick a form other than the one it is
wearing, so a cut always shows a change.

The wood and leaves are the species' own, worked out from the potted block's name: a potted birch
sapling grows birch. A wood set added next version needs no edit here. Mangrove, azalea and
flowering azalea are named individually, being the three that do not follow the rule.

## The Creeper

A creeper has always been half topiary: a green thing on four legs that turns up in a garden
uninvited. **Right-click an empty flower pot with a creeper spawn egg** and you get the shape
without the consequences - four feet, a body, a head sitting proud of it, clipped out of moss with
dark prismarine for the feet and the face.

An empty pot only. A pot with a tree in it is somebody's tree, and quietly replacing it is not what
anybody holding a spawn egg over it meant.

It is never rolled at random and shears never turn a tree into one. A creeper you did not plant is
a surprise of the wrong kind.

## How It Is Drawn

Block displays, one per block of the form. A display carries a full transformation and a block does
not, and the whole point is a tree at a fraction of block size - nothing actually made of blocks can
be smaller than one.

Those displays are also the only record. Nothing stores which pots have trees in them; the trees
sitting in them do. Vanilla clients see everything, because a block display is vanilla.

## Catching Up

A pot filled before this was installed, or one whose tree somebody removed, grows one the first
time a player reaches for it. There is deliberately no chunk-load sweep: planting means spawning
entities and asking what entities are nearby, and neither belongs anywhere near chunk loading.

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
| `mixin/FlowerPotMixin.java` | Noticing a pot's contents changing, in either direction |

## License

MIT, see [LICENSE](LICENSE).
