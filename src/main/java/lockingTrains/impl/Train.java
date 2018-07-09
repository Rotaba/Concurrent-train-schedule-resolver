package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Train extends Thread {
    private final TrainSchedule trainSchedule;
    private final Recorder recorder;
    private final Map map;
    private final TrainService trainService;
    private Location currentLocation;

    //an empty List of Connections to call map.route with an empty list to avoid
    private List<Connection> empty = new ArrayList<>();


    public Train(TrainSchedule trainSchedule, Recorder recorder, Map map, TrainService trainService) {
        this.trainSchedule = trainSchedule;
        this.recorder = recorder;
        this.map = map;
        this.trainService = trainService;
        this.currentLocation = trainSchedule.origin();


    }

    //todo: teste, was passiert, wenn ein zug auf einem parkplatz warten muss, und bereits aufm parkplatz steht
    public void run() {
         List <Connection> route ;
        recorder.start(trainSchedule);
        while(true) {
            route = map.route(currentLocation, trainSchedule.destination(), empty);
            assert (route != null);
            if (trainService.reserveConnections(route)) {
                //route was reserved
                print("drive wird als erstes aufgerufen von " + Thread.currentThread().getId());
                drive(route);
            } else {
                //could not reserve whole route
                List<Connection> alreadyTaken = new ArrayList<>();
                for (Connection c : route) {
                    if (!trainService.reserveConnection(c)) {
                        //search for all connections that are already reserved
                        alreadyTaken.add(c);
                    }
                    else {trainService.freeConnection(c);}
                }
                //update route
                route = map.route(currentLocation, trainSchedule.destination(), alreadyTaken);
                if (!(route == null)) {
                    //we found an alternative route
                    if(trainService.reserveConnections(route)){
                        print("drive wird als zweites aufgerufen von " + Thread.currentThread().getId());
                        drive(route);
                        System.out.println("wow, this basic block is really taken o.O");
                    }
                } else {
                    //there is no route without reserved parts
                    route = map.route(currentLocation, trainSchedule.destination(), empty);
                    //find nearest parking to destination
                    route = findAndReserveParking(route);
                    //beachte, route kann null sein, wenn zug bereits aufm parkplatz
                    if(route != null)  {
                        try {
                            trainService.waitingforReservedConnections(route);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            print("something went wrong :D");
                            throw new IllegalStateException();
                        }

                        print("drive wird als drittes aufgerufen von " + Thread.currentThread().getId());
                        drive(route);
                    }
                }
            }
            if (currentLocation.equals(trainSchedule.destination())) {
                recorder.finish(trainSchedule);
                print("finish event ");
                return;
            }
        }


    }

    //we leverage, that the route is sorted such that the first element is the first connection
    //from origin that has to be taken, and last element is the connection to the destination

    //fahrt durch
    private void drive(List <Connection> connections) {
        int i=0;
        assert (connections != null);
        Connection c;
        while(!connections.isEmpty()) {
            print("hier" + i);
            i++;
            c = connections.remove(0);
            recorder.leave(trainSchedule, currentLocation);
            recorder.travel(trainSchedule, c);
            try {
                c.travel();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("EEEEEEEERRROORR could not travel, why?");
                throw new IllegalStateException();
            }
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
            trainService.freeConnection(c);
        }



    }

    //finds the next parking, and reserves it, if it's no train station
    //retourns the route without all connections from parking to destination
    //ret null, if the train is already on the next parking
    private List <Connection> findAndReserveParking(List <Connection> connections)  {
        Location dest = this.trainSchedule.destination();
        Location canPark;
        //betrachte den fall, dass keine freie parkmöglichkeit auf strecke
        for(int i = connections.size(); i == 0; i --) {
            //assert (connections.get(i).second() == dest || connections.get(i).first() == dest);
            if(connections.get(i).second() == dest) {
                canPark = connections.get(i).first();
            }
            else if(connections.get(i).first() == dest) {
                canPark = connections.get(i).second();
            }
            else {
                 print("in findAndReserveParking, the connections do not drive to destination");
                 throw new IllegalStateException();
            }
            if(canPark.reserveParking()) {
                //one parking was reserved or canPark is a train station
                connections.remove(i);
                return connections;
            }
            else {
                dest = canPark;
                connections.remove(i);
            }
        }
        return  null;
    }

    private void print(String str) {
        System.out.println(str);
    }

}
