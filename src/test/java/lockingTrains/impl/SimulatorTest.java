package lockingTrains.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.Problem;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.validation.ExceptionRecorder;
import lockingTrains.validation.Validator;

public class SimulatorTest {
	private final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
	private final Location b = new Location("B", Location.Capacity.INFINITE, 0, 0);
	private final Location c = new Location("C", Location.Capacity.INFINITE, 0, 0);
	private final Location d = new Location("D", Location.Capacity.INFINITE, 0, 0);
	private final Location e = new Location("E", Location.Capacity.INFINITE, 0, 0);
	private final Map map;

	public SimulatorTest() {
		final var locations = new ArrayList<Location>();
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);
		locations.add(e);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1));
		connections.add(new Connection(b, c, 1));
		connections.add(new Connection(c, d, 1));
		connections.add(new Connection(d, e, 1));

		this.map = new Map("", locations, connections);
	}

	@Test(timeout = 100)
	public void testErrorReporting() {
		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(e, a));

		final var problem = new Problem(map, schedules);

		assertFalse("Simulator.run() does not test for exceptions!", Simulator.run(problem, new ExceptionRecorder()));
	}

	@Test(timeout = 100)
	public void singleTrain() {
		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(e, a));

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new Validator(problem)));
	}

	@Test(timeout = 100)
	public void twoTrains() {
		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(e, a));
		schedules.add(new TrainSchedule(e, a));

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new Validator(problem)));
	}

	@Test(timeout = 100)
	public void twoTrainsOpposing() {
		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(e, a));
		schedules.add(new TrainSchedule(a, e));

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new Validator(problem)));
	}

	@Test(timeout = 1000)
	public void complex() {
		final var schedules = new ArrayList<TrainSchedule>();
		for (final var locationA : map.locations()) {
			for (final var locationB : map.locations()) {
				if (locationA.equals(locationB))
					continue;

				schedules.add(new TrainSchedule(locationA, locationB));
			}
		}

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new Validator(problem)));
	}

	@Test(timeout = 100)
	public void connectionSwitchDirection() {

		final var locations = new ArrayList<Location>();
		final Location a = new Location("A", Location.Capacity.INFINITE, 0, 0);
		final Location b = new Location("B", Location.Capacity.get(0), 0, 0);
		final Location c = new Location("C", Location.Capacity.get(0), 0, 0);
		final Location d = new Location("D", Location.Capacity.INFINITE, 0, 0);
		locations.add(a);
		locations.add(b);
		locations.add(c);
		locations.add(d);

		final var connections = new ArrayList<Connection>();
		connections.add(new Connection(a, b, 1));
		connections.add(new Connection(b, c, 1));
		connections.add(new Connection(d, c, 1));

		final var map = new Map("", locations, connections);

		final var schedules = new ArrayList<TrainSchedule>();
		schedules.add(new TrainSchedule(a, d));
		schedules.add(new TrainSchedule(a, d));

		final var problem = new Problem(map, schedules);

		assertTrue(Simulator.run(problem, new Validator(problem)));
	}
}