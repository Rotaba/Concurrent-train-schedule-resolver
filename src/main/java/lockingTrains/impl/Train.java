package lockingTrains.impl;
import lockingTrains.validation.*;
import lockingTrains.shared.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Train extends Thread{ //to add run() method

    public TrainSchedule trainSchedule;
    public Location current;
    public Location destination;
    public int id;
    public boolean parked;
    public Recorder rc;
    static public TrainService ts;
    public Map map;
    private boolean isError = false;
    public boolean afterPause = false;


    //Constructor init at origin Trainstation
    public Train(TrainSchedule trainSchedule, TrainService ts, Recorder rc, Map map){
        this.trainSchedule = trainSchedule;
        this.id = trainSchedule.id();
        this.parked = true;
        this.current = trainSchedule.origin();
        this.destination = trainSchedule.destination();
        this.ts = ts;
        this.rc = rc;
        this.map = map;

    }


    public void run(){
        //init state
        try {
            List<Connection> route = new LinkedList<Connection>();
            List<Position> avoid = new LinkedList<Position>(); //todo what if its a Location that is the problem? what do I return to Train then? (not the connection to avoid; but the location?)
            boolean afterPark = false;

            //while LOOP break if reached destination
            while (true) { //route.isEmpty(); not reached dst == route is not empty
                System.out.println(" ");

                //(b)
                //consider map for route
                route = map.route(current, destination, avoid);

                //System.out.println(id + " starts in " + current + " gets route " + route);
                if (route != null) { //a path exists but we
                    //ask TS to reserve
                    System.out.println(id + " wants to reserve " +route.toString());
                    Connection result = ts.reserve(route, current, id);
                    if (result == null) { // meaning its reserved
                        //sort it to be right direction
                        System.out.println(id + " wants to drive " +route.toString());
                        route = sortroute(route, current);
                        //System.out.println("route drive");
                        drive(route);
                    } else { //there are results to avoid; must compute new route
                        //else goto (b)
                        avoid.add(result);
                        //finish if and run while again with new avoid
                    }
                } else {//(route == null) can't find a route; have to go find parking
                    //System.out.print("ERROR NO ROUTE POSSIBLE! Have to look for parking");
                    System.out.println(id + " Train.run() ELSE; gonna find me some parking!");
                    avoid = new LinkedList<Position>(); //ignore all problem thus far
                    route = map.route(current, destination, avoid); //again get direct path
                    route = sortroute(route, current);
                    //find closest parking to dst;
                    //find and reserve closes free parking to dst; get that parking back
                    Location parkHere = ts.findReserveParking(id, route, current, destination);
                    System.out.println(id + " found a parking spot; " + parkHere.toString() + " on route " + route.toString());

                    //get route to parking place
                    route = map.route(current, parkHere, avoid);
                    route = sortroute(route, current);

//                    Connection reserved = ts.reserve(route, current, id);
//                    while (reserved != null) {
//                        //lock.lock();
//                        try {
//                            //System.out.println(id + " goes to sleep; wants to park on " + parkHere.toString() + " on route " + route.toString());
//                            Thread.sleep(5); //TODO this has to be a condition waiting for a unlock - then repat reserver of this route
//                            reserved = ts.reserve(route, current, id);
//                        } catch (InterruptedException e) {
//                            isError = true;
//                            //lock.unlock();
//                        }
//                    }
                    //ask ts to reserve
                    ts.waitForReserve(route, current, id);
                    //route = sortroute(route, current);
                    System.out.println(id + " PARKING drive");
                    driveToPark(route); //drive to parking
                    afterPark = true;

                }
                if (current.equals(destination)) {
                    System.out.println(id +" map.rout gave an empty route; we're at the destination!");
                    finish();
                    break;
                }
                //repeat while
            }
        }catch(Exception e){
            e.printStackTrace();
            isError = true;
        }
        rc.finish(trainSchedule);

    }


    //TODO ROMAN does the map.route gives a sorted by driving direction list?
    //route that we get form map is not ordered to match origin starting point; we have to sort it before we travel this route
    public List<Connection> sortroute(List<Connection> route, Location current){
        System.out.println(id + " calls sortroute given route " + route.toString() + " with start at " + current.toString());
        List<Connection> result = new LinkedList<Connection>();
        Location marker = current;
        while(!route.isEmpty()){
            for (Connection c : route){
                if (c.first() == marker){
                    result.add(c);
                    marker  = c.second();
                    route.remove(c);
                    break;
                }
                if(c.second() == marker) {
                    result.add(c);
                    marker  = c.first();
                    route.remove(c);
                    break;
                }
            }
        }
        System.out.println(id + " sortroute output route " + result.toString() + " with start at " + current.toString());
        return result;
    }

    public void drive(List<Connection> route){
        System.out.println(id + " drives over route " + route.toString() + " and starting in " + current.toString());
        if(parked){
            current.leaveParking();
            parked = false;
        }

        if(afterPause){
            rc.resume(trainSchedule, current); //notify recorder that we resume after parking
            afterPause = false;
        }
        for (Connection c : route){ //ON CONN A-B
        //for (int i = 0; i < route.size(); i ++){
            //Connection c = route.get(i);
            //System.out.println("processing Connection: " + c.toString());
            if (current == c.first()){ //IF IN A; DRIVE TO B
                leave(c.first());
                //current = null;
                travel(c);
                arrive(c.second()); //ARRIVE AT B
                current = c.second();

            }
            else{ //IN B DRIVE TO A
                leave(c.second());
                //current = null;
                travel(c);
                arrive(c.first()); //ARRIVE AT A
                current = c.first();
            }

        }
//        leave(current); //unlock last Location
        ts.leave(trainSchedule, current);

    }

    public void driveToPark(List<Connection> route){
        System.out.println(id + " heading to park over route " + route.toString() + " and starting in " + current.toString());
        if(parked){
            current.leaveParking();
            parked = false;
        }
        if(afterPause){
            if(!current.isStation()) {
                rc.resume(trainSchedule, current); //notify recorder that we resume after parking
            }
            afterPause = false;
        }
        route = sortroute(route,current);
        for (Connection c : route){ //ON CONN A-B
        //for (int i = 0; i < route.size(); i++){
            //Connection c = route.get(i);
            //System.out.println("processing Connection: " + c.toString());
            if (current == c.first()){ //IF IN A; DRIVE TO B
                leave(c.first());
                //current = null;
                travel(c);
                arrive(c.second()); //ARRIVE AT B
                current = c.second();
            }
            else{ //(current == c.second()){
                leave(c.second());
                //current = null;
                travel(c);
                arrive(c.first()); //ARRIVE AT A
                current = c.first();
            }
        }
       //leave(current); //leave last Location we arrived to
        ts.leave(trainSchedule, current);

        if(!current.isStation()){
            rc.pause(trainSchedule, current); //notify recorder that we pause here to park
            afterPause = true;
        }
        parked = true;
    }

    /**
    * Call this method for each train that travels a connection
    * <strong>before</strong> the train enters {@link Connection#travel()}.
    //a method that locks/unlocks Positions and tells Recorder whats going on while driving **/
    public synchronized void travel(Connection section){
        System.out.println(id + " is travelling over " + section.toString());
        rc.travel(trainSchedule, section);
        try {
            section.travel();
            //Thread.sleep(section.time()); //TODO ROMAN: so where was it said that we have to sleep this amount of time?
        }catch(InterruptedException e){
            System.out.println("isError: while trying to sleep in train.travel");
            isError = true;
        }
        ts.travel(trainSchedule, section);
    }

    //a method that locks/unlocks Positions and tells Recorder whats going on while driving
    public  void leave(Location location){
        ts.leave(trainSchedule, location);
        System.out.println(id + " LEAVE " + location.toString());
        rc.leave(trainSchedule, location); //tell REC
        //location.getLock().unlock();
        //current = null; //IN TRANSIT
    }

    public void arrive(Location location){
        //current = location;
        rc.arrive(trainSchedule, location); //tell REC
        //                   location.getLock().unlock(); //just got there! not over with yet!
    }

    /**
     * Call this method for each train that pauses at a location that is not a
     * station. A train pauses at a location if the location is the destination of
     * the route but not the destination of the train schedule.
     *
     *
     * @param location to pause at.
     */
    public void pause(Location location){
        rc.pause(trainSchedule, location); //tell REC
        //location.getLock().unlock(); //just got there! not over with yet!
    }

    public void resume(Location location){
        rc.resume(trainSchedule, location); //tell REC
        //location.getLock().unlock(); //just got there! not over with yet!
    }

    /**
     * Call this method for each train that arrives at its scheduled destination.
     * This does not replace the call to {@link #arrive arrive} and must be called
     * afterwards.
     *
     * @param
     */
    public void finish(){
        //leave(current);
        System.out.println("Train " + id + " FINISHED");
        ts.finished(trainSchedule);
//        rc.finish(trainSchedule); //tell REC
        //ts.finTrack(trainSchedule); //tell TS
        //location.getLock().unlock(); //just got there! not over with yet!
    }

    public boolean isError(){
//        System.out.println("Train isError: " + s);
        return isError;
    }

    /**
     * Call this method for each train that pauses at a location that is not a
     * station. A train pauses at a location if the location is the destination of
     * the route but not the destination of the train schedule.
     *
     * @param schedule the action is recorded for.
     * @param location to pause at.
     */
    public void pause(TrainSchedule schedule, final Location location){
        rc.pause(schedule, current);
    }

    /**
     * Call this method for each train that resumes its schedule after it paused at
     * a non-station location.
     *
     * @param schedule the action is recorded for.
     * @param location to resume from.
//     */
//    public  void resume(final TrainSchedule schedule, final Location location){
//
//    }

}
