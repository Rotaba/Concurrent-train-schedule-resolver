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

    /**
     * Can be run by multiple Threads! i.e. Trains may ask to reserve routes while others are tyring aswell
     * On secessful reserve will lokc all connections and locations on the route
     * @param connections of the asked route
     * @param currentLocation of the asking Train
     * @param id of the askin train (debugging info)
     * @return True; reserved and locked, false; couldn't lock one of the Conn/Loc on the route
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
     * Lets a train, after being denied a reservation on route, to inquire which Positions are locked
     * to be used in the avoid on scucsessive map.route() calls
     * @param route The asked route; which was not reserved becasue of an already locked Position
     * @param id of calling train (debugging info)
     * @return List of Positions which couldn't be locked; to be used in the avoid b Train to recalculate the route again
     *
     */

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


    /**
     * Converts a connection route intro a Location list
     * @param route the inquired connection route
     * @param currentLocation of asking train
     * @return a List of locations corresponding to the given route
     */
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
     * Unlock a connection and singal to sleeping thread
     * @param connection on which we call the unlock
     * @param id of calling train (debugging info)
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
     * Unlock a Location and singal to sleeping thread
     * @param location on which we call the unlock
     * @param id of calling train (debugging info)
     */
    void freeLocation(Location location, int id) {
        location.getLock().unlock();
        lock.lock();
        waitingRouteFree.signalAll();
        lock.unlock();
      ;
    }

    /**
     * When a train is unable to reserve a route to a parking place; it would use the waitingRouteFree condition to wait
     * wait signal comes from any Conn/Loc unlock
     * @param connections that the train wants to reserve
     * @param currentLocation of calling train
     * @param id of calling train (debugging info)
     * @throws InterruptedException caused by the await
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



    //DEBUG
    synchronized void print (String str) {
        System.out.println(str);
    }

}
