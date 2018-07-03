package lockingTrains.validation;

import lockingTrains.shared.Connection;
import lockingTrains.shared.TrainSchedule;

/**
 * Event corresponding to {@link Recorder#travel}.
 */
public class TravelEvent extends TrainEvent {
	private final TrainSchedule schedule;
	private final Connection section;

	public TravelEvent(final TrainSchedule schedule, final Connection section) {
		this(System.currentTimeMillis(), schedule, section);
	}

	public TravelEvent(final long timestamp, final TrainSchedule schedule, final Connection section) {
		super(timestamp);
		this.schedule = schedule;
		this.section = section;
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.travel(timestamp, schedule, section);
	}

	@Override
	public String toString() {
		return String.format("%d: travel(%d,%s-%s,%d)", timestamp, schedule.id(), section.first().name(),
				section.second().name(), section.time());
	}
}
