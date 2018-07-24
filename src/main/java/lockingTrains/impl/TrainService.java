package lockingTrains.impl;
import lockingTrains.validation.*;
import lockingTrains.shared.*;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class TrainService {

    public Map map;

    public TrainService(Map map){
        this.map = map;
    }

    //find and reserve closest parking
    public Location findReserveParking(int id, List<Connection> route, Location current, Location destination){     //TODO well I need to check for parkings on the shortest route
        System.out.println(id + " wants to park on " + route.toString() + " starting in " + current);
        Location marker = destination;
        //Collections.sort(route); //TODO fighting over parking could also result in the ABC BCD over CB problem
        for (int i = route.size()-1; i >= 0; i--){ //-1 to ignore last connection?
            Connection c = route.get(i); //get last connection
            if (c.first() == marker){
                if (c.second().isParkable() && c.second() != current && c.second().hasParking()){ //we found parking!
                        return c.second();
                }
                else{ //not parkable!
                    marker = c.second();
                }
            }
            else{ /// (c.first() != destination){
                if (c.first().isParkable() && c.first() != current && c.first().hasParking()){ //we found parking!
                    return c.first();
                }
                else{
                    marker = c.first();
                }

            }

        }
        return destination; //if no parking found on route; return the final destiantion
    }


    //route that we get form map is not ordered to match origin starting point; we have to sort it before we travel this route
    public List<Connection> sortroute(List<Connection> input, Location current){
        //System.out.println("ts sortroute given route " + route.toString() + " with start at " + current.toString());
        List<Connection> result = new LinkedList<Connection>();
        List<Connection> route = new LinkedList<Connection>();
        for (Connection c: input){
            route.add(c);
        }
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
        //System.out.println("ts sortroute output route " + result.toString() + " with start at " + current.toString());
        return result;
    }


    //reserve route; null if possible, else return Connections to avoid
    //todo what if its a Location that is the problem? what do I return to Train then? (not the connection to avoid; but the location?)
    public Connection reserve(List<Connection> route, Location current, int id){  //todo ROMAN: do we want synchronized or parallel on this method?
        //Connection conMark = route.get(0);
        Collections.sort(route); //sorted list by id

        Connection problem = null;
        List<Position> thusFarConn = new LinkedList<Position>();
        //try to lock route over Connections
        //TODO have to reserve based on list to avoid the ABC BCE fighting over BC problem
        for(Connection c : route){ //could have done it over Position if we remade the list to include Loc and Conn -but heck it
            if(c.getLock().tryLock()){ //can't lock; revert-unlock what we lock thus far
                System.out.println(id + " has locked " + c.toString());
                thusFarConn.add(c);
            }
            else{
                System.out.println(id + " cant lock " + c.toString() + " because " + c.getLock().toString());
                System.out.println(id + " reverting the lock on " + thusFarConn.toString());//)
                 revertLocks(thusFarConn, id);
                 problem = c;
                 return problem;
            }
            //else we got that lock!

            //go next element
        }

        //whole route Connections are LOCKED

        List<Location> locFromConn = routeToLocation(id, route, current);
        Collections.sort(locFromConn);//sorted list by id
        List<Position> thusFarLoc = new LinkedList<Position>();;
        Location marker = current;
        //whole Conn route is booked; now to Location
        for(Location c : locFromConn){ //could have done it over Position if we remade the list to include Loc and Conn -but hackit
            if(c.getLock().tryLock()) { //can't lock; revert-unlock what we lock thus far
                System.out.println(id + " has locked " + c.toString()); //"did I really lock it? connection " + c.toString() + " is " + c.getLock());
                thusFarLoc.add(c);
                marker = c;
            }
            else{
                problem = loc2Problem(c, sortroute(route, current), marker); //TODO PLACE HOLDER!
                System.out.println(id + " cant lock " + c.toString() + " because " + c.getLock().toString());
                System.out.println(id + " reverting the lock on " + thusFarLoc.toString() + " and " + thusFarConn.toString());//)
                System.out.println(id + " becasue coming from " + marker.toString() + "and cant lock " + c.toString() + " resulting in: " + problem.toString());
                //revert those locked locations
                revertLocks(thusFarLoc, id);
                //AND THE CONNECTIONS THAT WE LOCKED!
                revertLocks(thusFarConn, id);
                //problem = null; //todo ROMAN: I can't deduce a connection based only on Locked Locations; the llist is not sorted by route; WHEN pascal gets the search to work i'll give back a Location in my problem - until then just break with a null in problem

                return problem;
                //todo what if its a Location that is the problem? what do I return to Train then? (not the connection to avoid; but the location?)
                //TODO I CANT RETURN NULL HERE! FK
            }
            //else we got that lock!
            //c.getLock().lock();

        }
        //Reservation taken! knock yourself out Train!
        System.out.println(id + " reserved the route " + route.toString());
        return null;
    }

    //get Position list and unlock all of them
    public void revertLocks(List<Position> posList, int id) {
        for (Position p : posList) {
            System.out.println(id + " unlocking " + p.toString());
            p.getLock().unlock();
        }
    }

    //beacsue Pascal is still wokring on the script to return a new route based on LOCATION (adn not only connection) we have to compute a "problematic" connection based on a problematic location
    public Connection loc2Problem(Location l, List<Connection> route, Location prev){
        for(Connection c : route){
            if (c.first() == l && c.second() == prev){
                return c;
            }
            else if(c.second() == l && c.first() == prev){
                    return c;
            }
            else{
                //do nothing...
            }
        }
        System.out.println("I cant find the problematic connection to avoid! return first ");
        return route.get(route.size() - 1); //if he doesnt find any.. whihc is unplausible
    }


    //gives back a location list based ona connection list; for locking
    public List<Location> routeToLocation(int id, List<Connection> input, Location current) {
        List<Location> result = new LinkedList<Location>();
        System.out.println(id + " on routeToLocation given route: " + input.toString());


        result.add(current);
        List<Connection> dummy = new LinkedList<Connection>();
        for (Connection c : input){
            dummy.add(c);
        }

        Location marker = current;
        while (!dummy.isEmpty()){
            for(Connection c: dummy){
                if (c.first() == marker){
                    result.add(c.second());
                    dummy.remove(c);
                    marker = c.second();
                    break;
                }
                if (c.second() == marker){
                    result.add(c.first());
                    dummy.remove(c);
                    marker = c.first();
                    break;
                }
            }
        }


//        result.add(current);
//        for (Connection c : input) {
//            if(current == c.first()){
//                result.add(c.second());
//                current = c.second();
//            }
//            else{ //(current != c.first()){
//                result.add(c.first());
//                current = c.first();
//            }
//        }


        System.out.println(id + " gets routeToLocation result: " + result.toString());
        return result;
    }

    protected final Lock waitLock = new ReentrantLock();
    private final Condition unlocked = waitLock.newCondition();

    //TRYING TO KEEP TRAKC OF TRAINS DONE
    int trainsDone = 0;
    //keep track of how many trains finished
    public void finished(TrainSchedule trainSchedule){
        //System.out.println("Final Location: " + trainSchedule.destination().toString() + " is locked? : " + trainSchedule.destination().getLock());
        //trainSchedule.destination().getLock().unlock(); //done now in drive()
        trainsDone++;
    }

    public int getDone(){
        return trainsDone;
    }

    public void leave(final TrainSchedule schedule, final Location location){
        System.out.println(schedule.id() + " is leaving Location: " + location.toString() + " is locked? : " + location.getLock());
        location.getLock().unlock();
        sigMethod();
    }

    public void travel(final TrainSchedule schedule, final Connection section){
        System.out.println(schedule.id() + " travels over Connection: " + section.toString() + " is locked? : " + section.getLock());
        section.getLock().unlock();
        sigMethod();
    }


    public void waitForReserve(List<Connection> route, Location current, int id) throws InterruptedException {
        waitLock.lock();
        try{
            while(reserve(route, current, id) != null){
                unlocked.await();
            }
        }
        catch(InterruptedException e){
            waitLock.unlock();
            //id.isError();// = true;
        }
        waitLock.unlock();

    }

    public void sigMethod() {
        waitLock.lock();
        try {
            unlocked.signalAll();
        } catch (Exception e) {
            waitLock.unlock();
        }
        waitLock.unlock();
//        waitLock.lock();
//        unlocked.signalAll();
//        waitLock.unlock();
    }
}
