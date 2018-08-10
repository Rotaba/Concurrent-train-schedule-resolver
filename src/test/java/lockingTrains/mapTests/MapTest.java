package lockingTrains.mapTests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import lockingTrains.impl.Simulator;
import lockingTrains.shared.io.Parser;
import lockingTrains.validation.Validator;

class MapTest {
	private String scanFile(File file) throws IOException {
		final var reader = new BufferedReader(new FileReader(file));
		final String res = reader.readLine();
		reader.close();
		return res;
	}

	@TestFactory
	public Collection<DynamicTest> runAllProblems() throws IOException {
		Collection<DynamicTest> dynamicTests = new ArrayList<>();

		final var mapsDir = new File("./src/test/resources");
		assert (mapsDir.isDirectory());

		Map<String, File> maps = new HashMap<>();
		Map<String, Set<File>> problems = new HashMap<>();

		for (final var file : mapsDir.listFiles()) {
			final String fileName = file.getName();
			if (fileName.endsWith(".map")) {
				final String mapName = scanFile(file);
				assertFalse(maps.containsKey(mapName), "duplicate map file");
				maps.put(mapName, file);
				problems.put(mapName, new HashSet<>());
			}
		}

		for (final var file : mapsDir.listFiles()) {
			final String fileName = file.getName();
			if (fileName.endsWith(".problem")) {
				final String mapName = scanFile(file);
				assertTrue(maps.containsKey(mapName), "missing map file");
				problems.get(mapName).add(file);
			}
		}

		for (final var entry : maps.entrySet()) {
			final String mapName = entry.getKey();
			final File mapFile = entry.getValue();
			for (final File problemFile : problems.get(mapName)) {
				dynamicTests.add(DynamicTest.dynamicTest("Problem " + problemFile.getName(), () -> {
					final var problem = Parser.parse(mapFile, problemFile);
					assertTrue(assertTimeoutPreemptively(Duration.ofSeconds(30), () -> Simulator.run(problem, new Validator(problem))));
				}));
			}
		}

		return dynamicTests;
	}
}
