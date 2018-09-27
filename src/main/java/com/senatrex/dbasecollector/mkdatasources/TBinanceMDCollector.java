/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

//import com.alk.netwrappers.ClientWebSocketEndpoint;
import com.alk.netwrappers.TNormalWebSocket;
import com.alk.netwrappers.TWebSocketable;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;

//import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.senatrex.dbasecollector.marketevents.TFixMarketEvent;
import com.senatrex.dbasecollector.marketevents.TTradeMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import com.senatrex.dbasecollector.marketinstruments.*;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
/**
 * <p>
 * Class gets market data from quik fix server<br>
 * updated 10 feb. 2018 18:04:28
 * @author Alexander Kumenkov
 *  </p>
 */
public class TBinanceMDCollector extends TAbstractMkDataCollector implements TWebSocketable{

    private long lastUpdateId;
    private HashMap<String,Long> fUpdateIdMap = new HashMap<>();
    private HashMap<String, HashMap<String, NavigableMap<BigDecimal, BigDecimal>>> depthCache = new HashMap<>();

    private TreeMap< String, Double > fMinAmoundMap = new TreeMap<>();
    private Map< String, String > fParametersMap;

    private TNormalWebSocket fWsDepthPoint;

    private boolean fIsClosed;
    private int fMessageNumber, fMarketDepth;
   
    private static final String BIDS  = "BIDS";
    private static final String ASKS  = "ASKS";    
    private String fDepthRespone = "";

    public TBinanceMDCollector( Map< String, String > aParametersMap ) {
        super( );

        fParametersMap = aParametersMap;
        try{
            fMarketDepth = Integer.parseInt( aParametersMap.get( "MarketDepth" ) );
        } catch ( Exception e){
            fMarketDepth = 10;
        }
        fIsClosed = false;
        fMessageNumber = 1;
        fUpdateIdMap  = new HashMap<>();
    }

    /**
    * Initializes the depth cache by using the REST API.
    */
    public void initializeDepthCache( String symbol ) {

        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();

        BinanceApiRestClient client = factory.newRestClient();

        OrderBook orderBook = client.getOrderBook(symbol.toUpperCase(), fMarketDepth);

     //   client.getMyTrades(symbol)
        HashMap<String, NavigableMap<BigDecimal, BigDecimal>> lDepthCache = new HashMap<>();

        long lastUpdateId = orderBook.getLastUpdateId();
        fUpdateIdMap.put( symbol, lastUpdateId );

        NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
        for( OrderBookEntry ask : orderBook.getAsks() ) {
            asks.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
        }
        lDepthCache.put(ASKS, asks);

        NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
        for( OrderBookEntry bid : orderBook.getBids() ) {
            bids.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
        }
        lDepthCache.put(BIDS, bids);

        this.depthCache.put( symbol, lDepthCache );
    }

    @Override
    public void run( ) {

        fDepthRespone = "wss://stream.binance.com:9443/stream?streams=";

        for( String[] lRes:fInstruments ){

            fDepthRespone += String.format("%s@depth/", lRes[1].toLowerCase());
            fDepthRespone += String.format("%s@trade/", lRes[1].toLowerCase());
            initializeDepthCache( lRes[1] );

        }
       // lDepthRespone += String.format("%s@depth/", ("ETHBTC").toLowerCase());
      //  lDepthRespone += String.format("%s@trade/", ("ETHBTC").toLowerCase());
      //  initializeDepthCache( ("ETHBTC") );

        fDepthRespone = fDepthRespone.substring( 0, fDepthRespone.length() - 1 );

        ConnectToServer();

    }
    
    private void ConnectToServer(){
        
        if( fWsDepthPoint != null ){
            fWsDepthPoint.disconnect();
        }
        
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            TAsyncLogQueue.getInstance().AddRecord( ex.getLocalizedMessage() );
        }

