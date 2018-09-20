package com.senatrex.dbasecollector.pmainpac;

import com.senatrex.dbasecollector.alarms.TAlarmSignal;
import com.senatrex.dbasecollector.mkdatasources.TAbstractMkDataCollector;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import com.senatrex.dbasecollector.queues.TDBaseQueue;
import com.senatrex.firebirdsample.pdbaseworking.DBaseWorking;
import java.util.List;

/**
 * <p>
 * Class once a time period watches processes and reports about mistakes<br>
 * updated 21 авг. 2015 г.17:11:20
 *
 * @author Alexander Kumenkov
 * </p>
 */
public class TStatusClock {

    private static Thread fThread = null;
    private static boolean fIsWorking;
    private static int fDelay;
    private static  List<TAbstractMkDataCollector> fAbstractMkDataCollectors;

    public static void initCollector( List<TAbstractMkDataCollector> aAbstractMkDataCollectors ) {
        fAbstractMkDataCollectors = aAbstractMkDataCollectors;
        fQueueSize = 1000;
    }

    public static void startClock( int aDelay ) {
        fDelay = aDelay;
        fIsWorking = true;
        if (fThread == null) {
            fThread = new Thread(new Runnable() {
                public void run() {
                    while ( fIsWorking ) {
                        try {
                            Thread.sleep(fDelay);
                            tickTock();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            TAsyncLogQueue.getInstance().AddRecord(e.getClass().getName() + ": " + e.getMessage(), 0);
                            fIsWorking = false;
                        }
                    }
                }
            });
            fThread.start();
        }
    }

    public static void stopClock() {
        fIsWorking = false;
    }

    private static int fQueueSize;
    private static String fStatus = "";

    private static void tickTock() {
        fStatus = "";
        
        fAbstractMkDataCollectors.forEach( 
            ( lCollector )->{
                if ( !lCollector.isEnabled() ) {     
                   TAlarmSignal.startAlarm(200, 3);
                   fStatus += "Connector Problem!";
                }
            });
        

        String lLastEcxeption = DBaseWorking.GetLastException();
        if (lLastEcxeption.length() > 0) {
            fStatus += "DBase Error!Check log!";
            TAlarmSignal.startAlarm(100, 2);
        }

        Integer lQueueSize = TDBaseQueue.getInstance().getQueueSize();

        if (lQueueSize > fQueueSize) {
            fStatus += "Queue size too big!";
            TAlarmSignal.startAlarm(500, 2);
            fQueueSize = fQueueSize * 10;
        }

        if (lQueueSize > 300000) {
            TDBaseQueue.getInstance().clearBuffer();
        }

        if (lQueueSize < 1000) {
            fQueueSize = 1000;
        }

        TUserInterFace.changeSizeText(lQueueSize.toString());
        TUserInterFace.changeStatusText(fStatus);
    }

}
