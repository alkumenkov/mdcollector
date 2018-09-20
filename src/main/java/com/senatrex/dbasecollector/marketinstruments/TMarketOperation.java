package com.senatrex.dbasecollector.marketinstruments;

/**
 * <p>
 * Exemplar of this class has information about latest market values<br>
 * updated 03 авг. 2015 г.13:05:56
 * @author Alexander Kumenkov
 *  </p>
 */
public class TMarketOperation {
	
	protected double fPrice;
	protected double fPrevPrice;
	protected int fVolume;

	/**
	 * Constructor initializes values
	 */
	public TMarketOperation( ) {
		fPrice = 0;
		fVolume = 0;
		fPrevPrice = 0;
	}
        
        /**
	 * Constructor initializes values
	 */
	public TMarketOperation(double aPrice, int aVolume ) {
		fPrice = aPrice;
		fVolume = aVolume;
		fPrevPrice = 0;
	}
	
	/**
	 * Method gets price of latest operation
	 * @return price of latest operation
	 */
	public double getPrice( ) {
		return fPrice;
		
	}
	
	/**
	 * Method gets previous price value. Needs to count side of tick
	 * @return previous price value
	 */
	public double getPrevPrice( ) {
		return fPrevPrice;	
	}
	
	/**
	 * Method sets price of latest operation
	 * @param aPrice price of latest operation
	 */
	public void setPrice( double aPrice ) {
		fPrevPrice = fPrice;
		fPrice = aPrice;
		
	}
	
	/**
	 * Method gets volume of latest operation
	 * @return Volume of latest operation
	 */
	public int getVolume( ) {
		return fVolume;
	}
	
	/**
	 * Method sets volume of latest operation
	 * @param aVolume volume of latest operation
	 */
	public void setVolume( int aVolume ) {
		fVolume = aVolume;
	}
}