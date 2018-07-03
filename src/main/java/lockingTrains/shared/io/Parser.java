package lockingTrains.shared.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.Problem;
import lockingTrains.shared.TrainSchedule;

/**
 * The class {@link Parser} provides a single static method {@link #parse parse}
 * for parsing map and problem files.
 * <p>
 * It cannot be instantiated as it does not need complex state information. Thus
 * it solely relies on internal calls to helper methods.
 * <p>
 * The file formats for the map and problem files are given below. They use the
 * following conventions:
 * <ul>
 * <li><code>name ∈ [a-zA-Z0-9_]+</code></li>
 * <li><code>capacity ∈ [-1,∞)</code></li>
 * <li><code>coord ∈ (-∞,∞)</code></li>
 * <li><code>time ∈ [0,∞)</code></li>
 * </ul>
 * <p>
 * Map files:
 * 
 * <pre>
 * {@code
 * name
 * name:capacity:(coord,coord)
 * ...
 * name:capacity:(coord,coord)
 * name-name:time
 * ...
 * name-name:time
 * }
 * </pre>
 * <p>
 * Problem files:
 * 
 * <pre>
 * {@code
 * name
 * name->name
 * ...
 * name->name
 * }
 * </pre>
 * <p>
 * Here is an example pair of input files that can be parsed successfully by the
 * {@link #parse parse} method:
 * <p>
 * {@code example.map}
 * 
 * <pre>
 * {@code
 * example
 * Saarbruecken:-1:(4923,700)
 * Paris:-1(4886,235)
 * Saarbruecken-Paris:7200000
 * }
 * </pre>
 * <p>
 * {@code example.problem}
 * 
 * <pre>
 * {@code
 * example
 * Saarbruecken->Paris
 * Paris->Saarbruecken
 * }
 * </pre>
 */
public class Parser {
	/**
	 * Default constructor is not needed and thus inaccessible.
	 */
	private Parser() {
	}

	/**
	 * Regex for matching names (maps and locations).
	 */
	private static final String REGEX_NAME = "\\w+";
	/**
	 * Regex for matching integers.
	 */
	private static final String REGEX_NUMBER = "[-+]?\\d+";

	/**
	 * Parses a new {@link Problem}, given the map and problem files to read from.
	 * <p>
	 * This method may throw an {@link IllegalArgumentException} in situations where
	 * the given input files cannot be parsed into a consistent problem description.
	 * Inconsistencies include:
	 * <ul>
	 * <li>One of the files uses an inappropriate input format.</li>
	 * <li>The name of the map and problem file do not match.</li>
	 * <li>A location in the map file is declared more than once.</li>
	 * <li>A capacity specification for a location in the map file is
	 * nonsensical.</li>
	 * <li>A connection in the map file uses locations that were not declared in the
	 * same file.</li>
	 * <li>A connection in the map file uses a nonsensical travel time.</li>
	 * <li>A train schedule listed in the problem file uses locations that were not
	 * declared in the map file.</li>
	 * </ul>
	 *
	 * @param mapFile     map file to read from.
	 * @param problemFile problem file to read from.
	 *
	 * @return The parsed {@link Problem}.
	 *
	 * @throws IOException              if an error occurs while reading one of the
	 *                                  files.
	 * @throws IllegalArgumentException if the given files lead to an inconsistent
	 *                                  {@link Problem}.
	 * @see Problem
	 */
	public static Problem parse(final File mapFile, final File problemFile) throws IOException {
		Map map;
		try (final var reader = new BufferedReader(new FileReader(mapFile))) {
			map = parseMap(reader);
		}

		try (final var reader = new BufferedReader(new FileReader(problemFile))) {
			return parseProblem(reader, map);
		}
	}

	/**
	 * Parses a single location and adds it to a list.
	 *
	 * @param line      the line to parse.
	 * @param locations the list of locations.
	 *
	 * @return {@code true} if the line could be parsed successfully.
	 *
	 * @throws IllegalArgumentException if the name of the location is already
	 *                                  taken.
	 */
	static boolean parseLocation(final String line, final List<Location> locations) {
		final String GROUP_NAME = "name";
		final String GROUP_CAPACITY = "capacity";
		final String GROUP_X = "x";
		final String GROUP_Y = "y";

		final var pattern = Pattern.compile(String.format("^(?<%s>%s):(?<%s>%s):\\((?<%s>%s),(?<%s>%s)\\)$", GROUP_NAME,
				REGEX_NAME, GROUP_CAPACITY, REGEX_NUMBER, GROUP_X, REGEX_NUMBER, GROUP_Y, REGEX_NUMBER));
		final var matcher = pattern.matcher(line);
		if (!matcher.matches())
			return false;

		final String name = matcher.group(GROUP_NAME);
		final int capacity = Integer.parseInt(matcher.group(GROUP_CAPACITY));
		final int x = Integer.parseInt(matcher.group(GROUP_X));
		final int y = Integer.parseInt(matcher.group(GROUP_Y));

		final var location = new Location(name, Location.Capacity.get(capacity), x, y);
		final boolean nameTaken = locations.stream().anyMatch(l -> l.equals(location));
		if (nameTaken)
			throw new IllegalArgumentException("Location names must be unique!");
		locations.add(location);

		return true;
	}

