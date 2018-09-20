package com.senatrex.dbasecollector.alarms;

import java.awt.Toolkit;

/**
 * <p>
 * This Class Calls alarm signal from anywhere in program<br>
 * updated 21 авг. 2015 г.15:50:18
 * @author Alexander Kumenkov
 * </p>
 */
public class TAlarmSignal {
	
	private static int fAlarmDelay, fAlarmCount;
	
	private static Thread fThread = null;
	
	public static void startAlarm( int AlarmDelay, int aAlarmCount ) {

		fAlarmDelay = AlarmDelay;
		fAlarmCount = aAlarmCount;
		
		fThread = new Thread(new Runnable(){
                    public void run(){
                        for( int i=0; i<fAlarmCount; i++ ) {
                            try {
                                Toolkit.getDefaultToolkit ().beep ();
                                Thread.sleep( fAlarmDelay );
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    };
		});
		fThread.start();
		
	}
		
}