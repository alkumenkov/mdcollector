package com.senatrex.dbasecollector.netprotocols;

import java.net.Socket;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

/**
 * <p>
 * Exemplar of this class sends answers with latest ask and bid data to follower <br>
 * updated 03 авг. 2015 г.15:32:22
 * @author Alexander Kumenkov
 *  </p>
 */
public class TAskBidMessaging extends TAbstractTcpMessaging{
	
    /**
     * Constructor initializes values
     */
    public TAskBidMessaging( Socket aSocket) {
        super( aSocket );
    }

    /**
     * Method generates message for client as < Ticker; ask price; bid price >
     * @param aAbstractInstrument its values will be compiled to answer
     */
    public void compileMessage( TAbstractInstrument  aAbstractInstrument ){
        String lAnswer = "<" + aAbstractInstrument.getLocalName( ) + ";" + String.format("%4.8f", aAbstractInstrument.getAsk( ).get(0).getPrice( ) ) + ";" +
                                        String.format( "%4.8f", aAbstractInstrument.getBid( ).get(0).getPrice( ) ) + ">";
        TAsyncLogQueue.getInstance( ).AddRecord( "answered "+lAnswer, 2 );
        super.sendMessage( lAnswer );

    }
}