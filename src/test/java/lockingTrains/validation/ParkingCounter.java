package lockingTrains.validation;

import java.util.HashMap;
import java.util.Map;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

public class ParkingCounter extends Recorder {
	private final Map<Integer, Integer> pauseCalls = new HashMap<>();
	private final Map<Integer, Integer> resumeCalls = new HashMap<>();

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
	}

	@Override
	void travel(final long timestamp, final TrainSchedule schedule, final Connection section) {
	}

	@Override
	public void arrive(final TrainSchedule schedule, final Location location) {
	}

	@Override
	void arrive(final long timestamp, final TrainSchedule schedule, final Location location) {
	}

	@Override
	public synchronized void pause(final TrainSchedule schedule, final Location location) {
		pause(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void pause(final long timestamp, final TrainSchedule schedule, final Location location) {
		var old = pauseCalls.get(location.id());
		if (old == null)
			old = 0;

		pauseCalls.put(location.id(), old + 1);
	}

	@Override
	public synchronized void resume(final TrainSchedule schedule, final Location location) {
		resume(System.currentTimeMillis(), schedule, location);
	}

	@Override
	void resume(final long timestamp, final TrainSchedule schedule, final Location location) {
		var old = resumeCalls.get(location.id());
		if (old == null)
			old = 0;

		resumeCalls.put(location.id(), old + 1);
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

	public synchronized int pauseCalls(final Location location) {
		final var calls = pauseCalls.get(location.id());
		return calls == null ? -1 : calls;
	}

	public synchronized int resumeCalls(final Location location) {
		final var calls = resumeCalls.get(location.id());
		return calls == null ? -1 : calls;
	}
}
