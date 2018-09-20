package com.senatrex.dbasecollector.netprotocols;

import java.net.Socket;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;

/**
 * <p>
 * Exemplar of this class sends answers with tick data to follower <br>
 * updated 03 авг. 2015 г.15:32:22
 * @author Alexander Kumenkov
 *  </p>
 */
public class TTickMessaging extends TAbstractTcpMessaging{
	
	/**
	 * Constructor initializes values
	 * @param aSocket via it all data will be sent to client
	 */
	public TTickMessaging( Socket aSocket) {
		super( aSocket );
	}
	
	/**
	 * Method generates message for client as < Ticker; side; price; size >
	 * @param aAbstractInstrument its values will be compiled to answer
	 */
	public void compileMessage( TAbstractInstrument  aAbstractInstrument ){
		String lAnswer = "<" + aAbstractInstrument.getLocalName( ) + ";" + aAbstractInstrument.getTick( ).getSide( ) + ";" +
						String.format("%4.8f",aAbstractInstrument.getTick( ).getPrice( )) + ";" + aAbstractInstrument.getTick( ).getVolume( ) + ">";
		
		super.sendMessage( lAnswer );
	}
}