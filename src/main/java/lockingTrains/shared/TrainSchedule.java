package lockingTrains.shared;

/**
 * Representation of train schedule. A train schedule consists of two
 * {@link Location}s: an {@link origin} and a {@link destination}. For each
 * schedule, there must to be a train running this schedule, i.e. starting at
 * {@link origin} and eventually arriving at {@link destination}. Each train can
 * only cover a single schedule, so if there a duplicate schedules, multiple
 * trains must be sent along the route.
 */
public class TrainSchedule {
	/**
	 * Counts the instances to assign unique ID's to them.
	 */
	private static int counter = 0;

	protected final Location origin;
	protected final Location destination;

	private final int id;

	/**
	 * Constructs a new train schedule.
	 *
	 * @param origin      of the schedule.
	 * @param destination of the schedule.
	 */
	public TrainSchedule(final Location origin, final Location destination) {
		this.origin = origin;
		this.destination = destination;
		this.id = counter++;
	}

	/**
	 * Get the origin {@link Location} of the train schedule.
	 *
	 * @return The origin of the train schedule.
	 */
	public Location origin() {
		return origin;
	}

	/**
	 * Get the destination {@link Location} of the train schedule.
	 *
	 * @return The destination of the train schedule.
	 */
	public Location destination() {
		return destination;
	}

	/**
	 * Get the unique ID assigned during construction.
	 *
	 * @return The unique ID assigned during construction.
	 */
	public int id() {
		return id;
	}
}
