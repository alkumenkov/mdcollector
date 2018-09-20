package com.senatrex.dbasecollector.queues;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.senatrex.dbasecollector.marketevents.TAbstractMarketEvent;

/**
 * <p>
 * Exemplar of this class catches market messages <br>
 * updated 04 авг. 2015 г.16:56:47
 *
 * @author Alexander Kumenkov
 * </p>
 */
public class TMarketEventQueue {

    private static TMarketEventQueue fAsyncQueue = null;
    private Queue< TAbstractMarketEvent> fQueue = null;
    private Thread fThread = null;
    private boolean fTerminate = false;
    private Object fWaitObject = new Object();
    private int sizeQueue;

    private TMarketEventQueue() {

        fQueue = new ConcurrentLinkedQueue< TAbstractMarketEvent>();
        fThread = new Thread(new TPopThread());
        fThread.start();
        fTerminate = false;
        sizeQueue = 0;
    }

    private class TPopThread implements Runnable {

        public TPopThread() {
        }

        public void run() {

            while (!fTerminate || !fQueue.isEmpty()) {
                //try {

                if (!fTerminate && fQueue.isEmpty()) {
                    try {
                        synchronized (fWaitObject) {
                            fWaitObject.wait();
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        TAsyncLogQueue.getInstance().AddRecord(e.getClass().getName() + ": " + e.getMessage(), 0);
                    }
                }
                doWork();
                //} catch (NullPointerException e) {
                //	System.out.println(e.toString());
                //}
            }
            
            TAsyncLogQueue.getInstance().AddRecord( "exit from run block", 0 );
        }

        protected void doWork() {

            if (fQueue.size() > 0) {
                TAbstractMarketEvent lAbstractMarketEvent = fQueue.poll();

                sizeQueue = fQueue.size();
                if (lAbstractMarketEvent != null) {
                    lAbstractMarketEvent.doWork();
                }
            }
        }
    }

    /**
     * <p>
     * In first calling creates safe queue object. Then return queue object
     *
     * @return Reference to queue Object
     * </p>
     */
    public static synchronized TMarketEventQueue getInstance() {
        if (fAsyncQueue == null) {
            fAsyncQueue = new TMarketEventQueue();
        }

        return fAsyncQueue;

    }

    /**
     * <p>
     * Adds item to Queue. Adds to item time since initializing in nanos
     *
     * @param aAbstractMarketEvent any event came from market
     * </p>
     */
    public void AddRecord(TAbstractMarketEvent aAbstractMarketEvent) {
        synchronized ( fWaitObject ) {
            fWaitObject.notify();
            fQueue.add( aAbstractMarketEvent );
            sizeQueue = fQueue.size();
        }
    }

    /**
     * <p>
     * Clears queue
     * </p>
     */
    public void clearBuffer() {
        fQueue.clear();
    }

    /**
     * <p>
     * Waiting for finishing all popers ant terminates process
     * </p>
     */
    public void finalize() throws Throwable {
        System.out.println("start terminate");
        fTerminate = true;
        synchronized ( fWaitObject ) {
            fWaitObject.notify();
        }

        fThread.join();
        fAsyncQueue = null;
        System.out.println("terminated!");

    }

}
