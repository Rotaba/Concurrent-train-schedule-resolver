package lockingTrains.shared.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import lockingTrains.shared.Map;

/**
 * <p>
 * This class allows you to export maps as PDF files. It is only useful if you
 * write your own maps.
 * </p>
 * 
 * <p>
 * To use it, a recent distribution of LaTeX needs to be installed, and
 * <code>pdflatex</code> must be on the path.
 * </p>
 * 
 * <p>
 * Simply running this class will export all maps. To only export some maps,
 * pass their names, without any extension, as arguments. The working directory
 * must be the root of the project repository.
 * </p>
 */
public class ExportTikZ {
	/**
	 * There is no need to construct instances of this class.
	 */
	private ExportTikZ() {
	}

	/**
	 * Export all maps, or the ones specified.
	 * 
	 * @param args List of map names without extension, or nothing to export all
	 *             maps.
	 * @throws IOException          if any file cannot be read or written
	 * @throws InterruptedException if the process is interrupted
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length > 0) {
			for (final String mapName : args) {
				System.out.println("Exporting map " + mapName + "...");
				exportMap(mapName);
			}
		} else {
			final var mapsDir = new File("./src/test/resources");
			assert (mapsDir.isDirectory());

			for (final var file : mapsDir.listFiles()) {
				final String fileName = file.getName();
				if (fileName.endsWith(".map")) {
					final String mapName = fileName.substring(0, fileName.length() - 4);
					System.out.println("Exporting map " + mapName + "...");
					exportMap(mapName);
				}
			}
		}

		System.out.println("Done.");
	}

	/**
	 * Exports a single map.
	 * 
	 * @param name the name of the map, without any extension
	 * @throws IOException          if any file cannot be read or written
	 * @throws InterruptedException if the process is interrupted
	 */
	public static void exportMap(final String name) throws IOException, InterruptedException {
		final var dir = new File("build/tikz-export");
		dir.mkdirs();

		final var writer = new FileWriter("build/tikz-export/" + name + ".tex");

		final var reader = new BufferedReader(new FileReader("./src/test/resources/" + name + ".map"));
		Map map = Parser.parseMap(reader);

		System.out.println("  Writing TₑX file...");

		writeMap(map, writer);

		writer.close();

		System.out.println("  Running pdfLᴬTₑX...");

		Process pdflatex = new ProcessBuilder("pdflatex", "-interaction=nonstopmode", name).directory(dir)
				.redirectErrorStream(true).redirectOutput(Redirect.DISCARD).start();
		pdflatex.getOutputStream().close();
		pdflatex.waitFor();

		if (pdflatex.exitValue() != 0) {
			throw new RuntimeException("Could not compile the map Tâ‚‘X file.");
		}
	}

	/**
	 * Write out the TikZ code for a single map.
	 * 
	 * @param map    the map to be exported
	 * @param writer where the TikZ code should be written
	 * @throws IOException if any file cannot be read or written
	 */
	public static void writeMap(final Map map, final FileWriter writer) throws IOException {
		writer.write("\\documentclass[tikz]{standalone}\n\n");

		writer.write("\\usetikzlibrary{calc}\n");
		writer.write("\\input{../../src/main/resources/trackstyle}\n\n");

		writer.write("\\begin{document}\n\n");

		writer.write("\\begin{tikzpicture}\n");

		for (final var location : map.locations()) {
			writer.write("    \\node [");

			if (location.isStation()) {
				writer.write("station={");
				writer.write(location.name().replace("_", " "));
				writer.write("}");
			} else {
				final var capacity = location.capacity();
				if (capacity > 0) {
					// identify angle
					// 0, 90, 180, 270
					boolean[] freeAngles = { true, true, true, true };
					map.connections().stream().filter(c -> c.first().equals(location) || c.second().equals(location))
							.forEach(c -> {
								var otherLoc = c.first();
								if (otherLoc.equals(location))
									otherLoc = c.second();
								final int dx = otherLoc.x() - location.x();
								final int dy = otherLoc.y() - location.y();
								if (dx == 0 && dy == 0)
									return;
								if (Math.abs(dx) >= Math.abs(dy)) {
									if (dx > 0) {
										freeAngles[0] = false;
									} else {
										freeAngles[2] = false;
									}
								} else {
									if (dy > 0) {
										freeAngles[3] = false;
									} else {
										freeAngles[1] = false;
									}
								}
							});
					int angle = 270;
					if (freeAngles[2])
						angle = 180;
					if (freeAngles[0])
						angle = 0;
					if (freeAngles[1])
						angle = 90;
					if (freeAngles[3])
						angle = 270;

					writer.write("siding={");
					writer.write(Integer.toString(capacity));
					writer.write("}{");
					writer.write(Integer.toString(angle));
					writer.write("}");
				} else {
					assert (capacity == 0);
					writer.write("crossing");
				}
			}

			writer.write("] at (");
			writer.write(Integer.toString(location.x()));
			writer.write(", ");
			writer.write(Integer.toString(-location.y()));
			writer.write(") (l");
			writer.write(Integer.toString(location.id()));
			writer.write(") {};\n");
		}

		writer.write("    \\begin{pgfonlayer}{tracks}\n");

		for (final var connection : map.connections()) {
			writer.write("        \\path [tracks] (l");
			writer.write(Integer.toString(connection.first().id()));
			writer.write(") to (l");
			writer.write(Integer.toString(connection.second().id()));
			writer.write(");\n");
		}

		writer.write("    \\end{pgfonlayer}{tracks}\n");

		writer.write("\\end{tikzpicture}\n\n");

		writer.write("\\end{document}\n");
	}

}
