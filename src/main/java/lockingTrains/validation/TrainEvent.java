package lockingTrains.validation;

/**
 * Abstracts all possible train events. The methods of the {@link Recorder}
 * interface correspond to the train events one-to-one.
 */
public abstract class TrainEvent {
	/**
	 * Creation timestamp of the event.
	 */
	protected final long timestamp;

	/**
	 * Constructs any {@link TrainEvent} by initializing its creation timestamp.
	 *
	 * @param timestamp the creation timestamp.
	 */
	protected TrainEvent(final long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Replay this event on a given {@link Recorder}.
	 *
	 * @param recorder the {@link Recorder} to replay on.
	 */
	public abstract void replay(final Recorder recorder);

	@Override
	public abstract String toString();
}
