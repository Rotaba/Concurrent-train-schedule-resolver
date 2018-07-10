package lockingTrains.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lockingTrains.shared.*;
import lockingTrains.shared.io.Parser;
import lockingTrains.validation.CatRecorder;
import lockingTrains.validation.Logger;
import lockingTrains.validation.Recorder;
import lockingTrains.validation.Validator;

/**
 * This is the starting point of your implementation. Feel free to add
 * additional method, but carefully read each existing method's documentation
 * before changing them.
 */
public class Simulator {
	private Simulator() {
	}

	/**
	 * Entrypoint for the simulator application.
	 *
	 * You may extend this (although you should not need to), but <strong>you must
	 * continue supporting the already implemented call scheme</strong>.
	 *
	 * @param args the command line arguments.
	 *
	 * @throws IOException if an error occurs while reading the input files.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: <command> <map file> <problem file>");
			System.exit(1);
		}

		final var problem = Parser.parse(new File(args[0]), new File(args[1]));
		final var logger = new Logger();

		final boolean result = run(problem, new CatRecorder(List.of(logger, new Validator(problem))));

		logger.eventLog().forEach(System.out::println);

		if (!result)
			System.exit(1);
	}

	/**
	 * Runs the entire problem simulation. The actions performed must be recorded
	 * using the interface {@link Recorder}. If the {@link Recorder} throws an
	 * exception, this method <strong>must</strong> return {@code false}.
	 *
	 * <strong>You may not change the signature of this method.</strong>
	 *
	 * @param problem  the problem to simulate.
	 * @param recorder the recorder instance to call.
	 *
	 * @return {@code true} if the simulation ran successfully.
	 */
	public static boolean run(final Problem problem, final Recorder recorder) {
		// TODO Start your implementation here.
		List<TrainSchedule> schedules = problem.schedules();
		Map map = problem.map();
		TrainService trainService = new TrainService();
		Train[] trains = new Train[schedules.size()];
		for (int i = 0; i < schedules.size(); i++) {
			trains[i] = new Train(schedules.get(i), recorder, map, trainService);
			trains[i].start();
			//	print("started one train "+ i);
		}
		for (int i = 0; i < schedules.size(); i++) {
			try {
				trains[i].join();
				if(trains[i].isError()) return false;
			} catch (Exception e) {
				print("simulation interrupted, who was this");
				return false;
			}
		}
		recorder.done();
		return true;
	}
	private static void print(String str) {
		System.out.println(str);
	}

}
