package lockingTrains.validation;

import lockingTrains.shared.TrainSchedule;

/**
 * Event corresponding to {@link Recorder#start}.
 */
public class StartEvent extends TrainEvent {
	private final TrainSchedule schedule;

	public StartEvent(final TrainSchedule schedule) {
		this(System.currentTimeMillis(), schedule);
	}

	public StartEvent(final long timestamp, final TrainSchedule schedule) {
		super(timestamp);
		this.schedule = schedule;
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.start(timestamp, schedule);
	}

	@Override
	public String toString() {
		return String.format("%d: start(%d)", timestamp, schedule.id());
	}
}
