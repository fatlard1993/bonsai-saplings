# Bonsai Saplings

A Fabric mod that turns a sapling in a flower pot into a little tree.

## What This Mod Does

Put a sapling in a pot and it grows, at pot size. Sneak and cut it back with shears and it comes up as a
different shape.

The sapling itself goes out of sight once the tree is up. A potted sapling is a block that draws a
sapling, and a sapling poking up through a trunk is the one thing that stops a bonsai reading as a
tree, so the pot is emptied and the tree becomes the record of what was planted. It behaves like a
full pot all the same: a plant offered to it is refused, an empty hand takes the tree out and hands
the sapling back, and breaking the pot drops the sapling with it.

## The Shapes

Three for each kind of sapling, drawn after the tree that sapling grows into. A birch is tall and
thin with a small head high up; a spruce is a cone; a jungle tree is a bare trunk with everything at
the top; an acacia is a flat pad on a bent trunk, or two pads; a dark oak is squat under a head far
wider than its trunk; a mangrove stands up on its roots; a cherry hangs a wide head past its own
trunk; an azalea is a bush with hardly any trunk. Pale oak grows like dark oak, flowering azalea
like azalea, and a sapling from another mod that nobody has drawn for grows like an oak.

Chunky on purpose. A cell is a sixth of a block and the soil in a pot is a quarter, so there is no
room at this size for the finesse of a real bonsai. These are drawn the way Minecraft draws each
tree, in blobs that read as that tree from across a room, and nothing goes below the soil, because
the pot is standing on something.

**Sneaking shears step through them in order**: the four quarter turns of the form the tree is
wearing, then the next form, then round again. A tree that should face the window can be turned to
face it and left there; twelve cuts bring it back to where it started. A pot grown fresh starts on a
form and a turn chosen at random.

The wood and leaves are the species' own, worked out from the potted block's name: a potted birch
sapling grows birch. A wood set added next version needs no edit here. Poplar, mangrove, azalea and
flowering azalea are named individually, being the four that do not follow the rule.

With [better-trees](../better-trees) on the server the canopy is finished the way it finishes a
grown tree: the outermost leaves become that species' leaf stairs, stepping down and outward,
with the underside turned over and the corners cut. The rule is theirs and the blocks are looked
up by name, so nothing here depends on it, and a world without it grows the same trees in cubes.

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

## Development

Installing and the map of the source are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
