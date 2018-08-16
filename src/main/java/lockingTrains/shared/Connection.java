package lockingTrains.shared;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representation of a connection between two {@link Location}s.
 * <p>
 * <b>Important note:</b>
 * <p>
 * Even though this representation may appear to imply a directed connection
 * (graph edge), it really is not. A connection from {@code A} to {@code B} is
 * no different than a connection from {@code B} to {@code A}. In particular, if
 * there is a connection from {@code A} to {@code B} and a connection from
 * {@code B} to {@code A} then there are two undirected connections between
 * {@code A} and {@code B}, allowing two trains to travel between {@code A} and
 * {@code B} at the same time.
 */
public class Connection extends Position {
	/**
	 * Counts the instances to assign unique ID's to them.
	 */
	private static int counter = 0;

	private final Location first;
	private final Location second;
	private final int time;
	private final int id;

	public int getRomanAndAntoineID() {
		return romanAndAntoineID;
	}

	public void setRomanAndAntoineID(int romanAndAntoineID) {
		this.romanAndAntoineID = romanAndAntoineID;
	}

	private int romanAndAntoineID;

	/**
	 *
	 */
	private final Lock lock;

	/**
	 * Constructs a new connection.
	 *
	 * @param first  one of the connected locations.
	 * @param second the other connected location.
	 * @param time   time needed to travel the connection, in
	 *               {@link java.util.concurrent.TimeUnit#MILLISECONDS}.
	 */
	public Connection(final Location first, final Location second, final int time) {
		this.first = first;
		this.second = second;
		this.time = time;
		this.id = counter++;
		lock = new ReentrantLock();
	}

	/**
	 * Get lock of Connection
	 * @return Lock of the Connection
	 */
	public Lock getLock() {
		return lock;
	}

	/**
	 * Get the first of connected locations.
	 *
	 * @return The first of connected locations.
	 */
	public Location first() {
		return first;
	}

	/**
	 * Get the second of the connected locations.
	 *
	 * @return The second of the connected locations.
	 */
	public Location second() {
		return second;
	}

	/**
	 * Get the time needed to travel the connection.
	 * <p>
	 * Uses {@link java.util.concurrent.TimeUnit#MILLISECONDS}.
	 *
	 * @return The time needed to travel the connection.
	 */
	public int time() {
		return time;
	}

	/**
	 * Lets the calling thread sleep for the travel time of the connection. Each
	 * train must call this whenever it passes this connection.
	 *
	 * @throws InterruptedException if {@link Thread#sleep(long) Thread.sleep}
	 *                              throws an {@link InterruptedException}.
	 */
	public void travel() throws InterruptedException {
		Thread.sleep(time);
	}

	/**
	 * Get the unique ID assigned during construction.
	 *
	 * @return The unique ID assigned during construction.
	 */
	public int id() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("%s<-(%d)->%s", first, time, second);
	}
}
