package justfatlard.bonsai_saplings;

import java.util.List;
import java.util.Map;


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

	private static BonsaiShape form(String name, Object... parts) {
		return new BonsaiShape(name, List.copyOf(cells(parts)));
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

	private static final Map<String, List<BonsaiShape>> FAMILIES = Map.ofEntries(
		Map.entry("oak", OAK), Map.entry("birch", BIRCH), Map.entry("spruce", SPRUCE),
		Map.entry("jungle", JUNGLE), Map.entry("acacia", ACACIA),
		Map.entry("dark_oak", DARK_OAK), Map.entry("pale_oak", DARK_OAK),
		Map.entry("mangrove", MANGROVE), Map.entry("cherry", CHERRY),
		Map.entry("azalea", AZALEA), Map.entry("flowering_azalea", AZALEA), Map.entry("poplar", POPLAR));

	/** The three forms a kind of sapling can take; a kind nobody drew for grows like an oak. */
	public static List<BonsaiShape> family(String kind) {
		return FAMILIES.getOrDefault(kind, OAK);
	}

	/**
	 * Where a tree goes next under the shears: the next quarter turn of its form, and after the
	 * fourth, the next form. A form no longer in the family - one of the earlier build's - starts
	 * the family over.
	 */
	public static Next next(String kind, String currentName, int currentTurn) {
		List<BonsaiShape> forms = family(kind);
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

	/**
	 * The one that is not a tree.
	 *
	 * <p>A creeper is a green thing on four legs that turns up in a garden uninvited, which is
	 * most of the way to topiary already. Read from the side: four feet, a body, a head sitting
	 * proud of it.
	 *
	 * <p>Deliberately outside every family. It is never rolled at random and shears never turn a
	 * tree into one, because a creeper you did not plant is a surprise of the wrong kind.
	 */
	public static final BonsaiShape CREEPER = new BonsaiShape("creeper", List.of(
		wood(-1, 0, -1), wood(1, 0, -1), wood(-1, 0, 1), wood(1, 0, 1),
		leaf(-1, 1, 0), leaf(0, 1, 0), leaf(1, 1, 0),
		leaf(-1, 1, -1), leaf(1, 1, -1), leaf(-1, 1, 1), leaf(1, 1, 1),
		leaf(0, 2, 0),
		wood(0, 3, 0)));

	public static List<BonsaiShape> all() {
		List<BonsaiShape> all = new java.util.ArrayList<>();
		for (List<BonsaiShape> family : FAMILIES.values()) {
			for (BonsaiShape shape : family) if (!all.contains(shape)) all.add(shape);
		}
		return all;
	}
}
