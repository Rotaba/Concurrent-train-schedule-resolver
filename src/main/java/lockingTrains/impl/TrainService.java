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
    boolean reserveConnections(List <Connection> connections, Location currentLocation, int id){
        //  print ("first");
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
                //   print("thread " + id + " holds connection lock " + c.toString());
                alreadyReservedConnection.add(c);
            }else{
                for(Connection con : alreadyReservedConnection) {
                    //       print("thread " + id + " freees connection lock " + con.toString());
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

    //  synchronized private void
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


    synchronized void freeConnection(Connection connection, int id) {
        connection.getLock().unlock();
        //we need notifyAll here, because we do not know which connection will be freed, and
        //which other train does need this freed connection.
        notifyAll();
    }
    void freeLocation(Location location, int id) {
        // print("fourth");
        //  print("thread " + id + " frees location lock " + location.toString());
        location.getLock().unlock();
    }


    //punkt (i)
    boolean waitingforReservedConnections(List <Connection> connections, Location currentLocation, int id)
            throws InterruptedException {
        List <Connection> alreadyReservedConnection = new LinkedList<>();
        List<Location> alreadyReservedLocation = new LinkedList<>();
        Location location ;
        System.out.println("this method is really entered once o.O !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        //try to get all locks of the route

        boolean reservedAll;
        while(true) {
            location = currentLocation;
            if(location.getLock().tryLock()) {
                alreadyReservedLocation.add(location);
                reservedAll = true;
                for (Connection c : connections) {
                    if(c.first().equals(location)) {
                        location = c.second();
                    }
                    else {
                        location = c.first();
                    }
                    //if couldn't get one lock, free all previous hold locks
                    if (!c.getLock().tryLock() || !location.getLock().tryLock()) {
                        for (Connection con : alreadyReservedConnection) {
                            con.getLock().unlock();
                            alreadyReservedConnection.remove(con);
                        }
                        for (Location l : alreadyReservedLocation) {
                            l.getLock().unlock();
                            alreadyReservedLocation.remove(l);
                        }
                        reservedAll = false;
                        break;

                    } else {
                        alreadyReservedConnection.add(c);
                        alreadyReservedLocation.add(location);

                    }
                }
                if (reservedAll) return reservedAll;
            }
            wait();
        }
    }
    private void print (String str) {
        System.out.println(str);
    }

}
