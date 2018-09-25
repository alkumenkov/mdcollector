package com.senatrex.dbasecollector.instrumentsdealers;


import com.senatrex.firebirdsample.pdbaseworking.DBaseWorking;

/**
 * <p>
 * class adds intruments from bloomberg dbase to system<br>
 * updated 10 авг. 2015 г.15:32:56
 * @author Alexander Kumenkov
 *  </p>
 */
public class TImportBcsDealer extends TAbstractDealer {
	
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
	public TImportBcsDealer( String aDriver, String aConnectionString, String aLogin, String aPassword ) {
		super( aDriver, aConnectionString, aLogin, aPassword );

	}
	
	/**
	 * Method initializes table of instruments
	 * @return instruments avaliable to collect info for them
	 */
	public String[ ][ ] initializeSystem( ) {
		
		DBaseWorking lDBaseWorking = new DBaseWorking( fConnectionString, fLogin, fPassword);
		String [ ][ ] lResuilt = lDBaseWorking.GetQueryAsStringArr( "select securityname, isin, ric, exdest, min_amound from securities" );
		
		for( int i = 1; i < lResuilt.length; i++ ){
			String [ ][ ] lExistingTables = lDBaseWorking.GetQueryAsStringArr( "SELECT lower(tablename) FROM pg_tables where tablename='" + lResuilt[ i ][ 0 ] + "'" );
                        if( lExistingTables.length == 1 ) {
                            lDBaseWorking.ExecuteUpdateQuery("CREATE TABLE \""+lResuilt[ i ][ 0 ]+"\"(  securityid character varying(25),  loctime timestamp(3) without time zone NOT NULL,  sendingtime timestamp(3) without time zone,  optime timestamp(3) without time zone,  booksize smallint,  bidprice double precision[],  bidvolume integer[],  askprice double precision[],  askvolume integer[],  lastprice double precision,  lastvolume integer,  id bigserial NOT NULL,  totalvolume bigint DEFAULT 0, PRIMARY KEY (id))WITH (  OIDS=FALSE);ALTER TABLE \"" + lResuilt[ i ][ 0 ] + "\" OWNER TO postgres;");
			}
                        
                        String [ ][ ] lExistingTickTables = lDBaseWorking.GetQueryAsStringArr( "SELECT lower(tablename) FROM pg_tables where tablename='" + lResuilt[ i ][ 0 ] + "-TICK'" );                        
                        if( lExistingTickTables.length == 1 ) {
                            lDBaseWorking.ExecuteUpdateQuery("CREATE TABLE \""+lResuilt[ i ][ 0 ]+"-TICK\"(  curr_time timestamp(3) without time zone NOT NULL,  party_side smallint,  price double precision,  lot_quantity bigint,  id bigserial NOT NULL,  tick_size bigint, open_interest bigint, conditioncode character varying(10), PRIMARY KEY (id))WITH (  OIDS=FALSE);ALTER TABLE \"" + lResuilt[ i ][ 0 ] + "-TICK\" OWNER TO postgres;");
                        }
		}
		return lResuilt;
	}
}