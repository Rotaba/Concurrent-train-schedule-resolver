package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.validation.Recorder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
    private Lock lock = new ReentrantLock();
    private Condition waitingRouteFree = lock.newCondition();


    public TrainService(){
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
    synchronized boolean reserveConnections(List <Connection> connections, Location currentLocation, int id){
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

    /**
     *
     * @param route
     * @param id
     * @return
     */
     Collection<Connection> getAlreadyTakenConnections(List<Connection> route, int id) {
        Collection<Connection> alreadyTaken = new LinkedList<>() ;
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

    /**
     *
     * @param connection
     * @param id
     */
    synchronized void freeConnection(Connection connection, int id) {
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
    synchronized void freeLocation(Location location, int id) {
        location.getLock().unlock();
        lock.lock();
        waitingRouteFree.signalAll();
        lock.unlock();
      ;
    }


    //punkt (i)
    //jetzt müssen die anderen methoden synchronizes sein, sonst kann es vorkommen dass grade einer ins wait set kommt,
    //in dem moment wo der letzte andere signalAll() aufruft, und dann ist er am A...
    void waitingforReservedConnections(List <Connection> connections, Location currentLocation, int id)
            throws InterruptedException {
        while(true) {
            if(reserveConnections(connections, currentLocation, id)) {
                return ;
            }
            else {
                lock.lock();
                waitingRouteFree.await();
                lock.unlock();
            }
        }
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



    private void print (String str) {
        System.out.println(str);
    }

}
