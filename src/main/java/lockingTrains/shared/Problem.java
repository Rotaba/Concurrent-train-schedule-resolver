package lockingTrains.shared;

import java.util.Collections;
import java.util.List;

/**
 * Representation of a problem. A problem is simply a {@link Map} and a list of
 * {@link TrainSchedule}s that should be simulated on this map.
 */
public class Problem {
	private final Map map;
	private final List<TrainSchedule> schedules;

	/**
	 * Constructs a new problem.
	 *
	 * @param map       used for the problem.
	 * @param schedules that need to be simulated.
	 */
	public Problem(final Map map, final List<TrainSchedule> schedules) {
		this.map = map;
		this.schedules = schedules;
	}

	/**
	 * Get the {@link Map} associated with the problem.
	 *
	 * @return The {@link Map} associated with the problem.
	 */
	public Map map() {
		return map;
	}

	/**
	 * Get the list of {@link TrainSchedule}s to simulate.
	 * <p>
	 * The result cannot be modified.
	 *
	 * @return The (unmodifiable) list of {@link TrainSchedule}s to simulate.
	 */
	public List<TrainSchedule> schedules() {
		return Collections.unmodifiableList(schedules);
	}
}
