package lockingTrains.validation;

import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Event corresponding to {@link Recorder#arrive}.
 */
public class ArriveEvent extends TrainEvent {
	private final TrainSchedule schedule;
	private final Location location;

	public ArriveEvent(final TrainSchedule schedule, final Location location) {
		this(System.currentTimeMillis(), schedule, location);
	}

	public ArriveEvent(final long timestamp, final TrainSchedule schedule, final Location location) {
		super(timestamp);
		this.schedule = schedule;
		this.location = location;
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.arrive(timestamp, schedule, location);
	}

	@Override
	public String toString() {
		return String.format("%d: arrive(%d,%s)", timestamp, schedule.id(), location.name());
	}
}
