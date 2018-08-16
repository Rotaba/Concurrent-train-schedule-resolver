package lockingTrains.impl;

import lockingTrains.shared.*;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * our trains which extend thread
 */
public class Train extends Thread {
    private final TrainSchedule trainSchedule;
    private final Recorder recorder;
    private final Map map;
    private final TrainService trainService;
    private Location currentLocation;
    private final int id;
    private static int counter = 0; //DEBUG
    private boolean error = false;
    private boolean parking = false;

    private List<Position> empty = new ArrayList<>(); //used in run()


    /**
     * Constructor for class; called in Simulator
     * @param trainSchedule  the trainSchedule
     * @param recorder shared among all trains
     * @param map needed to compute route
     * @param trainService shared among all trains
     */
    public Train(TrainSchedule trainSchedule, Recorder recorder, Map map, TrainService trainService) {
        this.trainSchedule = trainSchedule;
        this.recorder = recorder;
        this.map = map;
        this.trainService = trainService;
        this.currentLocation = trainSchedule.origin();
        this.id = counter++;

    }

    /**
     * Main run() method of our Train-Thread; engulfed in a try{}catch{} to grab any pesky Exceptions
     * 1.get map.route; if possible try to reserve; if reservable drive()
     * 2.if not possible to reserve - check why not; use this as the "avoid" in next map.route to get alternative route
     * 3.check if alternative route is reservable; if not check why and run the map.route with new "avoid" on step 2.
     * 4.if no alternative route is possible find a parking on route to dst and wait for it to be reservable
     */
    public void run() {
        try {
            List<Connection> route;
            recorder.start(trainSchedule);
            currentLocation.reserveParking(); //because the project definition says, we have to free the location
            while (true) {
                if (currentLocation.equals(trainSchedule.destination())) {
                    recorder.finish(trainSchedule);
                    return;
                }
                route = map.route(currentLocation, trainSchedule.destination(), empty);
                Collection<Position> alreadyTaken = new LinkedList<>();
                Position isTaken = trainService.reserveRoute(route, currentLocation, id);
                if (isTaken == null) {
                    //route was reserved
                    drive(route);
                } else {
                    //could not reserve whole route - need to check whats the problem and ask to reserve again

                    while(true) {
                        alreadyTaken.add(isTaken);
                        //update route to take the new "avoid" into account
                        route = map.route(currentLocation, trainSchedule.destination(), alreadyTaken);
                        if (route != null) {
                            //we found an alternative route
                            isTaken = trainService.reserveRoute(route, currentLocation, id);
                            if (isTaken == null) { //try to reserve from TrainService
                                drive(route); //drive using the newly reserved route
                                break;
                            }
                            //can't reserve route - try alreadyTaken again

                        } else { // there's no route possible - break and go to next ParkingPlace Phase
                            route = map.route(currentLocation, trainSchedule.destination(), empty);
                            //find nearest parking to destination
                            route = findAndReserveParking(route);
                            //reserveParking will always give back a possible route
                            trainService.waitingforReservedRoute(route, currentLocation, id);
                            drive(route); //finally we can drive to the parkingPlace!
                            break;
                        }
                    }

                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            error = true; //for isError()
        }
        //VERYIMPORTANTEDIT
        catch (AssertionError ass) {
            print("AssertionError in train ");
            error = true;
        }
    }

    /**
     * used for catch{} in Simulator for Exceptions
     * @return the local var error
     */
    public boolean isError() {
        return error;
    }


    /**
     * Get your motor running, get on the highway! looking for adventure - and whatever comes our way!
     *
     * We get a reserved route; including locations on the way and parking - if applicable
     * if we resume form parking; and its not a station - call resume and unset flag
     * iterate over route and call leave on current, free the current location,
     * travel over connection arrive to next location and update current
     * if last location is a station; we're supposed to park there - set flag and if not Station call pause
     * finally unlock current location
     *
     * @param connections the route to drive, it is never {@code null}
     * @throws InterruptedException when interrupted while travelling
     */
    private void drive(List <Connection> connections) throws InterruptedException {
        currentLocation.freeParking();
        if(parking) {
            recorder.resume(trainSchedule, currentLocation);
            parking = false;
        }
        assert (connections != null);
        Connection c;
        while(!connections.isEmpty()) {
            c = connections.remove(0);
            recorder.leave(trainSchedule, currentLocation);
            trainService.freeLocation(currentLocation, id);
            recorder.travel(trainSchedule, c);
            c.travel();
            if(c.first().equals(currentLocation)) {
                recorder.arrive(trainSchedule, c.second());
                currentLocation = c.second();
            }
            else if(c.second().equals(currentLocation)) {
                recorder.arrive(trainSchedule, c.first());
                currentLocation = c.first();
            }
            else {
                System.out.println("SOMETHING WENT TOTALLY WRONG, connection tryed which is not currently reachable");
                throw new IllegalStateException();
            }
            trainService.freeConnection(c, id);
        }
        if(!currentLocation.isStation()) {
            recorder.pause(trainSchedule, currentLocation);
            parking = true;
        }
        trainService.freeLocation(currentLocation, id);
    }



    /**
     * finds the next parking, and reserves it
     * @param route on which we need to find parking
     * @return the route to parking
     */
    private List <Connection> findAndReserveParking(List <Connection> route)  {
        Location current = currentLocation;
        Location canPark;
        LinkedList <Connection> returnRoute = new LinkedList<>();
        for(Connection c : route) {
            if(c.first() == current) {
                canPark = c.second();
            }
            else if (c.second() == current) {
                canPark = c.first();
            }
            else {
                print("in findAndReserveParking, the connections do not drive to destination");
                throw new IllegalStateException();
            }

            if(canPark.reserveParking()) {
                returnRoute.addLast(c);
                return returnRoute;
            }
            else {
                current = canPark;
                returnRoute.add(c);
            }
        }
        print("This state should never be reached, as the reserved parking should at least be the destination " +
                "station");
        throw new IllegalStateException();
    }





    private void print(String str) {
        System.out.println(str);
    }

}
