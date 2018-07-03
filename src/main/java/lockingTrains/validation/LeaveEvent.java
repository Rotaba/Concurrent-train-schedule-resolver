package lockingTrains.validation;

import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Event corresponding to {@link Recorder#leave}.
 */
public class LeaveEvent extends TrainEvent {
	private final TrainSchedule schedule;
	private final Location location;

	public LeaveEvent(final TrainSchedule schedule, final Location location) {
		this(System.currentTimeMillis(), schedule, location);
	}

	public LeaveEvent(final long timestamp, final TrainSchedule schedule, final Location location) {
		super(timestamp);
		this.schedule = schedule;
		this.location = location;
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.leave(timestamp, schedule, location);
	}

	@Override
	public String toString() {
		return String.format("%d: leave(%d,%s)", timestamp, schedule.id(), location.name());
	}
}
