package com.senatrex.dbasecollector.marketinstruments;

/**
 * <p>
 * Exemplar of this class has information about latest market values of Stocks<br>
 * updated 04 авг. 2015 г.12:42:38
 * @author Alexander Kumenkov
 * </p>
 */
public class TStock extends TAbstractInstrument {
	
    /**
     * Constructor initializes values
     * @param aLocalName name used in dbase and in local nets
     * @param aExchangeName name used in exchanges 
     * @param aMarketDepth market depth of this instrument
     */
    public TStock( String aLocalName, String aExchangeName, int aMarketDepth ) {
        super( aLocalName, aExchangeName, aMarketDepth );
    }
}