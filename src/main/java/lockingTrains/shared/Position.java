package lockingTrains.shared;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract base class of {@link Location} and {@link Connection}. May be used
 * to reference a location or connection interchangeably.
 */
public abstract class Position {

    //ROMAN
    final private Lock lock = new ReentrantLock();

    public Lock getLock(){
        return lock;
    }

}