	/**
	 * Parses a single connection and adds it to a list.
	 * <p>
	 * Also checks whether the connected locations are present in a list of
	 * locations.
	 *
	 * @param line        the line to parse.
	 * @param locations   the list of locations.
	 * @param connections the list of connections.
	 *
	 * @return {@code true} if the line could be parsed successfully.
	 *
	 * @throws IllegalArgumentException if the connected locations are not present
	 *                                  in the list of locations or the travel time
	 *                                  is negative.
	 */
	static boolean parseConnection(final String line, List<Location> locations, final List<Connection> connections) {
		final String GROUP_NAME_A = "nameA";
		final String GROUP_NAME_B = "nameB";
		final String GROUP_TIME = "time";

		final var pattern = Pattern.compile(String.format("^(?<%s>%s)-(?<%s>%s):(?<%s>%s)$", GROUP_NAME_A, REGEX_NAME,
				GROUP_NAME_B, REGEX_NAME, GROUP_TIME, REGEX_NUMBER));
		final var matcher = pattern.matcher(line);
		if (!matcher.matches())
			return false;

		final String nameA = matcher.group(GROUP_NAME_A);
		final String nameB = matcher.group(GROUP_NAME_B);
		final int time = Integer.parseInt(matcher.group(GROUP_TIME));
		if (time < 0)
			throw new IllegalArgumentException("Travel times must be non-negative!");

		final var locationA = locations.stream().filter(l -> l.name().equals(nameA)).findAny();
		final var locationB = locations.stream().filter(l -> l.name().equals(nameB)).findAny();

		if (!locationA.isPresent() || !locationB.isPresent())
			throw new IllegalArgumentException(
					String.format("Could not find location \"%s\" or \"%s\"!", nameA, nameB));

		final var connection = new Connection(locationA.get(), locationB.get(), time);
		connections.add(connection);

		return true;
	}

	/**
	 * Parses a given input as a map file.
	 *
	 * @param reader input to be parsed.
	 *
	 * @return The parsed {@link Map}.
	 *
	 * @throws IOException              if the input cannot be read.
	 * @throws IllegalArgumentException if the input does not meet the specification
	 *                                  for map files.
	 */
	static Map parseMap(final BufferedReader reader) throws IOException {
		final String name = reader.readLine();

		if (!name.matches(String.format("^%s$", REGEX_NAME))) {
			throw new IllegalArgumentException(
					String.format("Map file must start with a name! \"%s\" is not a valid name!", name));
		}

		final var locations = new ArrayList<Location>();
		String line;
		do {
			line = reader.readLine();
		} while (line != null && parseLocation(line, locations));

		final var connections = new ArrayList<Connection>();
		while (line != null && parseConnection(line, Collections.unmodifiableList(locations), connections)) {
			line = reader.readLine();
		}

		if (line != null) {
			throw new IllegalArgumentException(
					String.format("\"%s\" is not a valid input line for the map file!", line));
		}

		return new Map(name, locations, connections);
	}

	/**
	 * Parses a single train schedule and adds it to a list.
	 * <p>
	 * Also checks whether the scheduled origin and destination match existing
	 * locations.
	 *
	 * @param line      the line to parsed.
	 * @param locations the list of locations.
	 * @param schedules the list of schedules.
	 *
	 * @return {@code true} if the line could be parsed successfully.
	 */
	static boolean parseSchedule(final String line, List<Location> locations, final List<TrainSchedule> schedules) {
		final String GROUP_NAME_A = "nameA";
		final String GROUP_NAME_B = "nameB";

		final var pattern = Pattern
				.compile(String.format("^(?<%s>%s)->(?<%s>%s)$", GROUP_NAME_A, REGEX_NAME, GROUP_NAME_B, REGEX_NAME));
		final var matcher = pattern.matcher(line);
		if (!matcher.matches())
			return false;

		final String nameA = matcher.group(GROUP_NAME_A);
		final String nameB = matcher.group(GROUP_NAME_B);

		final var locationA = locations.stream().filter(l -> l.name().equals(nameA)).findAny();
		final var locationB = locations.stream().filter(l -> l.name().equals(nameB)).findAny();

		if (!locationA.isPresent() || !locationB.isPresent())
			throw new IllegalArgumentException(
					String.format("Could not find location \"%s\" or \"%s\"!", nameA, nameB));

		final var schedule = new TrainSchedule(locationA.get(), locationB.get());
		schedules.add(schedule);

		return true;
	}

	/**
	 * Parses a given input as problem file.
	 *
	 * @param reader input to be parsed.
	 * @param map    the map the problem should correspond to.
	 *
	 * @return The parsed {@link Problem}.
	 *
	 * @throws IOException              if the input cannot be read.
	 * @throws IllegalArgumentException if the input does not meet the specification
	 *                                  for problem files.
	 */
	static Problem parseProblem(final BufferedReader reader, final Map map) throws IOException {
		final String name = reader.readLine();

		if (!name.equals(map.name())) {
			throw new IllegalArgumentException(
					String.format("Name in problem file (%s) does not match name of the map (%s)!", name, map.name()));
		}

		final var schedules = new ArrayList<TrainSchedule>();
		String line;
		do {
			line = reader.readLine();
		} while (line != null && parseSchedule(line, map.locations(), schedules));

		if (line != null) {
			throw new IllegalArgumentException(
					String.format("\"%s\" is not a valid input line for the problem file!", line));
		}

		return new Problem(map, schedules);
	}
}
