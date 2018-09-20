package com.senatrex.dbasecollector.mkdatasources;

import com.senatrex.dbasecollector.marketevents.TTradeMarketEvent;
import com.senatrex.dbasecollector.queues.TMarketEventQueue;

public class TTestingCollector extends TAbstractMkDataCollector {
	
	public void run( ) {
        TTradeMarketEvent lTradeMarketEvent = new TTradeMarketEvent( "VEU5 Index", 13.50, 1000, 1, "cc");  	
    	TMarketEventQueue lMarketEventQueue = TMarketEventQueue.getInstance();
    	
    	while(fIsClosed==false){
    		try {
    			Thread.sleep( 1000 );
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		lMarketEventQueue.AddRecord( lTradeMarketEvent );
    	}
	}
	
	/** 
	 * @see com.senatrex.dbasecollector.mkdatasources.TAbstractMkDataCollector#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}
        
       
}