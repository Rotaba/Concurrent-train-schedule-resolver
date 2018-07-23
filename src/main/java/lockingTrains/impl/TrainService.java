package lockingTrains.impl;

import lockingTrains.shared.*;
import lockingTrains.shared.Map;
import lockingTrains.validation.Recorder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The purpose of this class is to lock the route, when a train is asking for it.
 * And to unlock connections/locations
 */
public class TrainService {

    private TrainSchedule trainSchedule;
    private Recorder recorder;
    //map nicht synchronized
    private Map map;
    private Location currentLocation;
    private Connection currentConnection;
    private LinkedList<Connection> route;
    private List<Connection> allConnections;
    private List<Location> allLocations;
    private int firstConnectionId;
    private int firstLocationId;
    private Lock lock = new ReentrantLock();
    private Condition waitingRouteFree = lock.newCondition();
    private static int counter = 0;
    private int sleeping;
    private int finished;


    public TrainService(Map map){
        this.allConnections = map.connections();
        this.allLocations = map.locations();
        this.firstConnectionId = allConnections.get(0).id();
        this.firstLocationId = allLocations.get(0).id();

        this.sleeping = 0;
        this.finished = 0;
    }

    synchronized void setFinished() {
        finished ++;
    }



    //TODO check ob du die anderen methoden dieser klasse aufrufen kannst
    //gibt true zurück, wenn sich alle connecctions reservieren lassenroute
    //denk dran, bei false auch alle streckenabschnitte wieder freizugeben


    /**
     *
     * @param connections
     * @param currentLocation
     * @param id
     * @return
     */
    boolean reserveRoute(List <Connection> connections, Location currentLocation, int id){
        List <Connection> alreadyReservedConnection = new LinkedList<>();
        List <Location> alreadyReservedLocation = new LinkedList<>();
        List <Location> locationsToReserve = locationsOnRoute(connections, currentLocation);

        int[] connectionsIds = new int[connections.size()];
        int[] locationIds = new int[locationsToReserve.size()];
        int i = 0;
        for(Connection c : connections) {
            connectionsIds[i] = c.id();
            i++;
        }

        i = 0;
        for(Location l : locationsToReserve) {
            locationIds[i] = l.id();
            i++;
        }
        Arrays.sort(connectionsIds);
        Arrays.sort(locationIds);

        for(i = 0; i < connectionsIds.length; i++) {
            if(allConnections.get(connectionsIds[i]-firstConnectionId).getLock().tryLock()){
                alreadyReservedConnection.add(allConnections.get(connectionsIds[i]-firstConnectionId));
            }
            else {
                for(Connection c : alreadyReservedConnection) {
                    c.getLock().unlock();
                }
                return false;
            }
        }
        for(i = 0; i < locationIds.length; i++) {
            if(allLocations.get(locationIds[i]-firstLocationId).getLock().tryLock()){
                alreadyReservedLocation.add(allLocations.get(locationIds[i]-firstLocationId));
            }
            else {
                for(Connection c : alreadyReservedConnection) {
                    c.getLock().unlock();
                }
                for(Location l : alreadyReservedLocation) {
                    l.getLock().unlock();
                }
                return false;
            }
        }
        return true;
    }


    /**
     *
     * @param connections
     * @param currentLocation
     * @param id
     * @return
     */
   /* boolean reserveConnections(List <Connection> connections, Location currentLocation, int id){
        Location location = currentLocation;
        List <Connection> alreadyReservedConnection = new LinkedList<>();
        List <Location> alreadyReservedLocation = new LinkedList<>();
        List <Location> locationsToReserve = new LinkedList<>();
        locationsToReserve.add(location);
        //try to get all locks of the route
        for (Connection c : connections) {
            if(c.first().equals(location)) {
                location = c.second();
            }
            else if (c.second().equals(location)) {
                location = c.first();
            }
            else print ("something went wront");
            locationsToReserve.add(location);
            //if couldn't get one lock, free all previous hold locks
            if(c.getLock().tryLock()) {
                //remember all locks you can hold
                alreadyReservedConnection.add(c);
            }else{
                for(Connection con : alreadyReservedConnection) {
                    con.getLock().unlock();
                }
                return false;
            }
        }
        for(Location l : locationsToReserve) {
            if(!l.getLock().tryLock()){
                for(Location loc : alreadyReservedLocation) {
                    loc.getLock().unlock();
                }
                for(Connection c : alreadyReservedConnection) {
                    c.getLock().unlock();
                }
                return false;
            }
            else {
                alreadyReservedLocation.add(l);
            }
        }
        return true;
    }

*/
    /**
     *
     * @param route
     * @param id
     * @return
     */
     Collection<Position> getAlreadyTakenConnections(List<Connection> route, int id) {
        Collection<Position> alreadyTaken = new LinkedList<>() ;
        for(Connection c : route) {
            if(c.getLock().tryLock()) {
                c.getLock().unlock();
            }
            else {
                alreadyTaken.add(c);
            }
        }
        return alreadyTaken;
    }



