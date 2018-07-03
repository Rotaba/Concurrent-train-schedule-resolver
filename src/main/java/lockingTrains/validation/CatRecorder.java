package lockingTrains.validation;

import java.util.List;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Implements the {@link Recorder} interface for the purpose of concatenating
 * multiple recorders.
 */
public class CatRecorder extends Recorder {
	private final List<Recorder> recorders;

	public CatRecorder(final List<Recorder> recorders) {
		this.recorders = recorders;
	}

	@Override
	public synchronized void start(final TrainSchedule schedule) {
		start(System.currentTimeMillis(), schedule);
	}

	@Override
	void start(final long timestamp, final TrainSchedule schedule) {
		recorders.forEach(r -> r.start(timestamp, schedule));
	}

	@Override
	public synchronized void leave(final TrainSchedule schedule, final Location location) {
		leave(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void leave(final long timestamp, final TrainSchedule schedule, final Location location) {
		recorders.forEach(r -> r.leave(timestamp, schedule, location));
	}

	@Override
	public synchronized void travel(final TrainSchedule schedule, final Connection section) {
		travel(System.currentTimeMillis(), schedule, section);
	}

	@Override
	void travel(final long timestamp, final TrainSchedule schedule, final Connection section) {
		recorders.forEach(r -> r.travel(timestamp, schedule, section));
	}

	@Override
	public synchronized void arrive(final TrainSchedule schedule, final Location location) {
		arrive(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
		recorders.forEach(r -> r.arrive(timestamp, schedule, location));
	}

	@Override
	public synchronized void pause(final TrainSchedule schedule, final Location location) {
		pause(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
		recorders.forEach(r -> r.pause(timestamp, schedule, location));
	}

	@Override
	public synchronized void resume(final TrainSchedule schedule, final Location location) {
		resume(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
		recorders.forEach(r -> r.resume(timestamp, schedule, location));
	}

	@Override
	public synchronized void finish(final TrainSchedule schedule) {
		finish(System.currentTimeMillis(), schedule);
	}

	@Override
	void finish(final long timestamp, final TrainSchedule schedule) {
		recorders.forEach(r -> r.finish(timestamp, schedule));
	}

	@Override
	public synchronized void done() {
		done(System.currentTimeMillis());
	}

	@Override
	void done(final long timestamp) {
		recorders.forEach(r -> r.done(timestamp));
	}
}
