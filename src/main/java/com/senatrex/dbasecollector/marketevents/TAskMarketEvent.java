package com.senatrex.dbasecollector.marketevents;

import java.util.ArrayList;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.netprotocols.TAbstractTcpMessaging;
import com.senatrex.dbasecollector.netprotocols.TAskBidMessaging;
import com.senatrex.dbasecollector.queues.TDBaseQueue;


/**
 * <p>
 * Exemplar of this class updates market data in mdata table<br>
 * updated 04 авг. 2015 г.16:56:47
 * @author Alexander Kumenkov
 * </p>
 */
public class TAskMarketEvent extends TAbstractMarketEvent{
	
    private double fPrice;
    private int fVolume;
    /**
     * Constructor initializes values
     * @param aExchName name used in exchanges 
     * @param aPrice price of ask order
     * @param aVolume size of ask order
     */
    public TAskMarketEvent( String aExchName, double aPrice, int aVolume ) {
        super( aExchName );

        fPrice = aPrice;
        fVolume = aVolume;
    }

    /**
     * Method sets  value of different fields of TAbstractInstrument depending on type of this objects
     * @param aAbstractInstrument TAbstractInstrument, which fields needs to update
     */
    public void updateInstrument( TAbstractInstrument aAbstractInstrument ) {
        aAbstractInstrument.getAsk( ).get(0).setPrice( fPrice );
        aAbstractInstrument.getAsk( ).get(0).setVolume( fVolume );

        ArrayList< TAbstractTcpMessaging > lListClients = aAbstractInstrument.getClients( );
        for( int i=0; i<lListClients.size(); i++ ) {
            if( lListClients.get( i ).isAvailable() == false ){
                lListClients.remove(i);
                i--;
                continue;
            }
            if( lListClients.get( i ) instanceof TAskBidMessaging ){
                ( ( TAskBidMessaging )( lListClients.get( i ) ) ).compileMessage( aAbstractInstrument );
            }
        }

        String lQuery = getDBaseQuery( aAbstractInstrument );

        TDBaseQueue lDBaseQueue = TDBaseQueue.getInstance( );
        lDBaseQueue.AddRecord( lQuery );
    }
}