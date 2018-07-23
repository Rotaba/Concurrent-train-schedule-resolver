package lockingTrains.impl;

import lockingTrains.shared.*;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
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
    private int connectionLocks = 0; //DEBUG
    private int locationLocks = 0; //DEBUG
    private boolean parking = false;

    private List<Position> empty = new ArrayList<>(); //used in run()


    /**
     * Constructor for class; called in Simulator
     * @param trainSchedule  given in Simulator from an Array
     * @param recorder given in Simulator
     * @param map fom problem
     * @param trainService that we inited in Simulator
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
            currentLocation.reserveParking();
            while (true) {
                route = map.route(currentLocation, trainSchedule.destination(), empty);
                if (trainService.reserveRoute(route, currentLocation, id)) {
                    connectionLocks += route.size(); //DEBUG
                    locationLocks += route.size() + 1; //DEBUG
                    //route was reserved
                    drive(route);
                } else {
                    //could not reserve whole route - need to check whats the problem and ask to reserve again
                    Collection<Position> alreadyTaken = new LinkedList<Position>();
                    while(true) {
                        alreadyTaken.addAll(trainService.getAlreadyTakenPosition(route, currentLocation, id));
                        //update route to take the new "avoid" into account
                        route = map.route(currentLocation, trainSchedule.destination(), alreadyTaken);
                        if (route != null) {
                            //we found an alternative route
                            if (trainService.reserveRoute(route, currentLocation, id)) { //try to reserve from TrainService
                                connectionLocks += route.size(); //DEBUG
                                locationLocks += route.size() + 1; //DEBUG
                                drive(route); //drive using the newly reserved route
                                break;
                            }
                            //can't reserve route - try alreadyTaken again
                        } else { // there's no route possible - break and go to next ParkingPlace Phase
                            route = map.route(currentLocation, trainSchedule.destination(), empty);
                            //find nearest parking to destination
                            route = findAndReserveParking(route);
                            //route will allways give back a possible route
                            trainService.waitingforReservedRoute(route, currentLocation, id);
                            connectionLocks += route.size();
                            locationLocks += route.size() + 1;
                            //assert(test == route.size() *2 + 1);
                            drive(route); //finally we can drive to the parkingPlace!
                            break;
                        }
                    }

                }

                if (currentLocation.equals(trainSchedule.destination())) {
                  //  print(connectionLocks + " locks " + locationLocks);
                    assert (connectionLocks == 0);
                    assert (0 == locationLocks);
                  //  print("finished event");
                    recorder.finish(trainSchedule);
                    trainService.setFinished();
                    return;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            error = true; //for isError()
        }
    }

    /**
     * used for catch{} in Simulator for Exceptions
     * @return the local var error
     */
    public boolean isError() {
        return error;
    }

    //we leverage, that the route is sorted such that the first element is the first connection
    //from origin that has to be taken, and last element is the connection to the destination


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
     * @param connections
     * @throws InterruptedException
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
            locationLocks--;
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
            connectionLocks--;
        }
        if(!currentLocation.isStation()) {
            recorder.pause(trainSchedule, currentLocation);
            parking = true;
        }
        trainService.freeLocation(currentLocation, id);
        locationLocks--;
    }



    /**
     * finds the next parking, and reserves it, if it's not a train station
     * @param route on which we need to find parking
     * @return the route without all connections from parking to destination, BUT null if the train is already on the next parking
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
