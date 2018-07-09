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
    private boolean geparkt = false;
    private boolean error = false;

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
        try{

            recorder.start(trainSchedule);
        }catch (Exception e){ error = true; return;}
        while(true) {
            route = map.route(currentLocation, trainSchedule.destination(), empty);
            assert (route != null);
            if (trainService.reserveConnections(route)) {
                //route was reserved
                if(everyInvocationOfRecorderNeedsTryCatch(route)) return;/*
                if(geparkt) {
                    geparkt = false;
                    recorder.resume(trainSchedule, currentLocation);
                    currentLocation.freeParking();
                    print("freed parking 1");
                }
                try {
                    drive(route);
                } catch (Exception e) {error = true; return;}*/
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
                        if(everyInvocationOfRecorderNeedsTryCatch(route)) return;/*
                        if(geparkt) {
                            geparkt = false;
                            recorder.resume(trainSchedule, currentLocation);
                            currentLocation.freeParking();
                            print("freed parking 2");
                        }
                        try {
                            drive(route);
                        } catch (Exception e) {error = true; return;}*/
                    }
                } else {
                    //there is no route without reserved parts
                    route = map.route(currentLocation, trainSchedule.destination(), empty);
                    //find nearest parking to destination
                    try {
                        route = findAndReserveParking(route);
                    } catch (Exception e) {error = true; return; }
                    //beachte, route kann null sein, wenn zug bereits aufm parkplatz, oder kein Parkplatz findet
                    if(route != null)  {

                        try {
                            trainService.waitingforReservedConnections(route);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            print("something went wrong :D");
                            throw new IllegalStateException();
                        }
                        if(everyInvocationOfRecorderNeedsTryCatch(route)) return;
                       /* try {
                            if (geparkt) {
                                geparkt = false;
                                recorder.resume(trainSchedule, currentLocation);
                                currentLocation.freeParking();
                                print("freed parking 3");
                            }
                            drive(route);
                            geparkt = true;
                        } catch (Exception e) {error = true; return;}*/
                    }
                }
            }
            if (currentLocation.equals(trainSchedule.destination())) {
                try {
                    recorder.finish(trainSchedule);
                }catch (Exception e) {
                    error = true;
                    return;

                }
                return;
            }
        }
    }

    private boolean everyInvocationOfRecorderNeedsTryCatch(List <Connection> route)  {
        try {
            if (geparkt) {
                geparkt = false;
                recorder.resume(trainSchedule, currentLocation);
                currentLocation.freeParking();
                print("freed parking ");
            }
            drive(route);
        } catch (Exception e) {
            error = true;
            return true;
        }
        return false;



    }

    //we leverage, that the route is sorted such that the first element is the first connection
    //from origin that has to be taken, and last element is the connection to the destination
    //currentLocation is never a Verbindungspunkt
    //fahrt durch

    /**
     *
     * @param connections
     * @throws Exception
     */
    private void drive(List <Connection> connections) throws Exception {
        assert (connections != null);
        Connection c;
        while(!connections.isEmpty()) {
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
            //we have to free every used connection
            trainService.freeConnection(c);
        }



    }

    //finds the next parking, and reserves it, if it's no train station
    //retourns the route without all connections from parking to destination
    //ret null, if the train is already on the next parking
    private List <Connection> findAndReserveParking(List <Connection> connections) throws Exception{
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
                print("reserved parking");
                //one parking was reserved or canPark is a train station
                if(!canPark.isStation()) recorder.pause(trainSchedule, canPark);
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

    public boolean getError() {
        return error;
    }
}
