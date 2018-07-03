package lockingTrains.validation;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.TrainSchedule;

/**
 * Interface for recording simulation actions. Your implementation must call
 * these methods appropriately in order to pass the functional correctness
 * tests. The methods document when they shall be called.
 */
public abstract class Recorder {
	/**
	 * Call this method for each train schedule that you simulate immediately after
	 * you started a thread for it.
	 *
	 * @param schedule the schedule that is simulated.
	 */
	public abstract void start(final TrainSchedule schedule);

	abstract void start(final long timestamp, final TrainSchedule schedule);

	/**
	 * Call this method for each train that leaves a location. This includes
	 * locations that the train only passes through.
	 *
	 * @param schedule the action is recorded for.
	 * @param location to leave.
	 */
	public abstract void leave(final TrainSchedule schedule, final Location location);

	abstract void leave(final long timestamp, final TrainSchedule schedule, final Location location);

	/**
	 * Call this method for each train that travels a connection
	 * <strong>before</strong> the train enters {@link Connection#travel()}.
	 *
	 * @param schedule the action is recorded for.
	 * @param section  to travel.
	 */
	public abstract void travel(final TrainSchedule schedule, final Connection section);

	abstract void travel(final long timestamp, final TrainSchedule schedule, final Connection section);

	/**
	 * Call this method for each train that arrives at a location. This includes
	 * locations that the train only passes through.
	 *
	 * @param schedule the action is recorded for.
	 * @param location to arrive at.
	 */
	public abstract void arrive(final TrainSchedule schedule, final Location location);

	abstract void arrive(final long timestamp, final TrainSchedule schedule, final Location location);

	/**
	 * Call this method for each train that pauses at a location that is not a
	 * station. A train pauses at a location if the location is the destination of
	 * the route but not the destination of the train schedule.
	 *
	 * @param schedule the action is recorded for.
	 * @param location to pause at.
	 */
	public abstract void pause(final TrainSchedule schedule, final Location location);

	abstract void pause(final long timestamp, final TrainSchedule schedule, final Location location);

	/**
	 * Call this method for each train that resumes its schedule after it paused at
	 * a non-station location.
	 *
	 * @param schedule the action is recorded for.
	 * @param location to resume from.
	 */
	public abstract void resume(final TrainSchedule schedule, final Location location);

	abstract void resume(final long timestamp, final TrainSchedule schedule, final Location location);

	/**
	 * Call this method for each train that arrives at its scheduled destination.
	 * This does not replace the call to {@link #arrive arrive} and must be called
	 * afterwards.
	 *
	 * @param schedule the schedule that was finished.
	 */
	public abstract void finish(final TrainSchedule schedule);

	abstract void finish(final long timestamp, final TrainSchedule schedule);

	/**
	 * Call this method after all trains completed their schedule.
	 */
	public abstract void done();

	abstract void done(final long timestamp);
}
