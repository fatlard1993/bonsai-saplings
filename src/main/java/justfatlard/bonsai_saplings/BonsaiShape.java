package justfatlard.bonsai_saplings;

import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * The forms a little tree can take.
 *
 * <p>Not a vanilla tree shrunk down. A vanilla tree is shaped by what a tree does when nobody is
 * managing it, and shrinking one gives you a small unmanaged tree, which is a shrub. These are
 * shaped the way a bonsai is shaped - by somebody deciding where the trunk leans and where the
 * foliage is allowed to sit - which is why they read as deliberate at a size where a real oak
 * reads as a blob.
 *
 * <p>The classical forms, near enough: upright, curved, leaning, cascading over the rim,
 * windswept, and twin trunk. Coordinates are in template blocks around the pot's middle, with y
 * counting up from the rim, so a negative y is a branch falling past it.
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

	/** Straight up, foliage in tiers. The formal one, and the one that reads as a tree fastest. */
	private static final BonsaiShape UPRIGHT = new BonsaiShape("upright", List.of(
		wood(0, 0, 0), wood(0, 1, 0), wood(0, 2, 0), wood(0, 3, 0),
		leaf(1, 2, 0), leaf(-1, 2, 0), leaf(0, 2, 1), leaf(0, 2, -1),
		leaf(0, 4, 0), leaf(1, 4, 0), leaf(-1, 4, 0), leaf(0, 4, 1), leaf(0, 4, -1)));

	/** The S-curve. A trunk that changes its mind twice on the way up. */
	private static final BonsaiShape CURVED = new BonsaiShape("curved", List.of(
		wood(0, 0, 0), wood(0, 1, 0), wood(1, 2, 0), wood(1, 3, 0), wood(0, 4, 0),
		leaf(2, 3, 0), leaf(1, 4, 1), leaf(0, 5, 0), leaf(1, 5, 0), leaf(-1, 5, 0),
		leaf(0, 5, 1), leaf(0, 5, -1)));

	/** Leaning, with the weight carried out over the lean. */
	private static final BonsaiShape LEANING = new BonsaiShape("leaning", List.of(
		wood(0, 0, 0), wood(0, 1, 0), wood(1, 2, 0), wood(2, 3, 0),
		leaf(2, 4, 0), leaf(1, 4, 0), leaf(3, 4, 0), leaf(2, 4, 1), leaf(2, 4, -1),
		leaf(2, 3, 1), leaf(3, 3, 0)));

	/** Over the edge and down past the rim, the way one growing off a cliff does. */
	private static final BonsaiShape CASCADE = new BonsaiShape("cascade", List.of(
		wood(0, 0, 0), wood(0, 1, 0), wood(1, 1, 0), wood(2, 0, 0), wood(2, -1, 0),
		leaf(0, 2, 0), leaf(2, -2, 0), leaf(3, -1, 0), leaf(2, -1, 1), leaf(2, -1, -1),
		leaf(2, 0, 1), leaf(3, 0, 0)));

	/** Everything blown to one side, as if it has been standing somewhere windy for a long time. */
	private static final BonsaiShape WINDSWEPT = new BonsaiShape("windswept", List.of(
		wood(0, 0, 0), wood(0, 1, 0), wood(0, 2, 0), wood(1, 3, 0),
		leaf(2, 3, 0), leaf(3, 3, 0), leaf(2, 4, 0), leaf(1, 4, 0), leaf(3, 2, 0),
		leaf(2, 3, 1), leaf(2, 3, -1)));

	/** Two trunks off one root, one clearly the elder. */
	private static final BonsaiShape TWIN = new BonsaiShape("twin", List.of(
		wood(0, 0, 0), wood(0, 1, 0), wood(0, 2, 0), wood(0, 3, 0),
		wood(1, 0, 0), wood(1, 1, 0),
		leaf(0, 4, 0), leaf(1, 4, 0), leaf(-1, 4, 0), leaf(0, 4, 1), leaf(0, 4, -1),
		leaf(1, 2, 0), leaf(2, 2, 0), leaf(1, 2, 1)));

	/**
	 * The one that is not a tree.
	 *
	 * <p>A creeper is a green thing on four legs that turns up in a garden uninvited, which is
	 * most of the way to topiary already. Read from the side: four feet, a body, a head sitting
	 * proud of it.
	 *
	 * <p>Deliberately outside {@link #ALL}. It is never rolled at random and shears never turn a
	 * tree into one, because a creeper you did not plant is a surprise of the wrong kind.
	 */
	public static final BonsaiShape CREEPER = new BonsaiShape("creeper", List.of(
		wood(-1, 0, -1), wood(1, 0, -1), wood(-1, 0, 1), wood(1, 0, 1),
		leaf(-1, 1, 0), leaf(0, 1, 0), leaf(1, 1, 0),
		leaf(-1, 1, -1), leaf(1, 1, -1), leaf(-1, 1, 1), leaf(1, 1, 1),
		leaf(0, 2, 0),
		wood(0, 3, 0)));

	public static final List<BonsaiShape> ALL =
		List.of(UPRIGHT, CURVED, LEANING, CASCADE, WINDSWEPT, TWIN);

	/**
	 * A form other than the one it is wearing, so a cut always shows a change.
	 *
	 * <p>Shears that sometimes did nothing visible would read as shears that sometimes do not
	 * work, and the player would keep clicking to find out which.
	 */
	public static BonsaiShape otherThan(RandomSource random, String current) {
		List<BonsaiShape> others = ALL.stream().filter(shape -> !shape.name().equals(current)).toList();

		return others.get(random.nextInt(others.size()));
	}
}
