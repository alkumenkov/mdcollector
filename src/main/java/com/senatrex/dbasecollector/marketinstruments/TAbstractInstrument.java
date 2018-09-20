package com.senatrex.dbasecollector.marketinstruments;

import java.util.ArrayList;

import com.senatrex.dbasecollector.netprotocols.TAbstractTcpMessaging;


/**
 * <p>
 * Exemplar of this class has information about latest market values<br>
 * updated 03 авг. 2015 г.13:47:52
 * @author Alexander Kumenkov
 * </p>
 */
public abstract class TAbstractInstrument {
	
	protected String fLocalName;
	protected String fExchangeName;
	protected String fBirthDate;
	
	protected ArrayList<TMarketOperation> fAsk;
	protected ArrayList<TMarketOperation> fBid;
	protected TTickOperation fTick;
	protected int fMarketDepth;
	protected ArrayList< TAbstractTcpMessaging > fClients;
	
	/**
	 * Constructor initializes values
	 * @param aLocalName name used in dbase and in local nets
	 * @param aExchangeName name used in exchanges 
	 * @param aMarketDepth market depth of this instrument
	 */
	public TAbstractInstrument( String aLocalName, String aExchangeName, int aMarketDepth ) {
		fMarketDepth = aMarketDepth;
		fLocalName = aLocalName;
		fExchangeName = aExchangeName;

		fAsk = new ArrayList<TMarketOperation>( aMarketDepth );
		fBid = new ArrayList<TMarketOperation>( aMarketDepth );
		for( int i = 0; i < fMarketDepth; i++ ) {
			fAsk.add( new TMarketOperation( ) );
			fBid.add( new TMarketOperation( ) );
		}
		fTick = new TTickOperation( );
		
		fClients = new ArrayList< TAbstractTcpMessaging >();
	}
	
	
	/**
	 * Method gets actual Ask operation
	 * @return actual Ask operation
	 */
	public ArrayList<TMarketOperation> getAsk( ) { 
		return fAsk;
	}
	
	/**
	 * Method gets actual Bid operation
	 * @return actual Bid operation
	 */
	public ArrayList<TMarketOperation> getBid( ) { 
		return fBid;
	}
	
	/**
	 * Method gets market depth of instrument
	 * @return market depth of instrument
	 */
	public int getMarketDepth(){
		return fMarketDepth;
	}
	
	/**
	 * Method gets latest Trade operation
	 * @return latest Trade operation
	 */
	public TTickOperation getTick( ) { 
		return fTick;
	}
	
	/**
	 * Method gets name, which used in dbase and in local net
	 * @return name, which used in dbase and in local net
	 */
	public String getLocalName( ) {
		return fLocalName;
	}
	              
	/**
	 * Method gets name, which used in exchanges
	 * @return name, which used in exchanges
	 */
	public String getExchangeName( ) {
		return fExchangeName;
	}
	
	/**
	 * Method gets list of client connections
	 * @return list of client connections
	 */
	public ArrayList<TAbstractTcpMessaging> getClients( ) {
		return fClients;
	}
	
	public void update( ) {	
	}
}