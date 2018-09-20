/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;


import com.senatrex.dbasecollector.marketevents.TFixMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import com.senatrex.tcpsample.ptcpclasses.TClientWebSocketEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import org.json.*;

/**
 *
 * @author wellington
 */

public class TBitmexCollector extends TAbstractMkDataCollector implements MessageHandler.Whole<String>{

    private Map< String, Double > fMinAmoundMap = new TreeMap<>();
    
    final static String TABLE = "table",  
                        SYMBOL = "symbol",
                        DATA = "data",
                        ORDERBOOK10 = "orderBook10",
                        ASKS = "asks",
                        BIDS = "bids";
    
    String fResuilt = "{\"table\":\"orderBook10\",\"action\":\"update\",\"data\":[{\"symbol\":\"XBTUSD\",\"bids\":[[11012.5,41002],[11012,1651],[11011.5,2701],[11011,5000],[11010.5,10000],[11010,208380],[11009,5000],[11008.5,20100],[11008,15000],[11007.5,40]],\"asks\":[[11013,31452],[11013.5,50000],[11014,4200],[11014.5,1768],[11015,1766],[11015.5,800],[11016,21351],[11016.5,7129],[11018,10000],[11019,2353]],\"timestamp\":\"2018-02-19T10:19:35.140Z\"}]}";
   
    TClientWebSocketEndpoint fSocket;
    
    @Override
    public void run() {
        fSocket = new TClientWebSocketEndpoint(); 
  //      Json lConfig = Json.createReader();
        fSocket.initialize( "wss://www.bitmex.com/realtime", this );
        fSocket.sendText( "{\"op\": \"subscribe\", \"args\": [\"orderBook10:XBTUSD\",\"orderBook10:BCHH18\"]}" );
        int t=0;
    }

    public static void DoFunction( String lNothing ){
        System.out.println(lNothing);
    }
    
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    @OnMessage
    public void onMessage(String aIncomeMessage) {

       JSONObject lObject =  new JSONObject( aIncomeMessage );
        if( lObject.has( TABLE ) ){
            if( lObject.get( TABLE ).equals( ORDERBOOK10 ) ){
                JSONArray lArr = lObject.getJSONArray( DATA );
                for(int j=0; j<lArr.length(); j++){
                    try{
                        JSONObject lTestData = lArr.getJSONObject( j );
                        String lSymbol = lTestData.get( SYMBOL ).toString();

                        ArrayList< TMarketOperation > lAsks = getOfferArray( lTestData.getJSONArray( ASKS ), 0.1 );
                        ArrayList< TMarketOperation > lBids = getOfferArray( lTestData.getJSONArray( BIDS ), 1.0 );

                        fMarketEventQueue.AddRecord( new TFixMarketEvent( lSymbol, lAsks, lBids ) );
                    }catch( Exception e ){
                        TAsyncLogQueue.getInstance().AddRecord( e.getMessage() + aIncomeMessage  );
                    }
                }
            }
        }
        System.out.println( aIncomeMessage );
    }

    private ArrayList< TMarketOperation > getOfferArray( JSONArray aOffers, double aMinAmound ){
        ArrayList< TMarketOperation > oResuilt = new ArrayList<>();
        int lLength = aOffers.length();
        for( int i=0; i < lLength; i++ ){
            JSONArray lArray = aOffers.getJSONArray( i );
            double lPrice = lArray.getDouble( 0 );
            int lVolume = ( int )( lArray.getDouble( 1 ) / aMinAmound );
            oResuilt.add( new TMarketOperation( lPrice, lVolume ) );
        }
        return oResuilt;
    }   
    
}
