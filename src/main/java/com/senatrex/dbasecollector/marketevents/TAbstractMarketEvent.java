package com.senatrex.dbasecollector.marketevents;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
import com.senatrex.dbasecollector.ptimeutilities.TTimeUtilities;

/**
 * <p>
 * Exemplar of this class updates market data in mdata table<br>
 * updated 04 авг. 2015 г.16:56:47
 * @author Alexander Kumenkov
 * </p>
 */
public abstract class TAbstractMarketEvent {
	
	
	protected String fExchName, fBirthDate;
	
	/**
	 * Constructor initializes values
	 * @param aExchName name used in exchanges 
	 */
	public TAbstractMarketEvent( String aExchName ) {
		fExchName = aExchName;
		fBirthDate = TTimeUtilities.GetCurrentTime( "yyyy-MM-dd HH:mm:ss.SSS" );
	}
	
	/**
	 * Method returns name used in exchanges 
	 * @return name used in exchanges 
	 */
	public String getExchName() {
		return fExchName;
	}
	
	/**
	 * Callback of  theese objects. Used to activate them in query
	 */
	public void doWork( ) {
		TLocalMdataTable lLocalMdataTable = TLocalMdataTable.getInstance( );
		lLocalMdataTable.updateTable( this );
	}
	
	/**
	 * Method generates query for table like this example
	 * CREATE TABLE "SIBN"
		(
		  securityid character varying(25),
		  loctime timestamp(3) without time zone NOT NULL,
		  sendingtime timestamp(3) without time zone,
		  optime timestamp(3) without time zone,
		  booksize smallint,
		  bidprice double precision[],
		  bidvolume integer[],
		  askprice double precision[],
		  askvolume integer[],
		  lastprice double precision,
		  lastvolume integer,
		  id bigint NOT NULL DEFAULT nextval('"SIBN_sq"'::regclass),
		  totalvolume bigint DEFAULT 0,
		  CONSTRAINT "SIBN_pkey" PRIMARY KEY (id)
		)
		WITH (
		  OIDS=FALSE
		);
	 * 
	 * @param aAbstractInstrument TAbstractInstrument, needed to update table
	 */
	protected String getDBaseQuery( TAbstractInstrument aAbstractInstrument ) {
		StringBuffer lAskPriceBuff = new StringBuffer();
		StringBuffer lAskSizeBuff = new StringBuffer();
		
		lAskPriceBuff.append( "{" );
		lAskSizeBuff.append( "{" );
		for( int i = 0; i < aAbstractInstrument.getAsk( ).size( ); i++ ) {
			lAskPriceBuff.append( aAbstractInstrument.getAsk( ).get( i ).getPrice( ) );
			lAskSizeBuff.append( aAbstractInstrument.getAsk( ).get( i ).getVolume( ) );
			if( i != aAbstractInstrument.getAsk( ).size( ) - 1 ) {
				lAskPriceBuff.append( " , " );
				lAskSizeBuff.append( " , " );
			}
		}
		lAskPriceBuff.append( "}" );
		lAskSizeBuff.append( "}" );
		
		StringBuffer lBidPriceBuff = new StringBuffer();
		StringBuffer lBidSizeBuff = new StringBuffer();
		
		lBidPriceBuff.append( "{" );
		lBidSizeBuff.append( "{" );
		for( int i = 0; i < aAbstractInstrument.getBid( ).size( ); i++ ) {
			lBidPriceBuff.append( aAbstractInstrument.getBid( ).get( i ).getPrice( ) );
			lBidSizeBuff.append( aAbstractInstrument.getBid( ).get( i ).getVolume( ) );
			if( i != aAbstractInstrument.getBid( ).size( ) - 1 ) {
				lBidPriceBuff.append( " , " );
				lBidSizeBuff.append( " , " );
			}
		}
		lBidPriceBuff.append( "}" );
		lBidSizeBuff.append( "}" );
		//aAbstractInstrument.getLocalName()
		String lQueryToDbase = "insert into \""+aAbstractInstrument.getLocalName()+"\" (securityid, loctime, bidprice,bidvolume,askprice,askvolume) values ('" 
									+ aAbstractInstrument.getLocalName() + "', '" + fBirthDate + "', '" + lBidPriceBuff + "', '" + lBidSizeBuff + "', '" + lAskPriceBuff + "', '" + lAskSizeBuff +"');\r\n";
		return lQueryToDbase;
	}
	
	/**
	 * Method sets  value of different fields of TAbstractInstrument depending on type of this objects
	 * @param aAbstractInstrument TAbstractInstrument, which fields needs to update
	 */
	public abstract void updateInstrument( TAbstractInstrument aAbstractInstrument );
}