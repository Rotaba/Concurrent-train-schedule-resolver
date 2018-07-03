package lockingTrains.validation;

import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Event corresponding to {@link Recorder#pause}.
 */
public class PauseEvent extends TrainEvent {
	private final TrainSchedule schedule;
	private final Location location;

	public PauseEvent(final TrainSchedule schedule, final Location location) {
		this(System.currentTimeMillis(), schedule, location);
	}

	public PauseEvent(final long timestamp, final TrainSchedule schedule, final Location location) {
		super(timestamp);
		this.schedule = schedule;
		this.location = location;
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.pause(timestamp, schedule, location);
	}

	@Override
	public String toString() {
		return String.format("%d: pause(%d,%s)", timestamp, schedule.id(), location.name());
	}
}
