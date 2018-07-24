


package lockingTrains.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lockingTrains.shared.*;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.shared.io.Parser;
import lockingTrains.validation.CatRecorder;
import lockingTrains.validation.Logger;
import lockingTrains.validation.Recorder;
import lockingTrains.validation.Validator;

import static java.lang.Thread.currentThread;

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
		final var logger = new Logger(); //our Validator/recorde thingy


		//GIVE FLASE if got an exception during exectution
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
		//init TrainService
		TrainService trainService = new TrainService(problem.map());

		//init Trains with their respectable TrainSchedules
		//get schedlu
		List<TrainSchedule> schedules = problem.schedules();
		//init list of trains to be populated later
		List<Train> trainList = new LinkedList<Train>();

        //start trains in a loop until they reach destination or get an exception
		//init Trains
		for (int i = 0; i < schedules.size(); i++){

			System.out.println("Train with id: " + schedules.get(i).id() + "gets the index: " + i);
		    Train train = new Train(schedules.get(i), trainService, recorder, problem.map());
            trainList.add(train);
		}

        //start Trains
        for (int i = 0; i < schedules.size(); i++){
            trainList.get(i).start();
			recorder.start(schedules.get(i)); //tell recorder this train started
        }

		//wait for Trains to finish
		for (Train t : trainList) {
			try {
				t.join(); //hold main until all threads are done terminating or return an exception
				if(t.isError()) {
					return false;
				}
			} catch (InterruptedException e) {
				return false;
			}
		}

		if(trainService.getDone() != schedules.size()){
			System.out.println("Something went bad; not all trains finished after .join()");
		}
		//per instruction - must notify the recorder
		recorder.done(); //call recorder to tell him we're done here

		return true;
	}
}
