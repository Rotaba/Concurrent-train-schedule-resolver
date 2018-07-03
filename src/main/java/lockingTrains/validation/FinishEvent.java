package lockingTrains.validation;

import lockingTrains.shared.TrainSchedule;

/**
 * Event corresponding to {@link Recorder#finish}.
 */
public class FinishEvent extends TrainEvent {
	private final TrainSchedule schedule;

	public FinishEvent(final TrainSchedule schedule) {
		this(System.currentTimeMillis(), schedule);
	}

	public FinishEvent(final long timestamp, final TrainSchedule schedule) {
		super(timestamp);
		this.schedule = schedule;
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.finish(timestamp, schedule);
	}

	@Override
	public String toString() {
		return String.format("%d: finish(%d)", timestamp, schedule.id());
	}
}
