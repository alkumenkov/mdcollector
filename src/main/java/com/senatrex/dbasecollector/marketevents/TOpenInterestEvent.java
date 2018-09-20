package com.senatrex.dbasecollector.marketevents;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.marketinstruments.TFutures;

public class TOpenInterestEvent extends TAbstractMarketEvent {

	private int fOpenInterest;
	/**
	 * @param aExchName
	 */
	public TOpenInterestEvent( String aExchName, int aOpenInterest ) {
		super(aExchName);
		// TODO Auto-generated constructor stub
		fOpenInterest = aOpenInterest;
	}

	/* (non-Javadoc)
	 * @see com.senatrex.dbasecollector.marketevents.TAbstractMarketEvent#updateInstrument(com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument)
	 */
	@Override
	public void updateInstrument(TAbstractInstrument aAbstractInstrument) {
		// TODO Auto-generated method stub
		if( aAbstractInstrument instanceof TFutures ) {
			( ( TFutures )aAbstractInstrument ).setOpenInterest( fOpenInterest );
		}
	}
	
}