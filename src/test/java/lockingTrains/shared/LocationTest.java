package lockingTrains.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocationTest {
	private static Location constructCity() {
		return new Location("Saarbruecken", Location.Capacity.INFINITE, 0, 0);
	}

	private static Location constructCrossing() {
		return new Location("Crossing", Location.Capacity.get(0), 1, 2);
	}

	private static Location constructSiding() {
		return new Location("Siding", Location.Capacity.get(42), 21, -21);
	}

	@Test
	public void infiniteCapacity() {
		assertSame(Location.Capacity.INFINITE, Location.Capacity.get(-1));
		assertTrue(Location.Capacity.INFINITE.isInfinite());
	}

	@Test(expected = RuntimeException.class)
	public void infiniteCapacityNoValue() {
		Location.Capacity.INFINITE.value();
	}

	@Test
	public void validCapacity() {
		final var cap = Location.Capacity.get(42);
		assertFalse(cap.isInfinite());
		assertEquals(42, cap.value());
	}

	@Test(expected = RuntimeException.class)
	public void invalidCapacity() {
		Location.Capacity.get(-2);
	}

	@Test
	public void validCity() {
		final var city = constructCity();
		assertNotNull(city);
		assertEquals("Saarbruecken", city.name());
		assertEquals(0, city.x());
		assertEquals(0, city.y());
		assertTrue(city.isStation());
	}

	@Test
	public void validCrossing() {
		final var crossing = constructCrossing();
		assertNotNull(crossing);
		assertEquals("Crossing", crossing.name());
		assertEquals(1, crossing.x());
		assertEquals(2, crossing.y());
		assertEquals(0, crossing.capacity());
		assertFalse(crossing.isStation());
	}

	@Test
	public void validSiding() {
		final var siding = constructSiding();
		assertNotNull(siding);
		assertEquals("Siding", siding.name());
		assertEquals(21, siding.x());
		assertEquals(-21, siding.y());
		assertEquals(42, siding.capacity());
		assertFalse(siding.isStation());
	}

	@Test(expected = RuntimeException.class)
	public void cityNoCapacity() {
		constructCity().capacity();
	}
}
