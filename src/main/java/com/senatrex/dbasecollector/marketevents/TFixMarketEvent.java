package com.senatrex.dbasecollector.marketevents;

import java.util.ArrayList;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.netprotocols.TAbstractTcpMessaging;
import com.senatrex.dbasecollector.netprotocols.TAskBidMessaging;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import com.senatrex.dbasecollector.queues.TDBaseQueue;

public class TFixMarketEvent extends TAbstractMarketEvent{
	
	private ArrayList<TMarketOperation> fAskArray, fBidArray;
	
	/**
	 * Constructor initializes values
	 * @param aExchName name used in exchanges 
	 * @param aPrice price of ask order
	 * @param aVolume size of ask order
	 */
	public TFixMarketEvent( String aExchName, ArrayList<TMarketOperation> aAskArray, ArrayList<TMarketOperation> aBidArray ) {
		super( aExchName );
		fAskArray = aAskArray;
		fBidArray = aBidArray;
	}
	
	/**
	 * Method sets  value of different fields of TAbstractInstrument depending on type of this objects
	 * @param aAbstractInstrument TAbstractInstrument, which fields needs to update
	 */
	public void updateInstrument( TAbstractInstrument aAbstractInstrument ) {
            //int lMarketDepth = aAbstractInstrument.getMarketDepth();
            try {	
                
		for( int i = 0; i < fAskArray.size(); i++ ) {
                    aAbstractInstrument.getAsk( ).get( i ).setPrice( fAskArray.get( i ).getPrice( ) );
                    aAbstractInstrument.getAsk( ).get( i ).setVolume( fAskArray.get( i ).getVolume( ) );
		}
		
		for( int i = 0; i < fBidArray.size(); i++ ) {
                    aAbstractInstrument.getBid( ).get( i ).setPrice( fBidArray.get( i ).getPrice( ) );
                    aAbstractInstrument.getBid( ).get( i ).setVolume( fBidArray.get( i ).getVolume( ) );
		}
		
		if( ( aAbstractInstrument.getAsk().get(0).getPrice() != aAbstractInstrument.getAsk().get(0).getPrevPrice( ) ) 
                        ||
                    ( aAbstractInstrument.getBid().get(0).getPrice() != aAbstractInstrument.getBid().get(0).getPrevPrice( ) ) ) {
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
		}
                
		String lQuery = getDBaseQuery( aAbstractInstrument );
		
		TDBaseQueue lDBaseQueue = TDBaseQueue.getInstance( );
		lDBaseQueue.AddRecord( lQuery );
                TAsyncLogQueue.getInstance().AddRecord( lQuery, 2 );
            } catch ( Exception e ) {
                    TAsyncLogQueue.getInstance().AddRecord( "TFixMarketEvent.updateInstrument\t"+e.getClass( ).getName( ) + ": " + e.getMessage( ), 0 );
            }
	}
}