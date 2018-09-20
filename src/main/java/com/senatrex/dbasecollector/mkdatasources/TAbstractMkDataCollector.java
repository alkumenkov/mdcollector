package com.senatrex.dbasecollector.mkdatasources;

import com.senatrex.dbasecollector.queues.TMarketEventQueue;
import com.senatrex.dbasecollector.queues.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * <p>
 * Childs  of this class gets market data from different sources converts <br>
 * updated 10 авг. 2015 г.18:04:28
 * @author Alexander Kumenkov
 *  </p>
 */
public abstract class TAbstractMkDataCollector implements Runnable{
	
	protected TAbstractQueue fAbstractQueue;
	protected String[][] fInstruments;
	protected TMarketEventQueue fMarketEventQueue;
        protected boolean fIsClosed;
	protected int fMarketDepth;
        
        public void closeCollector(){
        fIsClosed = true; 
            try {
                TMarketEventQueue.getInstance().finalize();
            } catch (Throwable ex) {
                Logger.getLogger(TTestingCollector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
	public TAbstractMkDataCollector(){
		fMarketEventQueue = TMarketEventQueue.getInstance( );
                fMarketDepth = 1;
	}
	
	/**
	 * Method adds instruments for download market data
	 * @param aInstruments array of instruments
	 */
	public void addInstruments( String[][] aInstruments ){
		fInstruments = aInstruments;
	}
	
	/**
	 * Method catches market data, generates event and send it to query
	 */
	public abstract void run();
	
	/**
	 * Method uses for status clock, if it return false, app will alarm
	 */
	public abstract boolean isEnabled();
}