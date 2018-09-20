package com.senatrex.dbasecollector.instrumentsdealers;

import com.senatrex.dbasecollector.marketinstruments.TStock;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
import com.senatrex.firebirdsample.pdbaseworking.DBaseWorking;

/**
 * <p>
 * class adds intruments from bloomberg dbase to system<br>
 * updated 10 авг. 2015 г.15:32:56
 * @author Alexander Kumenkov
 *  </p>
 */
public class TImportUsaDealer extends TAbstractDealer {
	
	private int fMarketDepth;
	
	/**
	 * <p>
	 * Constructor initializes values
	 * @param aDriver connection driver, used to connect to dbase
	 * @param aConnectionString address to dbase
	 * @param aLogin login to dbase
	 * @param aPassword password to dbase
	 * @param aMarketDepth depth of market, using for initializing system
	 *  </p>
	 */
	public TImportUsaDealer( String aDriver, String aConnectionString, String aLogin, String aPassword, int aMarketDepth ) {
		super( aDriver, aConnectionString, aLogin, aPassword );
		fMarketDepth = aMarketDepth;
	}
	
	/**
	 * Method initializes table of instruments
	 * @return instruments avaliable to collect info for them
	 */
	public String[ ][ ] initializeSystem( ) {
		TLocalMdataTable lLocalMdataTable = TLocalMdataTable.getInstance( );
		DBaseWorking lDBaseWorking = new DBaseWorking( fConnectionString, fLogin, fPassword);
		String [ ][ ] lResuilt = lDBaseWorking.GetQueryAsStringArr( "select securityname, isin, ric, exdest from securities" );
		
		for( int i = 1; i < lResuilt.length; i++ ) {
			
			String [ ][ ] lExistingTickTables = lDBaseWorking.GetQueryAsStringArr( "SELECT lower(tablename) FROM pg_tables where tablename='" + lResuilt[ i ][ 0 ] + "-TICK'" );
			if( lExistingTickTables.length == 1 ) {
				lDBaseWorking.ExecuteUpdateQuery("CREATE TABLE \""+lResuilt[ i ][ 0 ]+"-TICK\"(  curr_time timestamp(3) without time zone NOT NULL,  party_side smallint,  price double precision,  size bigint,  id bigserial NOT NULL, open_interest bigint, conditioncode character varying(10), PRIMARY KEY (id))WITH (  OIDS=FALSE);ALTER TABLE \"" + lResuilt[ i ][ 0 ] + "-TICK\" OWNER TO postgres;");
			}
			
			lLocalMdataTable.addInstrument( new TStock( lResuilt[ i ][ 0 ], lResuilt[ i ][ 1 ], fMarketDepth ) );
			
		}
		return lResuilt;
	}
}