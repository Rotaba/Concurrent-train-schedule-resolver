package lockingTrains.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Implements the {@link Recorder} interface for the purpose of logging the
 * events. This is useful to replay all events or to export the simulation
 * history.
 */
public class Logger extends Recorder {
	private final List<TrainEvent> eventLog;

	/**
	 * Constructs a new {@link Logger}.
	 */
	public Logger() {
		this.eventLog = new ArrayList<>();
	}

	@Override
	public synchronized void start(final TrainSchedule schedule) {
		eventLog.add(new StartEvent(schedule));
	}

	@Override
	void start(final long timestamp, final TrainSchedule schedule) {
		eventLog.add(new StartEvent(timestamp, schedule));
	}

	@Override
	public synchronized void leave(final TrainSchedule schedule, final Location location) {
		eventLog.add(new LeaveEvent(schedule, location));
	}

	@Override
	void leave(final long timestamp, final TrainSchedule schedule, final Location location) {
		eventLog.add(new LeaveEvent(timestamp, schedule, location));

	}

	@Override
	public synchronized void travel(final TrainSchedule schedule, final Connection section) {
		eventLog.add(new TravelEvent(schedule, section));
	}

	@Override
	void travel(final long timestamp, final TrainSchedule schedule, final Connection section) {
		eventLog.add(new TravelEvent(timestamp, schedule, section));
	}

	@Override
	public synchronized void arrive(final TrainSchedule schedule, final Location location) {
		eventLog.add(new ArriveEvent(schedule, location));
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
		eventLog.add(new ArriveEvent(timestamp, schedule, location));
	}

	@Override
	public synchronized void pause(final TrainSchedule schedule, final Location location) {
		eventLog.add(new PauseEvent(schedule, location));
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
		eventLog.add(new PauseEvent(timestamp, schedule, location));
	}

	@Override
	public synchronized void resume(final TrainSchedule schedule, final Location location) {
		eventLog.add(new ResumeEvent(schedule, location));
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
		eventLog.add(new ResumeEvent(timestamp, schedule, location));
	}

	@Override
	public synchronized void finish(final TrainSchedule schedule) {
		eventLog.add(new FinishEvent(schedule));
	}

	@Override
	void finish(final long timestamp, final TrainSchedule schedule) {
		eventLog.add(new FinishEvent(timestamp, schedule));
	}

	@Override
	public synchronized void done() {
		eventLog.add(new DoneEvent());
	}

	@Override
	void done(final long timestamp) {
		eventLog.add(new DoneEvent(timestamp));
	}

	/**
	 * Get the event log. The result cannot be modified. It stores all calls to the
	 * {@link Recorder}.
	 *
	 * @return The (unmodifiable) list of events.
	 */
	public synchronized List<TrainEvent> eventLog() {
		return Collections.unmodifiableList(eventLog);
	}
}
