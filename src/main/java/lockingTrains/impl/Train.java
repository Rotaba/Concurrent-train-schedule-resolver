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
import java.util.concurrent.locks.Lock;

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
    private List<Connection> empty = new ArrayList<>();


    public Train(TrainSchedule trainSchedule, Recorder recorder, Map map, TrainService trainService) {
        this.trainSchedule = trainSchedule;
        this.recorder = recorder;
        this.map = map;
        this.trainService = trainService;
        this.currentLocation = trainSchedule.origin();
        this.id = counter++;

    }

    //todo: teste, was passiert, wenn ein zug auf einem parkplatz warten muss, und bereits aufm parkplatz steht
    public void run() {
        try {
            List<Connection> route;
            recorder.start(trainSchedule);
            currentLocation.reserveParking();
            while (true) {
                route = map.route(currentLocation, trainSchedule.destination(), empty);

                if (trainService.reserveConnections(route, currentLocation, id)) {
                    connectionLocks += route.size();
                    locationLocks += route.size() + 1;
                    //route was reserved
                    drive(route);
                } else {
                    //could not reserve whole route
                    Collection<Connection> alreadyTaken;
                    alreadyTaken = trainService.getAlreadyTakenConnections(route, id);
                    //update route
                    route = map.route(currentLocation, trainSchedule.destination(), alreadyTaken);
                    if (route != null) {
                        //we found an alternative route
                        if (trainService.reserveConnections(route, currentLocation, id)) {
                            connectionLocks += route.size();
                            locationLocks += route.size() + 1;
                            drive(route);
                        }
                    } else {
                        //there is no route without reserved parts
                        route = map.route(currentLocation, trainSchedule.destination(), empty);
                        //find nearest parking to destination
                        route = findAndReserveParking(route);
                        //beachte, route kann null sein, wenn zug bereits aufm parkplatz
                        if (route != null) {
                            trainService.waitingforReservedConnections(route, currentLocation, id);
                            connectionLocks += route.size();
                            locationLocks += route.size() + 1;
                            //assert(test == route.size() *2 + 1);
                            drive(route);
                        }
                    }
                }
                if (currentLocation.equals(trainSchedule.destination())) {
                  //  print(connectionLocks + " locks " + locationLocks);
                    assert (connectionLocks == 0);
                    assert (0 == locationLocks);
                  //  print("finished event");
                    recorder.finish(trainSchedule);
                    return;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            error = true;
        }
    }

    public boolean isError() {
        return error;
    }

    //we leverage, that the route is sorted such that the first element is the first connection
    //from origin that has to be taken, and last element is the connection to the destination

    //fahrt durch
    private void drive(List <Connection> connections) throws InterruptedException {
        currentLocation.freeParking();
        if(parking) {
            recorder.resume(trainSchedule, currentLocation);
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
    private List <Connection> findAndReserveParking(List <Connection> route)  {
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

    private void print(String str) {
        System.out.println(str);
    }

}
