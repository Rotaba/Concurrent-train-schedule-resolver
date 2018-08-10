package lockingTrains.validation;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

public class EmptyScheduleRecorder extends Recorder {
	private int doneCalls = 0;

	@Override
	public void start(final TrainSchedule schedule) {
		throw new IllegalStateException();
	}

	@Override
	void start(final long timestamp, final TrainSchedule schedule) {
		throw new IllegalStateException();
	}

	@Override
	public void leave(final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	void leave(final long timestamp, final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	public void travel(final TrainSchedule schedule, final Connection section) {
		throw new IllegalStateException();
	}

	@Override
	void travel(final long timestamp, final TrainSchedule schedule, final Connection section) {
		throw new IllegalStateException();
	}

	@Override
	public void arrive(final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	public void pause(final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	public void resume(final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
		throw new IllegalStateException();
	}

	@Override
	public void finish(final TrainSchedule schedule) {
		throw new IllegalStateException();
	}

	@Override
	void finish(final long timestamp, final TrainSchedule schedule) {
		throw new IllegalStateException();
	}

	@Override
	public void done() {
		++doneCalls;
	}

	@Override
	void done(final long timestamp) {
		++doneCalls;
	}

	public boolean doneCalled() {
		return doneCalls == 1;
	}
}
