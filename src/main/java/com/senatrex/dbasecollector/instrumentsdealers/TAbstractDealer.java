package com.senatrex.dbasecollector.instrumentsdealers;

/**
 * <p>
 * childs of this class initializes table of instuments from different dbases<br>
 * updated 14 авг. 2015 г.16:31:23
 * @author Alexander Kumenkov
 *  </p>
 */
public abstract class TAbstractDealer {
	
	protected String fDriver;
	protected String fConnectionString;
	protected String fLogin;
	protected String fPassword;
	
	/**
	 * <p>
	 * Constructor initializes values
	 * @param aDriver connection driver, used to connect to dbase
	 * @param aConnectionString address to dbase
	 * @param aLogin login to dbase
	 * @param aPassword password to dbase
	 *  </p>
	 */
	public TAbstractDealer( String aDriver, String aConnectionString, String aLogin, String aPassword ) {
		fDriver = aDriver;
		fConnectionString = aConnectionString;
		fLogin = aLogin;
		fPassword = aPassword;
	}
	
	/**
	 * Method initializes table of instruments and prepraing database
	 * @return instruments avaliable to collect info for them
	 */
	public abstract String[ ][ ] initializeSystem( );
}