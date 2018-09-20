package com.senatrex.dbasecollector.marketinstruments;

/**
 * <p>
 * Exemplar of this class has information about latest market values of futures<br>
 * updated 04 авг. 2015 г.12:42:38
 * @author Alexander Kumenkov
 * </p>
 */
public class TFutures extends TAbstractInstrument {
	
	private int fOpenInterest;
	
	/**
	 * Constructor initializes values
	 * @param aLocalName name used in dbase and in local nets
	 * @param aExchangeName name used in exchanges 
	 * @param aMarketDepth market depth of this instrument
	 */
	public TFutures( String aLocalName, String aExchangeName, int aMarketDepth ) {
		super( aLocalName, aExchangeName, aMarketDepth );
		fOpenInterest = 0;
	}
	
	/**
	 * Method gets latest value of open interest
	 * @return latest value of open interest
	 */
	public int getOpenInterest( ) {
		return fOpenInterest;
	}
	
	/**
	 * Method sets latest value of open interest
	 * @param latest value of open interest
	 */
	public void setOpenInterest( int aOpenInterest ) {
		fOpenInterest = aOpenInterest;
	}	
}