    Collection<Position> getAlreadyTakenPosition(List<Connection> route, Location currentLocation, int id) {
        List <Position> avoid = new LinkedList<>();
        List <Location> locationsToReserve = locationsOnRoute(route, currentLocation);

        int[] connectionsIds = new int[route.size()];
        int[] locationIds = new int[locationsToReserve.size()];

        int i = 0;
        for(Connection c : route) {
            connectionsIds[i] = c.id();
            i++;
        }

        i = 0;
        for(Location l : locationsToReserve) {
            locationIds[i] = l.id();
            i++;
        }
        Arrays.sort(connectionsIds);
        Arrays.sort(locationIds);

        //first all connections
        for(i = 0; i < connectionsIds.length; i++) {
            if(allConnections.get(connectionsIds[i]-firstConnectionId).getLock().tryLock()){
                allConnections.get(connectionsIds[i]-firstConnectionId).getLock().unlock();
            }
            else {
                avoid.add(allConnections.get(connectionsIds[i]-firstConnectionId));
            }
        }

        //then all locations
        for(i = 0; i < locationIds.length; i++) {
            if(allLocations.get(locationIds[i]-firstLocationId).getLock().tryLock()){
                allLocations.get(locationIds[i]-firstLocationId).getLock().unlock();
            }
            else {
                avoid.add(allLocations.get(locationIds[i]-firstLocationId));
            }
        }
        return avoid;
    }

    private List<Location> locationsOnRoute(List<Connection> route, Location currentLocation) {
        List <Location> locationsToReserve = new LinkedList<>();
        Location location = currentLocation;
        locationsToReserve.add(location);
        for(Connection c : route) {
            if(c.first().equals(location)) {
                location = c.second();
            }
            else if (c.second().equals(location)) {
                location = c.first();
            }
            else print ("something went wront");
            locationsToReserve.add(location);
        }
        return locationsToReserve;
    }


    /**
     *
     * @param connection
     * @param id
     */
    void freeConnection(Connection connection, int id) {
        connection.getLock().unlock();
        //we need notifyAll here, because we do not know which connection will be freed, and
        //which other train does need this freed connection.
        lock.lock();
        waitingRouteFree.signalAll();
        lock.unlock();
    }


    /**
     * 
     * @param location
     * @param id
     */
    void freeLocation(Location location, int id) {
        location.getLock().unlock();
        lock.lock();
        waitingRouteFree.signalAll();
        lock.unlock();
      ;
    }


    //punkt (i)
    //jetzt müssen die anderen methoden synchronizes sein, sonst kann es vorkommen dass grade einer ins wait set kommt,
    //in dem moment wo der letzte andere signalAll() aufruft, und dann ist er am A...

    /**
     *
     * @param connections
     * @param currentLocation
     * @param id
     * @throws InterruptedException
     */
    void waitingforReservedRoute(List <Connection> connections, Location currentLocation, int id)
            throws InterruptedException {

        while(!reserveRoute(connections, currentLocation, id)){
            lock.lock();
            sleeping ++;
            waitingRouteFree.await(10, TimeUnit.MILLISECONDS);
            lock.unlock();

        }
        lock.lock();
        sleeping--;
        lock.unlock();

    }

        /*
        print("entered by " + id);
        boolean reservedAll = true;
        int holdLocks = 0;
        while(true) {
            Location location = currentLocation;
            List <Connection> alreadyReservedConnection = new LinkedList<>();
            List <Location> alreadyReservedLocation = new LinkedList<>();
            List <Location> locationsToReserve = new LinkedList<>();
            locationsToReserve.add(location);
            for(Connection connection:connections) {
                if(connection.first().equals(location)) {
                    location = connection.second();
                }
                else if (connection.second().equals(location)) {
                    location = connection.first();
                }
                else print ("something went wront");
                locationsToReserve.add(location);
                if(connection.getLock().tryLock()) {
                    holdLocks++;
                    //remember all locks you can hold
                    alreadyReservedConnection.add(connection);
                }else{
                    for(Connection con : alreadyReservedConnection) {
                        con.getLock().unlock();
                        holdLocks--;
                        alreadyReservedConnection.remove(con);
                    }
                    reservedAll = false;
                }
            }
            if(reservedAll) {
                for (Location l : locationsToReserve) {
                    if (!l.getLock().tryLock()) {
                        for (Location loc : alreadyReservedLocation) {
                            loc.getLock().unlock();
                            holdLocks--;
                            alreadyReservedLocation.remove(loc);
                        }
                        for (Connection c : alreadyReservedConnection) {
                            c.getLock().unlock();
                            holdLocks--;
                            alreadyReservedConnection.remove(c);
                        }
                        reservedAll = false;
                    } else {
                        alreadyReservedLocation.add(l);
                        holdLocks++;
                    }
                }
            }
            if(reservedAll){
                print("quit by " + id);
                return holdLocks;
            }
        }
    }*/



    synchronized void print (String str) {
        System.out.println(str);
    }

}
