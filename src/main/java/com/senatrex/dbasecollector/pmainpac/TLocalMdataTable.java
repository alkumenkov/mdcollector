package com.senatrex.dbasecollector.pmainpac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.senatrex.dbasecollector.marketevents.TAbstractMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.netprotocols.TAskBidMessaging;
import com.senatrex.dbasecollector.netprotocols.TFuturesMessaging;
import com.senatrex.dbasecollector.netprotocols.TTickMessaging;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

/**
 * <p>
 * Table has instruments to collect local histoty of them<br>
 * updated 04 авг. 2015 г.16:56:47
 * @author Alexander Kumenkov
 * </p>
 */
public class TLocalMdataTable {
	
	private static TLocalMdataTable fLocalMdataTable;
	private  ArrayList<TAbstractInstrument> fListOfInstruments;
	private HashMap< String,  TAbstractInstrument > fMapOfInstruments;
	
	/**
	 * <p>
	 * In first calling creates TLocalMdataTable
	 * @return Reference to queue TLocalMdataTable
	 *  </p>
	 */	
	public static synchronized TLocalMdataTable getInstance( ) {		
                if( fLocalMdataTable == null ) {
                    fLocalMdataTable = new TLocalMdataTable( );
                }
            return fLocalMdataTable;	
	}
	
	private TLocalMdataTable(  ) {
            fListOfInstruments = new ArrayList< TAbstractInstrument >( );
            fMapOfInstruments = new  HashMap< String, TAbstractInstrument >( );
	}
	
	/**
	 * Method updates Table of instruments with information come in new event
	 * @param aAbstractMaketEvent event, which updates table
	 */
	public void updateTable( TAbstractMarketEvent  aAbstractMarketEvent ) {	
            aAbstractMarketEvent.updateInstrument( fMapOfInstruments.get( aAbstractMarketEvent.getExchName() ) );
	}
	
	/**
	 * Method adds new instrument to table of instruments
	 * @param aAbstractInstrument new instruments
	 */
	public void addInstrument( TAbstractInstrument aAbstractInstrument  ) {
		fListOfInstruments.add( aAbstractInstrument );
		fMapOfInstruments.put( aAbstractInstrument.getExchangeName(), aAbstractInstrument );
	}
	
	private int getIndexByLocalName( String aLocalName ){
            for( int i = 0; i < fListOfInstruments.size( ); i++ ) {
                if( fListOfInstruments.get( i ).getLocalName( ).equals( aLocalName ) ) {
                    return i;
                }
            }
            return -1;
	}
	
	/**
	 * Method adds new client to some instrument included in the table of instruments
	 * @param aSocket socket, via him  will be dialog between tavle and client
	 */
	public void addListener( Socket aSocket ) {
		try {
			SocketChannel lsCh= aSocket.getChannel();
			
			BufferedReader ins = new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
			
			String lOut = "";
			char[ ] lBuffer = new char[ 1 ];
			
			while( ins.read( lBuffer )!=-1  ) {
				
				lOut += new String ( lBuffer );
				if( lBuffer[ 0 ] == '>' ) {
					parseSingleMessage( lOut.substring( lOut.indexOf( "<" ), lOut.length( ) ), aSocket );
					lOut = "";
				}
				Arrays.fill( lBuffer, '0' );
                                
			}
                        
                        TAsyncLogQueue.getInstance( ).AddRecord( "client disconnected", 0 );
			
		
		} catch ( Exception e ) {
			e.printStackTrace( );
			try {
				aSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
	}
	
	private void parseSingleMessage( String aMessage, Socket aSocket ) {
            
                TAsyncLogQueue.getInstance( ).AddRecord( aMessage, 0 );
            
		if( aMessage.indexOf( ";TRADES" ) > -1 ) {
			String lName = aMessage.substring( 1, aMessage.indexOf( ";" ) );
			int lIndex = getIndexByLocalName( lName );
			if(lIndex>-1 )
			fListOfInstruments.get( lIndex ).getClients( ).add( new TTickMessaging( aSocket ) );
			return;
		}
	
		if( aMessage.indexOf( ";OI" ) > -1 ) {
			String lName = aMessage.substring( 1, aMessage.indexOf( ";" ) );
			int lIndex = getIndexByLocalName( lName );
			if(lIndex>-1 )
			fListOfInstruments.get( lIndex ).getClients( ).add( new TFuturesMessaging( aSocket ) );
			return;
		}  
		
		if( aMessage.indexOf( ";ASK_BID" ) > -1 ) {
			String lName = aMessage.substring( 1, aMessage.indexOf( ";" ) );
			int lIndex = getIndexByLocalName( lName );
			if( lIndex>-1 )
                            fListOfInstruments.get( lIndex ).getClients( ).add( new TAskBidMessaging( aSocket ) );
                            TAsyncLogQueue.getInstance( ).AddRecord( aMessage + " added", 1 );
			return;
		}
		
		int lIndex = getIndexByLocalName( aMessage.substring( 1, aMessage.indexOf( ">" ) ) );
                
		if(lIndex>-1 )
		fListOfInstruments.get( lIndex ).getClients( ).add( new TAskBidMessaging( aSocket ) );
                TAsyncLogQueue.getInstance( ).AddRecord( aMessage + " added", 1 );
	}
}