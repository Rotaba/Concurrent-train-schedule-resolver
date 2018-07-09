package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.validation.Recorder;

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
    synchronized boolean reserveConnections(List <Connection> connections){
        List <Connection> alreadyReserved = new LinkedList<>();
        //try to get all locks of the route
        for (Connection c : connections) {
            //if couldn't get one lock, free all previous hold locks
            if(!c.getLock().tryLock()){
                for(Connection con : alreadyReserved) {
                    con.getLock().unlock();
                }
                return false;
            }else {
                //remember all locks you can hold
                alreadyReserved.add(c);
            }
        }
        return true;
    }

    synchronized boolean reserveConnection(Connection connection) {
        return connection.getLock().tryLock();
    }
    synchronized void freeConnection(Connection connection) {
        connection.getLock().unlock();
        //we need notifyAll here, because we do not know which connection will be freed, and
        //which other train does need this freed connection.
        notifyAll();
    }


    //punkt (i)
    synchronized boolean waitingforReservedConnections(List <Connection> connections) throws InterruptedException {
        List <Connection> alreadyReserved = new LinkedList<>();
        //try to get all locks of the route
        boolean reservedAll;
        while(true) {
            reservedAll = true;
            for (Connection c : connections) {
                //if couldn't get one lock, free all previous hold locks
                if (!c.getLock().tryLock()) {
                    for (Connection con : alreadyReserved) {
                        con.getLock().unlock();
                        alreadyReserved.remove(con);
                    }
                    reservedAll = false;
                    break;

                } else {
                    alreadyReserved.add(c);
                }
            }
            if(reservedAll) return reservedAll;
        }
    }

}
