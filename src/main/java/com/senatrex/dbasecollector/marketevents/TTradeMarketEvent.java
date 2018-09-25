package com.senatrex.dbasecollector.marketevents;

import java.util.ArrayList;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.marketinstruments.TFutures;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.netprotocols.TAbstractTcpMessaging;
import com.senatrex.dbasecollector.netprotocols.TFuturesMessaging;
import com.senatrex.dbasecollector.netprotocols.TTickMessaging;
import com.senatrex.dbasecollector.queues.TDBaseQueue;

/**
 * <p>
 * Exemplar of this class updates market data in mdata table<br>
 * updated 07 авг. 2015 г.15:23:28
 * @author Alexander Kumenkov
 * </p>
 */
public class TTradeMarketEvent extends TAbstractMarketEvent{
	
	private double fPrice;
	private int fSide, fVolume;
	private String fCondCode;
	private ArrayList<TMarketOperation> fAskArray, fBidArray;
	
	
	/**
	 * Constructor initializes values
	 * @param aExchName name used in exchanges 
	 * @param aPrice price of trade order
	 * @param aVolume size of trade order
	 * @param aSide side of trade. 1 - buy, 2 - sell, 0 - any 
	 * @param aCondCode condition code of each trade
	 */
	public TTradeMarketEvent( String aExchName, double aPrice, int aVolume, int aSide, String aCondCode ) {
		super( aExchName );
		
		fSide = aSide;
		fPrice = aPrice;
		fVolume = aVolume;
		fCondCode = aCondCode;
	}
	
	/**
	 * Constructor initializes values
	 * @param aExchName name used in exchanges 
	 * @param aPrice price of trade order
	 * @param aVolume size of trade order
	 * @param aSide side of trade. 1 - buy, 2 - sell, 0 - any 
	 * @param aCondCode condition code of each trade
	 */
	public TTradeMarketEvent( String aExchName, double aPrice, int aVolume, int aSide, String aCondCode, ArrayList<TMarketOperation> aAskArray, ArrayList<TMarketOperation> aBidArray ) {
		super( aExchName );
		
		fSide = aSide;
		fPrice = aPrice;
		fVolume = aVolume;
		fCondCode = aCondCode;

	}
	
	/**
	 * Method sets  value of different fields of TAbstractInstrument depending on type of this objects
	 * @param aAbstractInstrument TAbstractInstrument, which fields needs to update
	 */
	public void updateInstrument( TAbstractInstrument aAbstractInstrument ) {
		
		aAbstractInstrument.getTick( ).setPrice( fPrice );
		aAbstractInstrument.getTick( ).setVolume( fVolume );
		if( fSide == -1 ) {
			aAbstractInstrument.getTick().setSide( 0 );
			int lSide = GetSide( aAbstractInstrument );
			
			aAbstractInstrument.getTick( ).setSide( lSide );
		} else {
			aAbstractInstrument.getTick( ).setSide( fSide );
		}
		
		
		ArrayList< TAbstractTcpMessaging > lListClients = aAbstractInstrument.getClients( );
		
		
		
		for( int i=0; i<lListClients.size(); i++ ) {
			/*if( lListClients.get( i ).isAvailable() == false ){
				lListClients.remove(i);
				i--;
				continue;
			}*/
			if( lListClients.get( i ) instanceof TTickMessaging ){
				( ( TTickMessaging )( lListClients.get( i ) ) ).compileMessage( aAbstractInstrument );
			}
			
			if( lListClients.get( i ) instanceof TFuturesMessaging ){
				( ( TFuturesMessaging )( lListClients.get( i ) ) ).compileMessage( aAbstractInstrument );
			}
		}
		
		String lQuery = getDBaseQuery( aAbstractInstrument );
		TDBaseQueue lDBaseQueue = TDBaseQueue.getInstance( );
		lDBaseQueue.AddRecord( lQuery );
	}
	
	protected String getDBaseQuery( TAbstractInstrument aAbstractInstrument ){
		
		int lOpenInterest = 0;
		
		if( aAbstractInstrument instanceof TFutures ) {
			lOpenInterest = ( ( TFutures )aAbstractInstrument ).getOpenInterest( );
		}
				//aAbstractInstrument.getLocalName()
		String lQueryToDbase ="";
		if( fCondCode.equals( "fixticks" ) ) {
			lQueryToDbase = "insert into \""+aAbstractInstrument.getLocalName()+"-TICK\" (curr_time, party_side, price,size,open_interest,conditioncode) values ('" 
									+ fBirthDate + "', " + aAbstractInstrument.getTick().getSide() + ", '" + aAbstractInstrument.getTick().getPrice() + "', '"+ aAbstractInstrument.getTick().getVolume() +"', " + lOpenInterest + ", '" + fCondCode +"');\r\n";
		} else {
			lQueryToDbase = "insert into \""+aAbstractInstrument.getLocalName()+"-TICK\" (curr_time, party_side, price,lot_quantity,open_interest,conditioncode) values ('" 
					+ fBirthDate + "', " + aAbstractInstrument.getTick().getSide() + ", '" + aAbstractInstrument.getTick().getPrice() + "', '"+ aAbstractInstrument.getTick().getVolume() +"', " + lOpenInterest + ", '" + fCondCode +"');\r\n";
		}
		return lQueryToDbase;
	}
	
	int GetSide( TAbstractInstrument aAbstractInstrument ) {
		if(	aAbstractInstrument.getTick().getPrice() == aAbstractInstrument.getAsk( ).get( 0 ).getPrevPrice( ) ) {
			aAbstractInstrument.getTick().setSide( 1 );
		}
		else{
			if(aAbstractInstrument.getTick().getPrice() == aAbstractInstrument.getBid( ).get( 0 ).getPrevPrice( ) ) {
				aAbstractInstrument.getTick().setSide( 2 );
			}
			else{
				if(aAbstractInstrument.getTick().getPrice() == aAbstractInstrument.getAsk( ).get( 0 ).getPrice( ) ) {
					aAbstractInstrument.getTick().setSide( 1 );
				}
				else{
					if(aAbstractInstrument.getTick().getPrice() == aAbstractInstrument.getBid( ).get( 0 ).getPrice( ) ) {
						aAbstractInstrument.getTick().setSide( 2 );
					}
					else{
						if(aAbstractInstrument.getTick( ).getSide( ) == 0 ){
							if( aAbstractInstrument.getTick( ).getPrice() > aAbstractInstrument.getTick( ).getPrevPrice( ) ){
								aAbstractInstrument.getTick().setSide( 1 );
							}
							else{
								if( aAbstractInstrument.getTick( ).getPrice() < aAbstractInstrument.getTick( ).getPrevPrice( ) ){
									aAbstractInstrument.getTick().setSide( 2 );
								}
								else{
								if( aAbstractInstrument.getTick( ).getPrice() == aAbstractInstrument.getTick( ).getPrevPrice( ) ){
									aAbstractInstrument.getTick().setSide( aAbstractInstrument.getTick().getPrevSide() );
								}
							}
						}
					}
				}
			}				
		}
		}
	return aAbstractInstrument.getTick().getSide();
	}	
}