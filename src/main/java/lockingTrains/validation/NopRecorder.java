package lockingTrains.validation;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Implements the {@link Recorder} interface with blank methods. Useful if you
 * want to test your scheduling without functional correctness testing and
 * without the additional synchronization caused by logging.
 */
public class NopRecorder extends Recorder {
	@Override
	public void start(final TrainSchedule schedule) {
		// NOP
	}

	@Override
	void start(final long timestamp, final TrainSchedule schedule) {
		// NOP
	}

	@Override
	public void leave(final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	void leave(final long timestamp, final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	public void travel(final TrainSchedule schedule, final Connection section) {
		// NOP
	}

	@Override
	void travel(final long timestamp, final TrainSchedule schedule, final Connection section) {
		// NOP
	}

	@Override
	public void arrive(final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	public void pause(final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	public void resume(final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
		// NOP
	}

	@Override
	public void finish(final TrainSchedule schedule) {
		// NOP
	}

	@Override
	void finish(final long timestamp, final TrainSchedule schedule) {
		// NOP
	}

	@Override
	public void done() {
		// NOP
	}

	@Override
	void done(final long timestamp) {
		// NOP
	}
}
