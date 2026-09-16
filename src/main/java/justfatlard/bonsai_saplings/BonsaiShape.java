package justfatlard.bonsai_saplings;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;


/**
 * The forms a little tree can take: three for each kind of sapling, drawn after the tree that
 * sapling grows into.
 *
 * <p>Chunky on purpose. A cell is a sixth of a block and the soil in a pot is a quarter, so
 * there is no room here for the finesse of a real bonsai, and forms drawn to imitate one came
 * out as a log or two stuck to a pot. These are drawn the way Minecraft draws each tree - a
 * birch is tall and thin, a spruce a cone, an acacia a flat pad on a bent trunk, a mangrove up
 * on its roots - in blobs that read as that tree from across a room. Coordinates are in cells
 * around the pot's middle, with y counting up from the soil; nothing goes below the soil,
 * because the pot is standing on something.
 *
 * <p>Sneaking shears step through them in a fixed order rather than at random: the four quarter
 * turns of one form, then the next form. A player who wants the tree facing the window can get
 * there and stop, and a player who wants a different tree goes round four times.
 */
public record BonsaiShape(String name, List<Cell> cells) {
	/** One block of the little tree: where it sits, and whether it is wood or foliage. */
	public record Cell(int x, int y, int z, boolean wood) {}

	private static Cell wood(int x, int y, int z) {
		return new Cell(x, y, z, true);
	}

	private static Cell leaf(int x, int y, int z) {
		return new Cell(x, y, z, false);
	}

	/** A straight trunk from the soil, {@code height} cells tall. */
	private static List<Cell> trunk(int height) {
		List<Cell> cells = new java.util.ArrayList<>();
		for (int y = 0; y < height; y++) cells.add(wood(0, y, 0));
		return cells;
	}

	/** The four neighbours of the centre at a height. */
	private static List<Cell> cross(int y) {
		return List.of(leaf(1, y, 0), leaf(-1, y, 0), leaf(0, y, 1), leaf(0, y, -1));
	}

	/** The four corners at a height. */
	private static List<Cell> corners(int y) {
		return List.of(leaf(1, y, 1), leaf(-1, y, 1), leaf(1, y, -1), leaf(-1, y, -1));
	}

	/** The centre and everything around it: a full three by three. */
	private static List<Cell> disc(int y) {
		return cells(leaf(0, y, 0), cross(y), corners(y));
	}

	/** Two cells out in each direction, for a head wider than the pot. */
	private static List<Cell> reach(int y) {
		return List.of(leaf(2, y, 0), leaf(-2, y, 0), leaf(0, y, 2), leaf(0, y, -2));
	}

	/** The same cells moved sideways, for a second stem or cane beside the first. */
	private static List<Cell> shifted(int dx, int dz, Object... parts) {
		List<Cell> out = new java.util.ArrayList<>();
		for (Cell cell : cells(parts)) out.add(new Cell(cell.x() + dx, cell.y(), cell.z() + dz, cell.wood()));
		return out;
	}

	/** A cane: a column of bamboo with fronds at the top, {@code height} cells tall. */
	private static List<Cell> cane(int x, int z, int height) {
		List<Cell> out = new java.util.ArrayList<>();
		for (int y = 0; y < height - 1; y++) out.add(wood(x, y, z));
		out.add(leaf(x, height - 1, z));
		return out;
	}

