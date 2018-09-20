package com.senatrex.dbasecollector.marketinstruments;

/**
 * <p>
 * Exemplar of this class has information about market values of last trade<br>
 * Side of trade added
 * updated 03 авг. 2015 г.13:05:56
 * @author Alexander Kumenkov
 *  </p>
 */
public class TTickOperation extends TMarketOperation {
	
	/**
	 * <p>
	 * Side of trade. If trade is sell, fSide is 2; if trade is buy, fSide is 1; Otherwise fSide is 0;
	 *  </p>
	 */
	private int fSide;
	private int fPrevSide;
	
	/**
	 * Constructor initializes values
	 */
	public TTickOperation( ) {
            super();
            fSide = 0;
	}
	
	/**
	 * Method gets side of latest operation
	 * @return Side of latest operation
	 */
	public int getSide( ) {
            return fSide;
	}
	
	/**
	 * Method sets side of latest operation
	 * @param aSide side of latest trade. 2 if sell, 1 if buy, 0 if any
	 */
	public void setSide( int aSide ) {
		fPrevSide = fSide;
		fSide = aSide;
	}
	
	/**
	 * Method gets side of previous operation
	 * @return Side of previous operation
	 */
	public int getPrevSide( ) {
		return fPrevSide;
	}

}