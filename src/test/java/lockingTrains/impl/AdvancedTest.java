package lockingTrains.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.Problem;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.validation.CatRecorder;
import lockingTrains.validation.EmptyScheduleRecorder;
import lockingTrains.validation.ErrorRecorder;
import lockingTrains.validation.ExceptionRecorder;
import lockingTrains.validation.ParkingCounter;
import lockingTrains.validation.SlowValidator;
import lockingTrains.validation.Validator;

public class AdvancedTest {
	@Test(timeout = 100)
	public void emptySchedulePositive() {
		final var map = new Map("", Collections.emptyList(), Collections.emptyList());
		final var problem = new Problem(map, Collections.emptyList());

		final var recorder = new EmptyScheduleRecorder();
		assertTrue(Simulator.run(problem, recorder));
		assertTrue(recorder.doneCalled());
	}

	@Test(timeout = 100)
	public void emptyScheduleException() {
		final var map = new Map("", Collections.emptyList(), Collections.emptyList());
		final var problem = new Problem(map, Collections.emptyList());

		assertFalse(Simulator.run(problem, new ExceptionRecorder()));
	}

	@Test(timeout = 100)
	public void emptyScheduleError() {
		final var map = new Map("", Collections.emptyList(), Collections.emptyList());
		final var problem = new Problem(map, Collections.emptyList());

		try {
			assertFalse(Simulator.run(problem, new ErrorRecorder()));
		} catch (AssertionError e) {
			return;
		} catch (Throwable e) {
			assert false;
		}
	}

	@Test(timeout = 100)
	public void errorRecorder() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, b));

		final var problem = new Problem(map, schedules);

		assertFalse(Simulator.run(problem, new ErrorRecorder()));
	}

	@Test(timeout = 100)
	public void idsNotStartingAtZeroAndNotContinous() {
		new Location("a", Location.Capacity.INFINITE, 0, 0);
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		new Location("b", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);

		new Connection(a, b, 1);
		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1));
		new Connection(a, b, 1);
		connections.add(new Connection(b, a, 1));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, b));
		schedules.add(new TrainSchedule(a, b));
		schedules.add(new TrainSchedule(a, b));
		schedules.add(new TrainSchedule(a, b));

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new Validator(problem)));
	}

	@Test(timeout = 2000)
	public void concurrentSimulation() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final Location c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		final Location d = new Location("D", Location.Capacity.INFINITE, 0, 0);
		final Location e = new Location("E", Location.Capacity.INFINITE, 0, 0);
		final Location f = new Location("F", Location.Capacity.INFINITE, 0, 0);
		final Location g = new Location("G", Location.Capacity.INFINITE, 0, 0);
		final Location h = new Location("H", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);
		locations.add(e);
		locations.add(f);
		locations.add(g);
		locations.add(h);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1000));
		connections.add(new Connection(c, d, 1000));
		connections.add(new Connection(e, f, 1000));
		connections.add(new Connection(g, h, 1000));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, b));
		schedules.add(new TrainSchedule(c, d));
		schedules.add(new TrainSchedule(e, f));
		schedules.add(new TrainSchedule(g, h));

		final var problem = new Problem(map, schedules);

		final var start = System.currentTimeMillis();
		assertTrue(Simulator.run(problem, new Validator(problem)));
		final var stop = System.currentTimeMillis();

		final var time = stop - start;
		assertTrue(1000 <= time && time <= 1500);
	}

	@Test(timeout = 2500)
	public void alternativeRoute() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final Location c = new Location("C", Location.Capacity.get(0), 0, 0);
		final Location d = new Location("D", Location.Capacity.get(0), 0, 0);
		final Location e = new Location("E", Location.Capacity.INFINITE, 0, 0);
		final Location f = new Location("F", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);
		locations.add(e);
		locations.add(f);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, c, 1000));
		connections.add(new Connection(a, d, 750));
		connections.add(new Connection(b, c, 1000));
		connections.add(new Connection(b, d, 750));
		connections.add(new Connection(c, e, 1));
		connections.add(new Connection(c, f, 1));
		connections.add(new Connection(d, e, 750));
		connections.add(new Connection(d, f, 750));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, e));
		schedules.add(new TrainSchedule(b, f));

		final var problem = new Problem(map, schedules);

		final var start = System.currentTimeMillis();
		assertTrue(Simulator.run(problem, new Validator(problem)));
		final var stop = System.currentTimeMillis();

		final var time = stop - start;
		assertTrue(1500 <= time && time <= 2000);
	}

	@Test(timeout = 4000)
	public void optimizable() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.INFINITE, 0, 0);
		final Location c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1000));
		connections.add(new Connection(b, c, 750));
		connections.add(new Connection(c, b, 750));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, b));
		schedules.add(new TrainSchedule(a, b));

		final var problem = new Problem(map, schedules);

		final var start = System.currentTimeMillis();
		assertTrue(Simulator.run(problem, new Validator(problem)));
		final var stop = System.currentTimeMillis();

		final var time = stop - start;
		assertTrue(2000 <= time && time <= 3000);
	}

	@Test(timeout = 4000)
	public void parkingSimple() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.get(1), 0, 0);
		final Location c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1000));
		connections.add(new Connection(b, c, 1000));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, c));
		schedules.add(new TrainSchedule(a, c));

		final var problem = new Problem(map, schedules);

		final var start = System.currentTimeMillis();
		final var validator = new Validator(problem);
		final var parking = new ParkingCounter();
		assertTrue(Simulator.run(problem, new CatRecorder(List.of(validator, parking))));
		final var stop = System.currentTimeMillis();

		final var time = stop - start;
		assertTrue(3000 <= time && time <= 3500);

		assertEquals(1, parking.pauseCalls(b));
		assertEquals(1, parking.resumeCalls(b));
	}

	@Test(timeout = 10000)
	public void parkingAdvanced() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.get(1), 0, 0);
		final Location c = new Location("C", Location.Capacity.get(1), 0, 0);
		final Location d = new Location("D", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1000));
		connections.add(new Connection(b, c, 1000));
		connections.add(new Connection(c, d, 1000));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, d));
		schedules.add(new TrainSchedule(a, d));
		schedules.add(new TrainSchedule(a, d));

		final var problem = new Problem(map, schedules);

		final var start = System.currentTimeMillis();
		final var validator = new Validator(problem);
		final var parking = new ParkingCounter();
		assertTrue(Simulator.run(problem, new CatRecorder(List.of(validator, parking))));
		final var stop = System.currentTimeMillis();

		final var time = stop - start;
		assertTrue(7000 <= time && time <= 9000);

		assertEquals(1, parking.pauseCalls(b));
		assertEquals(1, parking.resumeCalls(b));
		assertEquals(1, parking.pauseCalls(c));
		assertEquals(1, parking.resumeCalls(c));
	}

	@Test(timeout = 1000)
	public void rushB() {
		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.get(1), 0, 0);
		final Location c = new Location("C", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 0));
		connections.add(new Connection(b, c, 10));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		for (int i = 0; i < 5; ++i)
			schedules.add(new TrainSchedule(a, c));

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new SlowValidator(problem, b)));
	}
}
