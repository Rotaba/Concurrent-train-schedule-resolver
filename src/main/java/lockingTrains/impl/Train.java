package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;
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
    private static int counter = 0;
    private boolean error = false;
    private int connectionLocks = 0;
    private int locationLocks = 0;
    private boolean parking = false;

    //an empty List of Connections to call map.route with an empty list to avoid
    /**
     *
     */
    private List<Connection> empty = new ArrayList<>();


    /**
     *
     * @param trainSchedule
     * @param recorder
     * @param map
     * @param trainService
     */
    public Train(TrainSchedule trainSchedule, Recorder recorder, Map map, TrainService trainService) {
        this.trainSchedule = trainSchedule;
        this.recorder = recorder;
        this.map = map;
        this.trainService = trainService;
        this.currentLocation = trainSchedule.origin();
        this.id = counter++;

    }


    public void run() {
        try {
            List<Connection> route;
            recorder.start(trainSchedule);
            currentLocation.reserveParking();
            while (true) {
                route = map.route(currentLocation, trainSchedule.destination(), empty);
                if (trainService.reserveRoute(route, currentLocation, id)) {
                    connectionLocks += route.size();
                    locationLocks += route.size() + 1;
                    //route was reserved
                    drive(route);
                } else {
                    //could not reserve whole route
                    Collection<Connection> alreadyTaken;
                    while(true) {
                        alreadyTaken = trainService.getAlreadyTakenConnections(route, id);
                        //update route
                        route = map.route(currentLocation, trainSchedule.destination(), alreadyTaken);
                        if (route != null) {
                            //we found an alternative route
                            if (trainService.reserveRoute(route, currentLocation, id)) {
                                connectionLocks += route.size();
                                locationLocks += route.size() + 1;
                                drive(route);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (route == null) {
                        //there is no route without reserved parts
                        route = map.route(currentLocation, trainSchedule.destination(), empty);
                        //find nearest parking to destination
                        route = findAndReserveParking(route);
                        //beachte, route kann null sein, wenn zug bereits aufm parkplatz
                        if (route != null) {
                            trainService.waitingforReservedRoute(route, currentLocation, id);
                            connectionLocks += route.size();
                            locationLocks += route.size() + 1;
                            //assert(test == route.size() *2 + 1);
                            drive(route);
                         //   print("zum parkingh");
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
            error = true;
        }
    }

    /**
     *
     * @return
     */
    public boolean isError() {
        return error;
    }

    //we leverage, that the route is sorted such that the first element is the first connection
    //from origin that has to be taken, and last element is the connection to the destination

    //fahrt durch

    /**
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

    //finds the next parking, and reserves it, if it's no train station
    //retourns the route without all connections from parking to destination
    //ret null, if the train is already on the next parking

    /**
     *
     * @param route
     * @return
     */
  /*  private List <Connection> findAndReserveParking(List <Connection> route)  {
        Location dest = this.trainSchedule.destination();
        Location canPark;
        //betrachte den fall, dass keine freie parkmöglichkeit auf strecke

       // print("" + connections.size());
        int i;
        for(i = route.size()-1; i >= 0; i--) {

            if(route.get(i).second() == dest) {
                canPark = route.get(i).first();
            }
            else if(route.get(i).first() == dest) {
                canPark = route.get(i).second();
            }
            else {
                print("in findAndReserveParking, the connections do not drive to destination");
                throw new IllegalStateException();
            }
            if(canPark.reserveParking()) {
                //one parking was reserved or canPark is a train station
                route.remove(i);
                return route;
            }
            else {
                dest = canPark;
                route.remove(i);
            }
        }
        return  null;
    }
*/


    /**
     *
     * @param route
     * @return
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
