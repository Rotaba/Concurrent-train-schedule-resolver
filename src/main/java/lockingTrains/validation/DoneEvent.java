package lockingTrains.validation;

/**
 * Event corresponding to {@link Recorder#done}.
 */
public class DoneEvent extends TrainEvent {
	public DoneEvent() {
		this(System.currentTimeMillis());
	}

	public DoneEvent(final long timestamp) {
		super(timestamp);
	}

	@Override
	public void replay(final Recorder recorder) {
		recorder.done(timestamp);
	}

	@Override
	public String toString() {
		return String.format("%d: done", timestamp);
	}
}
