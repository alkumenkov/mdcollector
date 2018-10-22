package com.senatrex.dbasecollector.alarms;

import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * This Class Calls alarm signal from anywhere in program<br>
 * updated 21 авг. 2015 г.15:50:18
 * @author Alexander Kumenkov
 * </p>
 */
public class TAlarmSignal {
	
	private static int fAlarmDelay, fAlarmCount;
	private static Object fWaitingObject = new Object();
        private static boolean fClosed = false;
	private static Thread fThread = null;
	
        public static void close( ){
            fClosed = true;           
            fAlarmCount = 0;            
            synchronized( fWaitingObject ){                
                fWaitingObject.notify();                
            }
        }
        
	public static void startAlarm( int AlarmDelay, int aAlarmCount ) {

            fAlarmDelay = AlarmDelay;
            fAlarmCount = aAlarmCount;
            
            synchronized( fWaitingObject ){                
                fWaitingObject.notify();                
            }

            if( fThread == null ){
                fThread = new Thread(new Runnable(){
                    public void run(){
                        while(!fClosed){
                            for( int i=0; i<fAlarmCount; i++ ) {
                                try {
                                    Toolkit.getDefaultToolkit().beep();
                                    Thread.sleep( fAlarmDelay );
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                            }
                            synchronized(fWaitingObject){
                                try {
                                    fWaitingObject.wait();
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TAlarmSignal.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    };
                });
                fThread.start();
            }
		
	}
		
}