	/** A square of cells {@code r} out from the centre at a height, wood or foliage: a pad or a bed. */
	private static List<Cell> square(int r, int y, boolean wood) {
		List<Cell> out = new java.util.ArrayList<>();
		for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) out.add(new Cell(x, y, z, wood));
		return out;
	}

	/** Flattens cells and lists of cells into one list. */
	@SuppressWarnings("unchecked")
	private static List<Cell> cells(Object... parts) {
		List<Cell> out = new java.util.ArrayList<>();
		for (Object part : parts) {
			if (part instanceof Cell cell) out.add(cell);
			else out.addAll((List<Cell>) part);
		}
		return out;
	}

	/**
	 * A form from its parts. One cell per place, wood over foliage where both were asked for -
	 * a crown drawn round a trunk would otherwise put a leaf inside every block of it - and
	 * nothing below the soil, because the pot is standing on something.
	 */
	private static BonsaiShape form(String name, Object... parts) {
		return hanging(name, 0, parts);
	}

	/**
	 * A form that may hang below the soil, as far as {@code deepest}: a trailing plant spills over
	 * the rim and down the outside of its pot. Only as far as the pot is tall, so a strand never
	 * goes through whatever the pot is standing on.
	 */
	private static BonsaiShape hanging(String name, int deepest, Object... parts) {
		Map<Long, Cell> at = new java.util.LinkedHashMap<>();
		for (Cell cell : cells(parts)) {
			if (cell.y() < deepest) continue;
			long key = BlockPos.asLong(cell.x(), cell.y(), cell.z());
			Cell there = at.get(key);
			if (there == null || (cell.wood() && !there.wood())) at.put(key, cell);
		}
		return new BonsaiShape(name, List.copyOf(at.values()));
	}

	// Builders for the larger forms, which are too big to lay out a cell at a time.

	/** Wood straight up from a cell, {@code height} cells tall. */
	private static List<Cell> post(int x, int y, int z, int height) {
		List<Cell> out = new java.util.ArrayList<>();
		for (int i = 0; i < height; i++) out.add(wood(x, y + i, z));
		return out;
	}

	/** Wood from one cell to another, a cell at a time: a branch, or a trunk that bends. */
	private static List<Cell> branch(int x0, int y0, int z0, int x1, int y1, int z1) {
		List<Cell> out = new java.util.ArrayList<>();
		int steps = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
		for (int i = 0; i <= steps; i++) {
			double t = steps == 0 ? 0 : i / (double) steps;
			out.add(wood((int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t),
				(int) Math.round(z0 + (z1 - z0) * t)));
		}
		return out;
	}

	/**
	 * Foliage round a centre, the shell of an ellipsoid: a crown. Only the outside of it, since
	 * a cell with foliage on every side is never seen and costs two displays all the same.
	 */
	private static List<Cell> crown(double cx, double cy, double cz, double rx, double ry, double rz) {
		java.util.function.Predicate<int[]> inside = c -> {
			double dx = (c[0] - cx) / rx, dy = (c[1] - cy) / ry, dz = (c[2] - cz) / rz;
			return dx * dx + dy * dy + dz * dz <= 1.0;
		};
		return shell(inside, (int) Math.floor(cx - rx), (int) Math.ceil(cx + rx),
			(int) Math.floor(cy - ry), (int) Math.ceil(cy + ry), (int) Math.floor(cz - rz), (int) Math.ceil(cz + rz));
	}

	/** Foliage in a cone about the trunk, {@code base} across at the bottom and a point at the top. */
	private static List<Cell> cone(int from, int to, double base) {
		java.util.function.Predicate<int[]> inside = c -> {
			if (c[1] < from || c[1] > to) return false;
			double r = 0.6 + (base - 0.6) * (to - c[1]) / (double) Math.max(1, to - from);
			return c[0] * c[0] + c[2] * c[2] <= r * r;
		};
		int reach = (int) Math.ceil(base);
		return shell(inside, -reach, reach, from, to, -reach, reach);
	}

	/** A flat round pad of foliage one cell thick. */
	private static List<Cell> pad(double cx, int y, double cz, double r) {
		List<Cell> out = new java.util.ArrayList<>();
		for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
			for (int z = (int) Math.floor(cz - r); z <= Math.ceil(cz + r); z++) {
				if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r) out.add(leaf(x, y, z));
			}
		}
		return out;
	}

	private static List<Cell> shell(java.util.function.Predicate<int[]> inside,
			int x0, int x1, int y0, int y1, int z0, int z1) {
		List<Cell> out = new java.util.ArrayList<>();
		int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
			if (!inside.test(new int[] {x, y, z})) continue;
			for (int[] d : around) {
				if (!inside.test(new int[] {x + d[0], y + d[1], z + d[2]})) {
					out.add(leaf(x, y, z));
					break;
				}
			}
		}
		return out;
	}

	/** A strand of foliage hanging from a cell, {@code length} cells down. */
	private static List<Cell> strand(int x, int top, int z, int length) {
		List<Cell> out = new java.util.ArrayList<>();
		for (int i = 0; i < length; i++) out.add(leaf(x, top - i, z));
		return out;
	}

	/** A tall flower: {@code stalks} of stalk and the bloom on top. */
	private static List<Cell> flower(int x, int z, int stalks) {
		List<Cell> out = new java.util.ArrayList<>(post(x, 0, z, stalks));
		out.add(leaf(x, stalks, z));
		return out;
	}

	// Oak: a trunk and a round head, the tree everybody knows.
	private static final List<BonsaiShape> OAK = List.of(
		form("oak_round", trunk(3), cross(2), disc(3), leaf(0, 4, 0), leaf(2, 3, 0), leaf(1, 4, 0)),
		form("oak_branching", trunk(3), wood(1, 2, 0), wood(2, 2, 0),
			cross(3), leaf(0, 4, 0), leaf(1, 4, 0),
			leaf(2, 3, 0), leaf(3, 2, 0), leaf(2, 3, 1), leaf(2, 3, -1), leaf(3, 3, 0)),
		form("oak_bushy", wood(0, 0, 0), wood(0, 1, 0), cross(1), corners(1), cross(2), leaf(0, 2, 0), leaf(0, 3, 0), leaf(2, 1, 0), leaf(1, 2, 1)));

	// Birch: tall, thin, and a small head high up.
	private static final List<BonsaiShape> BIRCH = List.of(
		form("birch_tall", trunk(4), cross(3), disc(4), leaf(0, 5, 0), leaf(1, 5, 0), leaf(2, 4, 0)),
		form("birch_slim", trunk(5), cross(4), leaf(0, 5, 0), leaf(1, 5, 0)),
		form("birch_leaning", wood(0, 0, 0), wood(0, 1, 0), wood(0, 2, 0), wood(1, 3, 0), wood(1, 4, 0),
			leaf(2, 4, 0), leaf(0, 4, 0), leaf(1, 4, 1), leaf(1, 4, -1), leaf(1, 5, 0), leaf(2, 5, 0), leaf(0, 3, 0)));

	// Spruce: a cone, wide at the foot and a point at the top.
	private static final List<BonsaiShape> SPRUCE = List.of(
		form("spruce_cone", trunk(4), cross(1), corners(1), cross(2), leaf(1, 3, 0), leaf(0, 3, -1),
			leaf(0, 4, 0), leaf(0, 5, 0)),
		form("spruce_pillar", trunk(5), cross(2), cross(4), leaf(0, 5, 0), leaf(0, 6, 0), leaf(1, 3, 0), leaf(0, 1, 1)),
		form("spruce_tuft", trunk(4), cross(4), leaf(0, 4, 0), leaf(0, 5, 0), leaf(1, 5, 0)));

	// Jungle: a long bare trunk with the leaves all at the top, or a bush when it is young.
	private static final List<BonsaiShape> JUNGLE = List.of(
		form("jungle_tall", trunk(4), disc(4), leaf(0, 5, 0), leaf(1, 5, 0), leaf(2, 4, 0)),
		form("jungle_tufted", trunk(4), cross(2), disc(4), leaf(0, 5, 0), leaf(1, 5, 0)),
		form("jungle_bush", wood(0, 0, 0), cross(0), disc(1), leaf(0, 2, 0), leaf(1, 2, 0)));

	// Acacia: a flat pad of leaves on a trunk that bends, sometimes two pads.
	private static final List<BonsaiShape> ACACIA = List.of(
		form("acacia_umbrella", trunk(3), disc(3), reach(3), leaf(0, 4, 0), leaf(1, 4, 0), leaf(0, 4, 1)),
		form("acacia_bent", wood(0, 0, 0), wood(0, 1, 0), wood(1, 2, 0), wood(2, 3, 0),
			leaf(1, 4, 0), leaf(2, 4, 0), leaf(3, 4, 0), leaf(2, 4, 1), leaf(2, 4, -1), leaf(3, 4, 1), leaf(1, 4, -1),
			leaf(2, 5, 0)),
		form("acacia_two_pads", wood(0, 0, 0), wood(0, 1, 0), wood(1, 2, 0), wood(-1, 2, 0), wood(-1, 3, 0),
			leaf(1, 3, 0), leaf(2, 3, 0), leaf(0, 3, 0), leaf(1, 3, 1), leaf(1, 3, -1),
			leaf(-1, 4, 0), leaf(-2, 4, 0), leaf(0, 4, 0), leaf(-1, 4, 1), leaf(-1, 4, -1)));

	// Dark oak and pale oak: squat, with a head far wider than the trunk.
	private static final List<BonsaiShape> DARK_OAK = List.of(
		form("dark_oak_broad", trunk(2), disc(2), reach(2), disc(3), leaf(0, 4, 0), leaf(1, 4, 0), leaf(2, 3, 0)),
		form("dark_oak_thick", trunk(3), wood(1, 1, 0), wood(1, 2, 0), disc(3), reach(3), leaf(0, 4, 0), leaf(1, 4, 0)),
		form("dark_oak_squat", trunk(2), cross(1), corners(1), disc(2), leaf(0, 3, 0), leaf(2, 1, 0), leaf(1, 3, 0)));

	// Mangrove: up on its roots, which is the whole point of a mangrove.
	private static final List<BonsaiShape> MANGROVE = List.of(
		form("mangrove_roots", wood(1, 0, 0), wood(-1, 0, 0), wood(0, 0, 1), wood(0, 0, -1),
			trunk(3), disc(3), leaf(0, 4, 0), wood(1, 1, 0), leaf(1, 4, 0)),
		form("mangrove_stilts", wood(1, 0, 1), wood(-1, 0, -1), wood(1, 0, -1), wood(-1, 0, 1),
			wood(0, 1, 0), wood(0, 2, 0), cross(3), leaf(0, 3, 0), leaf(0, 4, 0), leaf(1, 4, 0)),
		form("mangrove_leaning", wood(1, 0, 0), wood(-1, 0, 0), wood(0, 0, 1), wood(0, 0, -1),
			wood(0, 1, 0), wood(1, 2, 0), wood(1, 3, 0),
			leaf(2, 4, 0), leaf(0, 4, 0), leaf(1, 4, 1), leaf(1, 4, -1), leaf(1, 4, 0), leaf(1, 5, 0)));

	// Cherry: a wide, drooping head that hangs past its own trunk.
	private static final List<BonsaiShape> CHERRY = List.of(
		form("cherry_wide", trunk(3), disc(3), reach(3), leaf(2, 2, 0), leaf(-2, 2, 0), leaf(0, 4, 0), leaf(1, 4, 0), leaf(-1, 4, 0), leaf(0, 2, 2), leaf(1, 4, 1)),
		form("cherry_fork", wood(0, 0, 0), wood(0, 1, 0), wood(1, 2, 0), wood(-1, 2, 0), wood(1, 3, 0), wood(-1, 3, 0),
			disc(4), reach(4), leaf(0, 3, 0), leaf(2, 3, 0), leaf(-2, 3, 0), leaf(1, 5, 0), leaf(0, 3, -1)),
		form("cherry_weeping", trunk(3), disc(3), reach(3), leaf(2, 2, 0), leaf(-2, 2, 0), leaf(0, 2, 2), leaf(0, 2, -2),
			leaf(0, 4, 0), leaf(2, 1, 0), leaf(1, 4, 1)));

	// Azalea: a bush, flowering or not, low and wide and hardly any trunk.
	private static final List<BonsaiShape> AZALEA = List.of(
		form("azalea_bush", wood(0, 0, 0), disc(1), cross(2), leaf(0, 2, 0), leaf(2, 1, 0), leaf(1, 2, 1)),
		form("azalea_sprawl", wood(0, 0, 0), wood(1, 1, 0), cross(1), leaf(2, 1, 0), leaf(1, 1, 1),
			leaf(1, 2, 0), leaf(0, 2, 0), leaf(-1, 2, 0), leaf(0, 2, -1)),
		form("azalea_tall", trunk(2), disc(2), cross(3), leaf(0, 3, 0), leaf(0, 4, 0), leaf(1, 4, 0), leaf(2, 2, 0)));

	/** A poplar is a column: all height, the canopy no wider than a cross, tapering to a point. */
	private static final List<BonsaiShape> POPLAR = List.of(
		form("poplar_column", trunk(5), cross(2), cross(3), cross(4), leaf(0, 5, 0), leaf(0, 6, 0), leaf(1, 5, 0)),
		form("poplar_spire", trunk(6), cross(3), cross(5), leaf(0, 6, 0), leaf(0, 7, 0), leaf(-1, 4, 0), leaf(0, 2, 1),
			leaf(1, 4, 0)),
		form("poplar_windswept", trunk(3), wood(0, 3, 0), wood(1, 4, 0), leaf(1, 5, 0), leaf(1, 6, 0), leaf(2, 5, 0),
			leaf(2, 4, 0), leaf(1, 4, 1), leaf(1, 4, -1), leaf(0, 4, 0), leaf(-1, 3, 0), leaf(0, 3, -1)));

	/** Bamboo is canes, not a tree: a grove of them at different heights, a single tall one, or one bent over. */
	private static final List<BonsaiShape> BAMBOO = List.of(
		form("bamboo_grove", cane(0, 0, 6), cane(1, 1, 4), cane(-1, 0, 5), cane(0, -1, 3), cane(1, -1, 2)),
		form("bamboo_single", cane(0, 0, 8), leaf(0, 8, 0), leaf(1, 6, 0)),
		form("bamboo_leaning", trunk(3), wood(1, 3, 0), wood(1, 4, 0), wood(2, 5, 0), leaf(2, 6, 0), leaf(3, 6, 0),
			leaf(2, 7, 0), cane(-1, 0, 0), leaf(-1, 1, 0)));

	/** A mushroom is the huge one: a stem and a cap, either a toadstool, a flat plate, or two together. */
	private static final List<BonsaiShape> MUSHROOM = List.of(
		form("mushroom_toadstool", trunk(3), disc(3), reach(3), cross(4), leaf(0, 4, 0), leaf(0, 5, 0)),
		form("mushroom_flat", trunk(2), disc(2), reach(2), leaf(2, 2, 1), leaf(2, 2, -1), leaf(-2, 2, 1), leaf(-2, 2, -1),
			leaf(1, 2, 2), leaf(-1, 2, 2), leaf(1, 2, -2), leaf(-1, 2, -2), leaf(0, 3, 0)),
		form("mushroom_pair", trunk(2), cross(2), leaf(0, 2, 0), leaf(0, 3, 0),
			shifted(2, 0, wood(0, 0, 0), leaf(0, 1, 0), leaf(1, 1, 0), leaf(0, 1, 1), leaf(0, 1, -1))));

	/** The huge fungi of the nether: a tall stem under a cap of wart, straight, bent, or twinned. */
	private static final List<BonsaiShape> FUNGUS = List.of(
		form("fungus_huge", trunk(4), cross(3), disc(4), leaf(0, 5, 0), leaf(1, 5, 0)),
		form("fungus_bent", trunk(2), wood(1, 2, 0), wood(1, 3, 0), shifted(1, 0, cross(4), leaf(0, 4, 0), leaf(0, 5, 0)),
			leaf(0, 3, 0), leaf(-1, 1, 0)),
		form("fungus_twin", trunk(3), cross(3), leaf(0, 3, 0), leaf(0, 4, 0),
			shifted(2, 0, wood(0, 0, 0), wood(0, 1, 0), cross(2), leaf(0, 2, 0), leaf(0, 3, 0))));

	/** A cactus is a saguaro: the two-armed one, a plain column, or a cluster of barrels. All of it cactus. */
	private static final List<BonsaiShape> CACTUS = List.of(
		form("cactus_saguaro", trunk(6), wood(1, 2, 0), wood(1, 3, 0), wood(1, 4, 0), wood(-1, 3, 0), wood(-1, 4, 0), wood(-1, 5, 0)),
		form("cactus_column", trunk(7)),
		form("cactus_cluster", trunk(4), wood(1, 0, 0), wood(1, 1, 0), wood(-1, 0, 1), wood(-1, 1, 1), wood(-1, 2, 1),
			wood(0, 0, -1), wood(1, 0, -1)));

	/**
	 * Deadwood: the oldest bonsai style, a bleached trunk with hardly anything on it. Windswept,
	 * leaning right off the pot; a cascade, falling over the rim; a snag, standing broken.
	 */
	private static final List<BonsaiShape> DEADWOOD = List.of(
		form("deadwood_windswept", trunk(2), wood(1, 2, 0), wood(2, 3, 0), wood(3, 3, 0), leaf(3, 4, 0), leaf(2, 4, 0),
			leaf(4, 3, 0), leaf(-1, 1, 0)),
		form("deadwood_cascade", trunk(3), wood(1, 3, 0), wood(2, 2, 0), wood(3, 1, 0), wood(3, 0, 0), leaf(3, 1, 1),
			leaf(4, 0, 0), leaf(0, 4, 0), leaf(2, 3, 0)),
		form("deadwood_snag", trunk(5), wood(1, 4, 0), wood(-1, 3, 0), wood(-1, 2, 0), leaf(0, 5, 0), leaf(1, 5, 0),
			leaf(-1, 4, 0), leaf(-2, 2, 0)));

	/** A chorus plant: the End's tree, a stalk that forks and carries a flower on every tip. */
	private static final List<BonsaiShape> CHORUS = List.of(
		form("chorus_branching", trunk(3), wood(1, 2, 0), wood(1, 3, 0), leaf(1, 4, 0), wood(-1, 3, 0), leaf(-1, 4, 0),
			wood(0, 3, 0), leaf(0, 4, 0)),
		form("chorus_tall", trunk(6), leaf(0, 6, 0), wood(1, 3, 0), leaf(1, 4, 0), wood(0, 4, -1), leaf(0, 5, -1)),
		form("chorus_bent", trunk(2), wood(1, 2, 0), wood(1, 3, 0), wood(2, 4, 0), leaf(2, 5, 0), wood(-1, 2, 0),
			leaf(-1, 3, 0), wood(1, 4, 1), leaf(1, 5, 1)));

	/** A handful of grass grows a tray of it: a square of moss under a square of grass, the wheatgrass in the window. */
	private static final List<BonsaiShape> GRASS = List.of(
		form("grass_square", square(1, 0, true), square(1, 1, false)),
		form("grass_tray", square(2, 0, true), square(2, 1, false)),
		form("grass_tuft", wood(0, 0, 0), cross(0).stream().map(c -> new Cell(c.x(), 0, c.z(), true)).toList(),
			leaf(0, 1, 0), cross(1), leaf(1, 1, 1), leaf(-1, 1, -1)));

	/** Seeds grow starts: a tray of seedlings on a bed of soil, kept at their first leaves for good. */
	private static final List<BonsaiShape> STARTS = List.of(
		form("starts_tray", square(1, 0, true), square(1, 1, false)),
		form("starts_rows", square(2, 0, true), leaf(-2, 1, -1), leaf(-1, 1, -1), leaf(0, 1, -1), leaf(1, 1, -1), leaf(2, 1, -1),
			leaf(-2, 1, 1), leaf(-1, 1, 1), leaf(0, 1, 1), leaf(1, 1, 1), leaf(2, 1, 1)),
		form("starts_pair", wood(0, 0, 0), wood(1, 0, 0), wood(0, 0, 1), wood(1, 0, 1), leaf(0, 1, 0), leaf(1, 1, 1)));

	/**
	 * The tall flowers: a stalk under a bloom, the two halves of the flower. A clump, one on a
	 * long stem, or three in a row.
	 */
	private static final List<BonsaiShape> TALL_FLOWER = List.of(
		form("tall_flower_clump", flower(0, 0, 2), flower(1, 0, 1), flower(-1, 1, 1), flower(0, -1, 1), flower(1, -1, 1)),
		form("tall_flower_single", flower(0, 0, 3)),
		form("tall_flower_row", flower(-1, 0, 1), flower(0, 0, 2), flower(1, 0, 1)));

	// The larger forms, grown only in a cachepot: a flower pot set inside a decorated pot, whose
	// wider mouth and extra height give a tree room to be old. Two for every kind, drawn at the
	// same cell size as the small ones so they read as the same trees grown on, not blown up.

	private static final List<BonsaiShape> OAK_LARGE = List.of(
		form("oak_grand", post(0, 0, 0, 6), branch(0, 4, 0, 2, 6, 1), branch(0, 5, 0, -2, 7, -1),
			crown(0, 8, 0, 3.5, 2.5, 3.5), crown(2, 7, 1, 2, 1.5, 2), crown(-2, 8, -1, 2, 1.5, 2)),
		form("oak_twin", branch(0, 0, 0, 1, 6, 0), branch(0, 1, 0, -2, 5, 1),
			crown(1, 8, 0, 2.5, 2, 2.5), crown(-2, 6, 1, 2.2, 1.8, 2.2)));

	private static final List<BonsaiShape> BIRCH_LARGE = List.of(
		form("birch_grove", post(0, 0, 0, 9), post(1, 0, 1, 7), post(-1, 0, 0, 8),
			crown(0, 10, 0, 1.3, 2.2, 1.3), crown(1, 8, 1, 1.2, 1.8, 1.2), crown(-1, 9, 0, 1.2, 2, 1.2)),
		form("birch_elder", post(0, 0, 0, 9), branch(0, 5, 0, 2, 7, 0), crown(0, 10, 0, 2, 3, 2), crown(2, 8, 0, 1.3, 1.3, 1.3)));

	private static final List<BonsaiShape> SPRUCE_LARGE = List.of(
		form("spruce_giant", post(0, 0, 0, 11), cone(2, 12, 3.6), leaf(0, 13, 0)),
		form("spruce_tiered", post(0, 0, 0, 10), cone(3, 5, 3), cone(6, 8, 2.3), cone(9, 11, 1.5), leaf(0, 12, 0)));

	private static final List<BonsaiShape> JUNGLE_LARGE = List.of(
		form("jungle_giant", post(0, 0, 0, 9), post(1, 0, 0, 9), post(0, 0, 1, 9), post(1, 0, 1, 9),
			crown(0.5, 10, 0.5, 3.5, 1.6, 3.5), leaf(3, 8, 0), leaf(-2, 8, 1), leaf(0, 8, 3)),
		form("jungle_buttress", post(0, 0, 0, 9), wood(1, 0, 0), wood(-1, 0, 0), wood(0, 0, 1), wood(0, 0, -1),
			wood(2, 0, 0), wood(-1, 0, 1), crown(0, 9, 0, 3, 1.5, 3), branch(0, 5, 0, 2, 6, 0), crown(2, 6, 0, 1.5, 1, 1.5)));

	private static final List<BonsaiShape> ACACIA_LARGE = List.of(
		form("acacia_savanna", branch(0, 0, 0, 0, 3, 0), branch(0, 3, 0, 3, 6, 0), branch(0, 3, 0, -2, 5, 1),
			pad(3, 7, 0, 3), pad(3, 8, 0, 1.5), pad(-2, 6, 1, 2.2)),
		form("acacia_parasol", post(0, 0, 0, 5), branch(0, 5, 0, 1, 6, 0), pad(1, 7, 0, 4), pad(1, 8, 0, 2.5)));

	private static final List<BonsaiShape> DARK_OAK_LARGE = List.of(
		form("dark_oak_ancient", post(0, 0, 0, 5), post(1, 0, 0, 5), post(0, 0, 1, 5), post(1, 0, 1, 5),
			wood(-1, 0, 0), wood(2, 0, 1), wood(0, 0, -1), wood(1, 0, 2), crown(0.5, 7, 0.5, 4, 2.2, 4)),
		form("dark_oak_spread", post(0, 0, 0, 4), post(1, 0, 0, 4), post(0, 0, 1, 4), post(1, 0, 1, 4),
			branch(0, 3, 0, -2, 5, 0), branch(1, 3, 1, 3, 5, 1), branch(1, 3, 0, 1, 5, -2), branch(0, 3, 1, 0, 5, 3),
			crown(0.5, 6, 0.5, 3, 1.6, 3), crown(-2, 6, 0, 1.6, 1.2, 1.6), crown(3, 6, 1, 1.6, 1.2, 1.6),
			crown(1, 6, -2, 1.6, 1.2, 1.6), crown(0, 6, 3, 1.6, 1.2, 1.6)));

	private static final List<BonsaiShape> MANGROVE_LARGE = List.of(
		form("mangrove_arches", branch(2, 0, 2, 0, 3, 0), branch(-2, 0, 2, 0, 3, 0), branch(2, 0, -2, 0, 3, 0),
			branch(-2, 0, -2, 0, 3, 0), post(0, 3, 0, 5), crown(0, 9, 0, 3, 2, 3)),
		form("mangrove_stilted", branch(2, 0, 0, 0, 4, 0), branch(-2, 0, 1, 0, 4, 0), branch(0, 0, -2, 0, 4, 0),
			branch(1, 0, 2, 0, 4, 0), post(0, 4, 0, 4), branch(0, 6, 0, 2, 8, 1),
			crown(0, 9, 0, 2.5, 1.8, 2.5), crown(2, 9, 1, 1.8, 1.4, 1.8)));

	private static final List<BonsaiShape> CHERRY_LARGE = List.of(
		form("cherry_blossom", post(0, 0, 0, 4), branch(0, 4, 0, 2, 7, 0), branch(0, 4, 0, -2, 7, 1),
			crown(0, 8, 0, 4, 1.8, 4), leaf(4, 6, 0), leaf(4, 5, 0), leaf(-4, 6, 0), leaf(0, 6, 4),
			leaf(0, 5, 4), leaf(0, 6, -4), leaf(3, 6, 3), leaf(-3, 6, -3), leaf(-3, 5, -3), leaf(3, 6, -3)),
		form("cherry_arching", branch(0, 0, 0, 3, 6, 0), crown(3, 7, 0, 3, 1.6, 3),
			leaf(6, 5, 0), leaf(6, 6, 0), leaf(3, 5, 3), leaf(3, 6, 3), leaf(3, 5, -3), leaf(3, 6, -3), leaf(0, 6, 0)));

	private static final List<BonsaiShape> AZALEA_LARGE = List.of(
		form("azalea_dome", post(0, 0, 0, 2), crown(0, 3, 0, 4, 2.5, 4)),
		form("azalea_standard", post(0, 0, 0, 5), crown(0, 7, 0, 2.5, 2, 2.5)));

	private static final List<BonsaiShape> POPLAR_LARGE = List.of(
		form("poplar_giant", post(0, 0, 0, 12), crown(0, 9, 0, 1.6, 5, 1.6), leaf(0, 15, 0)),
		form("poplar_pair", post(0, 0, 0, 10), post(2, 0, 1, 8), crown(0, 8, 0, 1.4, 4, 1.4), crown(2, 7, 1, 1.3, 3.2, 1.3)));

	private static final List<BonsaiShape> BAMBOO_LARGE = List.of(
		form("bamboo_thicket", cane(0, 0, 12), cane(1, 0, 10), cane(-1, 1, 11), cane(1, -1, 8), cane(-1, -1, 9),
			cane(0, 1, 7), cane(2, 1, 6), cane(-2, 0, 8), cane(0, -2, 10)),
		form("bamboo_towering", cane(0, 0, 14), cane(1, 1, 12), cane(-1, 0, 10)));

	private static final List<BonsaiShape> MUSHROOM_LARGE = List.of(
		form("mushroom_giant", post(0, 0, 0, 7), pad(0, 6, 0, 3.5), pad(0, 7, 0, 3), pad(0, 8, 0, 2), pad(0, 9, 0, 1)),
		form("mushroom_colony", post(0, 0, 0, 6), pad(0, 6, 0, 2.5), pad(0, 7, 0, 1.3),
			post(2, 0, 1, 4), pad(2, 4, 1, 1.8), pad(2, 5, 1, 0.8), post(-2, 0, -1, 3), pad(-2, 3, -1, 1.4)));

	private static final List<BonsaiShape> FUNGUS_LARGE = List.of(
		form("fungus_towering", post(0, 0, 0, 9), crown(0, 9, 0, 2.6, 2, 2.6),
			leaf(2, 7, 0), leaf(-2, 6, 1), leaf(0, 7, -2), leaf(1, 6, 2)),
		form("fungus_grove", post(0, 0, 0, 7), crown(0, 7, 0, 2, 1.5, 2), post(2, 0, 1, 5), crown(2, 5, 1, 1.5, 1.2, 1.5),
			post(-2, 0, -1, 4), crown(-2, 4, -1, 1.3, 1, 1.3)));

	private static final List<BonsaiShape> CACTUS_LARGE = List.of(
		form("cactus_giant", post(0, 0, 0, 12), wood(1, 4, 0), post(2, 4, 0, 5), wood(-1, 6, 0), post(-2, 6, 0, 5),
			wood(0, 8, 1), post(0, 8, 2, 3)),
		form("cactus_candelabra", post(0, 0, 0, 9), wood(1, 3, 0), post(2, 3, 0, 5), wood(-1, 4, 0), post(-2, 4, 0, 4),
			wood(0, 5, 1), post(0, 5, 2, 3), wood(0, 2, -1), post(0, 2, -2, 4)));

	private static final List<BonsaiShape> DEADWOOD_LARGE = List.of(
		form("deadwood_ancient", branch(0, 0, 0, 1, 3, 0), branch(1, 3, 0, 0, 6, 1), branch(0, 6, 1, 2, 9, 1),
			branch(1, 4, 0, 3, 6, -1), branch(0, 7, 1, -2, 8, 1), wood(1, 0, 0), wood(-1, 0, 0), wood(0, 0, 1),
			leaf(2, 10, 1), leaf(3, 7, -1), leaf(-2, 9, 1)),
		form("deadwood_driftwood", branch(0, 0, 0, 1, 2, 0), branch(1, 2, 0, 5, 3, 0), branch(1, 2, 0, -3, 4, 1),
			branch(4, 3, 0, 5, 5, 0), leaf(5, 6, 0), leaf(-3, 5, 1), leaf(3, 4, 0)));

	private static final List<BonsaiShape> CHORUS_LARGE = List.of(
		form("chorus_great", post(0, 0, 0, 5), leaf(0, 5, 0), branch(0, 4, 0, 2, 6, 0), post(2, 6, 0, 3), leaf(2, 9, 0),
			branch(0, 4, 0, -2, 7, 1), leaf(-2, 8, 1), branch(2, 7, 0, 3, 8, 1), leaf(3, 9, 1),
			branch(0, 3, 0, 0, 6, -2), leaf(0, 7, -2)),
		form("chorus_spire", post(0, 0, 0, 11), leaf(0, 11, 0), branch(0, 3, 0, 2, 4, 0), leaf(2, 5, 0),
			branch(0, 6, 0, -2, 7, 0), leaf(-2, 8, 0), branch(0, 8, 0, 0, 9, 2), leaf(0, 10, 2)));

	private static final List<BonsaiShape> GRASS_LARGE = List.of(
		form("grass_meadow", square(3, 0, true), square(3, 1, false)),
		form("grass_mound", square(3, 0, true), square(2, 1, true), square(1, 2, true),
			square(3, 1, false), square(2, 2, false), square(1, 3, false)));

	private static final List<BonsaiShape> STARTS_LARGE = List.of(
		form("starts_field", square(3, 0, true), square(3, 1, false)),
		form("starts_beds", square(3, 0, true),
			java.util.stream.IntStream.rangeClosed(-3, 3).boxed().flatMap(x -> java.util.stream.Stream.of(
				leaf(x, 1, -3), leaf(x, 1, -1), leaf(x, 1, 1), leaf(x, 1, 3))).toList()));

	private static final List<BonsaiShape> TALL_FLOWER_LARGE = List.of(
		form("tall_flower_bouquet", flower(0, 0, 4), flower(1, 0, 3), flower(-1, 0, 3), flower(0, 1, 3), flower(0, -1, 3),
			flower(1, 1, 2), flower(-1, -1, 2), flower(1, -1, 2), flower(-1, 1, 2)),
		form("tall_flower_hedge", flower(-2, 0, 2), flower(-1, 0, 3), flower(0, 0, 2), flower(1, 0, 3), flower(2, 0, 2),
			flower(-1, 1, 2), flower(1, -1, 2)));

	/**
	 * Trailing plants in a flower pot: a mound on a bed, spilling one cell over the rim, which is
	 * as far down the outside of a flower pot as there is before the table. All round, all to one
	 * side, or a heaped mound.
	 */
	private static final List<BonsaiShape> TRAILING = List.of(
		hanging("trailing_spill", -1, square(1, 0, true), pad(0, 1, 0, 1.3), leaf(0, 2, 0),
			strand(2, 1, 0, 3), strand(-2, 1, 1, 3), strand(0, 1, -2, 3), strand(1, 1, 2, 2)),
		hanging("trailing_sided", -1, square(1, 0, true), pad(0, 1, 0, 1.2),
			strand(2, 1, -1, 3), strand(2, 1, 0, 3), strand(2, 1, 1, 3), strand(-2, 1, 0, 2)),
		hanging("trailing_mound", -1, square(1, 0, true), pad(0, 1, 0, 1.5), pad(0, 2, 0, 1), leaf(0, 3, 0),
			strand(2, 1, 1, 2), strand(-2, 1, -1, 2)));

	/**
	 * Trailing plants in a cachepot, where the neck stands a block up and the strands have the whole
	 * side of the decorated pot to hang down: a curtain all round, or a cascade down one face.
	 */
	private static final List<BonsaiShape> TRAILING_LARGE = List.of(
		hanging("trailing_curtain", -7, crown(0, 1, 0, 2, 1.2, 2), leaf(2, 0, 0), leaf(-2, 0, 1), leaf(0, 0, 2), leaf(1, 0, -2),
			strand(3, 0, -1, 6), strand(3, 0, 1, 8), strand(-3, 0, 0, 5), strand(-3, 0, 2, 7),
			strand(0, 0, 3, 7), strand(2, 0, 3, 4), strand(1, 0, -3, 8), strand(-1, 0, -3, 5)),
		hanging("trailing_cascade", -7, crown(0, 1, 0, 2, 1.3, 2), leaf(2, 0, -1), leaf(2, 0, 0), leaf(2, 0, 1),
			strand(3, 0, -2, 7), strand(3, 0, -1, 8), strand(3, 0, 0, 8), strand(3, 0, 1, 8), strand(3, 0, 2, 6),
			strand(0, 0, 3, 3), strand(-3, 0, 0, 3)));

	private static final Map<String, List<BonsaiShape>> LARGE = Map.ofEntries(
		Map.entry("oak", OAK_LARGE), Map.entry("birch", BIRCH_LARGE), Map.entry("spruce", SPRUCE_LARGE),
		Map.entry("jungle", JUNGLE_LARGE), Map.entry("acacia", ACACIA_LARGE),
		Map.entry("dark_oak", DARK_OAK_LARGE), Map.entry("pale_oak", DARK_OAK_LARGE),
		Map.entry("mangrove", MANGROVE_LARGE), Map.entry("cherry", CHERRY_LARGE),
		Map.entry("azalea", AZALEA_LARGE), Map.entry("flowering_azalea", AZALEA_LARGE), Map.entry("poplar", POPLAR_LARGE),
		Map.entry("bamboo", BAMBOO_LARGE), Map.entry("red_mushroom", MUSHROOM_LARGE), Map.entry("brown_mushroom", MUSHROOM_LARGE),
		Map.entry("crimson_fungus", FUNGUS_LARGE), Map.entry("warped_fungus", FUNGUS_LARGE),
		Map.entry("cactus", CACTUS_LARGE), Map.entry("dead_bush", DEADWOOD_LARGE), Map.entry("chorus", CHORUS_LARGE),
		Map.entry("grass", GRASS_LARGE), Map.entry("starts", STARTS_LARGE), Map.entry("tall_flower", TALL_FLOWER_LARGE),
		Map.entry("trailing", TRAILING_LARGE));

	private static final Map<String, List<BonsaiShape>> FAMILIES = Map.ofEntries(
		Map.entry("oak", OAK), Map.entry("birch", BIRCH), Map.entry("spruce", SPRUCE),
		Map.entry("jungle", JUNGLE), Map.entry("acacia", ACACIA),
		Map.entry("dark_oak", DARK_OAK), Map.entry("pale_oak", DARK_OAK),
		Map.entry("mangrove", MANGROVE), Map.entry("cherry", CHERRY),
		Map.entry("azalea", AZALEA), Map.entry("flowering_azalea", AZALEA), Map.entry("poplar", POPLAR),
		Map.entry("bamboo", BAMBOO), Map.entry("red_mushroom", MUSHROOM), Map.entry("brown_mushroom", MUSHROOM),
		Map.entry("crimson_fungus", FUNGUS), Map.entry("warped_fungus", FUNGUS),
		Map.entry("cactus", CACTUS), Map.entry("dead_bush", DEADWOOD), Map.entry("chorus", CHORUS),
		Map.entry("grass", GRASS), Map.entry("starts", STARTS), Map.entry("tall_flower", TALL_FLOWER),
		Map.entry("trailing", TRAILING));

	/** The three forms a kind of sapling can take; a kind nobody drew for grows like an oak. */
	public static List<BonsaiShape> family(String kind) {
		return FAMILIES.getOrDefault(kind, OAK);
	}

	/** The two larger forms, for a cachepot. */
	public static List<BonsaiShape> large(String kind) {
		return LARGE.getOrDefault(kind, OAK_LARGE);
	}

	/** Everything a cachepot's shears step through: the small forms, then the large. */
	private static List<BonsaiShape> cachepotFamily(String kind) {
		List<BonsaiShape> all = new java.util.ArrayList<>(family(kind));
		all.addAll(large(kind));
		return all;
	}

	/**
	 * Where a tree goes next under the shears: the next quarter turn of its form, and after the
	 * fourth, the next form. A form no longer in the family - one of the earlier build's - starts
	 * the family over.
	 */
	public static Next next(String kind, String currentName, int currentTurn, boolean cachepot) {
		List<BonsaiShape> forms = cachepot ? cachepotFamily(kind) : family(kind);
		int at = 0;
		for (int i = 0; i < forms.size(); i++) {
			if (forms.get(i).name().equals(currentName)) at = i;
		}
		boolean known = at < forms.size() && forms.get(at).name().equals(currentName);
		int turn = known ? (currentTurn + 1) % 4 : 0;
		if (known && turn == 0) at = (at + 1) % forms.size();
		return new Next(forms.get(at), turn);
	}

	/** A form and the quarter turn to plant it at. */
	public record Next(BonsaiShape shape, int turn) {}

	public static List<BonsaiShape> all() {
		List<BonsaiShape> all = new java.util.ArrayList<>();
		for (List<BonsaiShape> family : FAMILIES.values()) {
			for (BonsaiShape shape : family) if (!all.contains(shape)) all.add(shape);
		}
		for (List<BonsaiShape> family : LARGE.values()) {
			for (BonsaiShape shape : family) if (!all.contains(shape)) all.add(shape);
		}
		return all;
	}
}
