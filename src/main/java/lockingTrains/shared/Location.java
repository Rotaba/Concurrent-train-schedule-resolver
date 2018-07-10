package lockingTrains.shared;

import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representation of a map location. Locations can be categorized into one of
 * the following three types. A location may be a:
 *
 * <ol>
 * <li>station (infinite capacity; project description: Bahnhof)</li>
	 * <li>crossing (capacity of 0; project description: Verbindungspunkt)</li>
 * <li>siding (finite positive capacity; project description: Knotenpunkt)</li>
 * </ol>
 */
public class Location extends Position {
	/**
	 * Abstraction for representing capacities, including infinite ones.
	 */
	public static class Capacity {
		/**
		 * The infinite capacity. You may not assume its internal representation.
		 */
		public static final Capacity INFINITE = new Capacity(-1);

		private final int value;
		private int reservedParking;

		private Capacity(final int value) {
			this.value = value;
			this.reservedParking = 0;
		}

		/**
		 * Constructs a capacity given an integer value.
		 *
		 * @param value to construct the capacity for.
		 *
		 * @return A new capacity for a non-negative input or {@link #INFINITE} for
		 *         {@code -1}.
		 *
		 * @throws IllegalArgumentException if {@code value} is smaller than {@code -1}.
		 */
		public static Capacity get(final int value) {
			if (value < -1)
				throw new IllegalArgumentException("Cannot construct capacity for value " + value + "!");
			else if (value == -1)
				return INFINITE;
			else
				return new Capacity(value);
		}

		/**
		 * Use this to distinguish between the infinite capacity and finite ones.
		 *
		 * @return {@code true} iff called on the infinite capacity.
		 */
		public boolean isInfinite() {
			return this == INFINITE;
		}

		/**
		 * Integer value of the capacity. The infinite capacity does not have an integer
		 * value!
		 *
		 * @return The integer value of the capacity.
		 *
		 * @throws NoSuchElementException if called on the infinite capacity.
		 */
		public int value() {
			if (isInfinite())
				throw new NoSuchElementException("Infinite capacity cannot have finite bound!");
			return value;
		}
		public synchronized int reservedParking() {
			return reservedParking;
		}
		public synchronized void reserve() {
			reservedParking++;
		}
		public void leave() {
			reservedParking--;
		}
	}

	/**
	 * Counts the instances to assign unique ID's to them.
	 */
	private static int counter = 0;

	private final String name;
	private final Capacity capacity;
	private final int x;
	private final int y;
	private final int id;
	private final Lock lock;

	/**
	 * Constructs a new location.
	 *
	 * @param name     the printable name of the location.
	 * @param capacity the abstracted capacity of the location.
	 * @param x        coordinate on the {@link Map}.
	 * @param y        coordinate on the {@link Map}.
	 */
	public Location(final String name, final Capacity capacity, final int x, final int y) {
		this.name = name;
		this.capacity = capacity;
		this.x = x;
		this.y = y;
		this.id = counter++;
		this.lock = new ReentrantLock();
	}


	public Lock getLock() {
		return lock;
	}
	synchronized public boolean reserveParking()  {
		if(isStation()) return true;
		if(capacity.value() == 0) {
			return false;
		} else if(capacity.value() > capacity.reservedParking()) {
			capacity.reserve();
			return true;
		} else if(capacity.value() == capacity.reservedParking()) {
			//sadly no free parking possibility
			return false;
		} else {
			System.out.println("unhandled if statement, ");
			throw new IllegalStateException();
		}
	}



	synchronized public void freeParking() {
		capacity.leave();
	}

	/**
	 * Get the printable name of the location.
	 *
	 * @return The printable name of the location.
	 */
	public String name() {
		return name;
	}

	/**
	 * Get the abstracted capacity of the location.
	 *
	 * @return The abstracted capacity of the location.
	 */
	public int capacity() {
		return capacity.value();
	}

	/**
	 * Get the x coordinate on the {@link Map}.
	 *
	 * @return The x coordinate on the {@link Map}.
	 */
	public int x() {
		return x;
	}

	/**
	 * Get the y coordinate on the {@link Map}.
	 *
	 * @return The y coordinate on the {@link Map}.
	 */
	public int y() {
		return y;
	}

	/**
	 * Get the unique ID assigned during construction.
	 *
	 * @return The unique ID assigned during construction.
	 */
	public int id() {
		return id;
	}

	/**
	 * Checks whether the location is a station.
	 *
	 * @return {@code true} if the location is a station, i.e. has infinite
	 *         capacity.
	 */
	public boolean isStation() {
		return capacity.isInfinite();
	}

	@Override
	public String toString() {
		if (isStation())
			return name;
		else
			return String.format("%s(%d)", name, capacity());
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null)
			return false;
		if (this == other)
			return true;
		if (!(other instanceof Location))
			return false;

		final var otherLocation = (Location) other;
		return name.equals(otherLocation.name);
	}
}
