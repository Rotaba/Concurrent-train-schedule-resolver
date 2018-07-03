package lockingTrains.validation;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

public class ExceptionRecorder extends Recorder {
	@Override
	public void start(final TrainSchedule schedule) {
	}

	@Override
	void start(final long timestamp, final TrainSchedule schedule) {
	}

	@Override
	public void leave(final TrainSchedule schedule, final Location location) {
	}

	@Override
	void leave(final long timestamp, final TrainSchedule schedule, final Location location) {
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
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
	}

	@Override
	public void pause(final TrainSchedule schedule, final Location location) {
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
	}

	@Override
	public void resume(final TrainSchedule schedule, final Location location) {
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
	}

	@Override
	public void finish(final TrainSchedule schedule) {
	}

	@Override
	void finish(final long timestamp, final TrainSchedule schedule) {
	}

	@Override
	public void done() {
	}

	@Override
	void done(final long timestamp) {
	}
}
