package com.senatrex.dbasecollector.netprotocols;

import java.net.Socket;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.marketinstruments.TFutures;

/**
 * <p>
 * Exemplar of this class sends answers with latest markert data with open interest data to follower <br>
 * updated 04 авг. 2015 г.12:17:33
 * @author Alexander Kumenkov
 *  </p>
 */
public class TFuturesMessaging extends TAbstractTcpMessaging{
	
	/**
	 * Constructor initializes values
	 * @param aSocket via it all data will be sent to client
	 */
	public TFuturesMessaging( Socket aSocket) {
		super( aSocket );
	}
	
	/**
	 * Method generates message for client as < Ticker; side; price; size; open_interest >
	 * @param aAbstractInstrument its values will be compiled to answer
	 */
	public void compileMessage( TAbstractInstrument  aAbstractInstrument ){
		if( aAbstractInstrument instanceof TFutures ) {		
			String lAnswer = "<" + aAbstractInstrument.getLocalName( ) + ";" + aAbstractInstrument.getTick( ).getSide( ) + ";" +
					String.format( "%4.8f", aAbstractInstrument.getTick( ).getPrice( ) ) + ";" + aAbstractInstrument.getTick( ).getVolume( ) + ";" +
					( ( TFutures )aAbstractInstrument ).getOpenInterest( ) + ">";
					
			super.sendMessage( lAnswer );
		}
	}
}