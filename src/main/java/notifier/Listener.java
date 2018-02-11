package notifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Abstract class to implement a Listener class
 * The class is thread safe, it allows multiple reader concurrently and one writer
 * @param <T> type of Listener
 */
public abstract class Listener<T> {
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private List<T> listeners = new ArrayList<>();

    /**
     * Add a listener to the list
     * @param listener to add
     */
    public synchronized void addListener( T listener ) {
        readWriteLock.writeLock().lock();
        listeners.add(listener);
        readWriteLock.writeLock().unlock();
    }

    /**
     * Remove a listener from the list
     * @param listener to be removed
     */
    public void removeListener( T listener ) {
        readWriteLock.writeLock().lock();
        listeners.remove(listener);
        readWriteLock.writeLock().lock();
    }

    /**
     * Notify the listeners of a changed to the observed element firing an event
     * @param consumer consumer
     */
    void notifyListeners(Consumer<? super T> consumer) {
        listeners.forEach(consumer);
    }
}