        fWsDepthPoint = new TNormalWebSocket( fDepthRespone, this );

    }

    /**
    * Updates an order book (bids or asks) with a delta received from the server.
    *
    * Whenever the qty specified is ZERO, it means the price should was removed from the order book.
    */
    private void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries, List<TOrderBookEntry> orderBookDeltas, NavigableMap<BigDecimal, BigDecimal> aOppositeEntries, String lMainParsing ) {

        for (TOrderBookEntry orderBookDelta : orderBookDeltas) {

            BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
            BigDecimal qty = new BigDecimal(orderBookDelta.getQty());

            if (qty.compareTo(BigDecimal.ZERO) == 0) {
              // qty=0 means remove this level
                if( lastOrderBookEntries.get( price ) == null ){
                    TAsyncLogQueue.getInstance().AddRecord( "cant remove price!no such element!price is " + price );
                }  
                lastOrderBookEntries.remove( price );
            } else {
                lastOrderBookEntries.put(price, qty);

                Set< Entry<BigDecimal, BigDecimal > > Keys = aOppositeEntries.entrySet();

                Entry[] lPrices = Keys.toArray(new Entry[]{});

                for(int i=0; i<lPrices.length; i++){
                     BigDecimal lPrice = (BigDecimal)lPrices[i].getKey();
                    if( lMainParsing.equals( BIDS ) ){
                        if( price.compareTo( lPrice ) > 0 ){
                            aOppositeEntries.remove( lPrice );
                        }
                    }else{
                        if( price.compareTo( lPrice ) < 0 ){
                            aOppositeEntries.remove( lPrice );
                        }
                    }
                }

            }   
        }

    }


    private void sendSnapShot( String aSymbol, NavigableMap<BigDecimal, BigDecimal> aAsks, NavigableMap<BigDecimal, BigDecimal> aBids ) {

        ArrayList<TMarketOperation> lAsks = new ArrayList<>();
        ArrayList<TMarketOperation> lBids = new ArrayList<>();
        Set< Entry<BigDecimal, BigDecimal > > lAskEntries = aAsks.entrySet();

        int lDepth = ( fMarketDepth < lAskEntries.size( ) )?fMarketDepth:( lAskEntries.size( ) );
        Iterator< Entry<BigDecimal, BigDecimal >>lIt=lAskEntries.iterator();
        for ( int i=0; i < lDepth && lIt.hasNext(); i++ ) {  

            Entry<BigDecimal, BigDecimal> lEntry = lIt.next();
            TMarketOperation lMarketOperation = new TMarketOperation();                    
            try{
                lMarketOperation.setPrice( lEntry.getKey().doubleValue() );
                lMarketOperation.setVolume(  (int)( ( lEntry.getValue().doubleValue() )/fMinAmoundMap.get( aSymbol ) )  );
            } catch (Exception e ) {
                lMarketOperation.setPrice( 0.0 );
                lMarketOperation.setVolume( 0 );
            } 
            lAsks.add( lMarketOperation );	
        }

       // Collections.reverse(aBids);  
        Set< Entry<BigDecimal, BigDecimal > > lBidEntries = aBids.entrySet();
        lDepth = ( fMarketDepth < lBidEntries.size( ) )?fMarketDepth:( lBidEntries.size( ) );
        lIt=lBidEntries.iterator();
               
        for ( int i=0; i < lDepth && lIt.hasNext(); i++ ) {     
            Entry<BigDecimal, BigDecimal> lEntry = lIt.next();
            TMarketOperation lMarketOperation = new TMarketOperation();                    
            try{
                lMarketOperation.setPrice( lEntry.getKey().doubleValue() );
                lMarketOperation.setVolume( (int)( ( lEntry.getValue().doubleValue() )/fMinAmoundMap.get( aSymbol ) ) );
            } catch (Exception e ) {
                lMarketOperation.setPrice( 0.0 );
                lMarketOperation.setVolume( 0 );
            }
            lBids.add( lMarketOperation );	
        }
        fMarketEventQueue.AddRecord( new TFixMarketEvent( aSymbol, lAsks, lBids ) );
    }


    /**
     * Method adds instruments for download market data
     * @param aInstruments array of instruments
     */
    @Override
    public void addInstruments( String[][] aInstruments ){
        TLocalMdataTable lLocalMdataTable = TLocalMdataTable.getInstance( );
        if( aInstruments != null ){
                ArrayList<String[]> lInstruments = new ArrayList<>();
                for( String[] lResuilt:aInstruments ){
                    if( lResuilt[ 3 ].equals( "binance" ) ){
                        lLocalMdataTable.addInstrument( new TStock( lResuilt[ 0 ], lResuilt[ 1 ], fMarketDepth ) );
                        lInstruments.add( lResuilt );
                        fMinAmoundMap.put( lResuilt[ 1 ], Double.parseDouble( lResuilt[ 4 ] ) );
                    }
                }

                fInstruments = lInstruments.toArray( new String[ 1 ][ 1 ] );
                int i=0;
        } else {
            throw new NullPointerException();
        }      
    }

    /**
     * @see com.senatrex.dbasecollector.mkdatasources.TAbstractMkDataCollector#isEnabled()
     */
    @Override
    public boolean isEnabled( ) {
        return true;//TODO make connector diagnostic!!!
    }

    @Override
    public void onMessage(String aMessage) {
      
        TAsyncLogQueue.getInstance().AddRecord( aMessage, 2 );
        
        if( aMessage.contains( "Closing!" ) ){
            ConnectToServer();
        }else if( aMessage.contains( "Error!" ) ){
            fWsDepthPoint.disconnect();
        }else if( aMessage.contains( "Opened!" ) ){
            
        }else {
            JSONObject lResponse = new JSONObject( aMessage );
            JSONObject lData = lResponse.getJSONObject("data");
            if(lData.getString("e").equals("depthUpdate")){
                ParseDepthUpdate( lData );
            }
            
            if(lData.getString("e").equals("trade")){
                ParseTradeResponse( lData );
            }
        }
    }

    private void ParseDepthUpdate( JSONObject aResponse ){
        String lRespSymbol = aResponse.getString("s");
        Long lLastUpdateId = fUpdateIdMap.get( lRespSymbol );
      /*  if( ( lLastUpdateId == null || lLastUpdateId != ( aResponse.getLong("U")-1 ) ) ){
            TAsyncLogQueue.getInstance().AddRecord( "missed id!refreshing" );
            initializeDepthCache( lRespSymbol );
        }*/

        
        if ( aResponse.getLong("u") > fUpdateIdMap.get(lRespSymbol) ) {
            
            if( fMinAmoundMap.get( lRespSymbol ) != null ){
                
                JSONArray lBids = aResponse.getJSONArray( "b" );
                JSONArray lAsks = aResponse.getJSONArray( "a" );
                updateOrderBook( depthCache.get(lRespSymbol).get(ASKS), getOrdersList( lAsks ), depthCache.get(lRespSymbol).get(BIDS), ASKS );
                updateOrderBook( depthCache.get(lRespSymbol).get(BIDS), getOrdersList( lBids ), depthCache.get(lRespSymbol).get(ASKS), BIDS );

                sendSnapShot( lRespSymbol,  depthCache.get(lRespSymbol).get(ASKS), depthCache.get(lRespSymbol).get(BIDS) );
                
            } else {
                TAsyncLogQueue.getInstance().AddRecord( "unknown symbol!" );
            }
            
        }
    }
    
    private ArrayList<TOrderBookEntry> getOrdersList( JSONArray aJsonArray ){
        
        int lGlassLength = aJsonArray.length();
        ArrayList<TOrderBookEntry> oRes = new ArrayList<>();
        
        for( int i=0; i < lGlassLength; i++ ){
            JSONArray lOrdBookJson = aJsonArray.getJSONArray( i );
            String lPrice = lOrdBookJson.getString( 0 ); 
            String lQty = lOrdBookJson.getString( 1 );
            oRes.add( new TOrderBookEntry( lPrice, lQty ) );
        }
        
        return oRes;
    }    
    
    private void ParseTradeResponse( JSONObject aResponse ){
        String lRespSymbol = aResponse.getString("s");
        int lSide = -1;
        double lPrice = 0.0;
        int lVolume = 0;

        if( fMinAmoundMap.get( lRespSymbol ) != null ){
            try{
                lSide = aResponse.getBoolean("m")?(1):(-1);
                lPrice = Double.parseDouble( aResponse.getString("p") );
                lVolume = (int)( Double.parseDouble( aResponse.getString("q") )/fMinAmoundMap.get( lRespSymbol ) );
            } catch (Exception e ) {
                lPrice = 0.0;
                lVolume = 0;
            } 
            fMarketEventQueue.AddRecord(new TTradeMarketEvent(lRespSymbol, lPrice, lVolume, lSide, "binance"));
            int t=0;
        }
    }
    
}
