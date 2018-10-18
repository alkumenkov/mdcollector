/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

//import com.alk.netwrappers.ClientWebSocketEndpoint;
import com.alk.netwrappers.TNormalWebSocket;
import com.alk.netwrappers.TWebSocketable;
//import com.binance.api.client.BinanceApiClientFactory;
//import com.binance.api.client.BinanceApiRestClient;

import com.senatrex.dbasecollector.marketevents.TFixMarketEvent;
import com.senatrex.dbasecollector.marketevents.TTradeMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.marketinstruments.TStock;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
//import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author wellington
 */
public class TOkExMDCollector extends TAbstractMkDataCollector implements TWebSocketable{

    private long fLastUpdateId;
    private Map<String,Long> fUpdateIdMap = new HashMap<>();
    private Map<String, HashMap<String, NavigableMap<BigDecimal, BigDecimal>>> fDepthCache = new HashMap<>();

    private Map< String, Double > fMinAmoundMap = new TreeMap<>();
    private Map< String, String > fParametersMap;

    private TNormalWebSocket fWsDepthPoint;
    Thread fPingThread;
    private boolean fIsOpened;
    private boolean fIsClosed;
    private int fMessageNumber;
   
    private static final String BIDS  = "BIDS";
    private static final String ASKS  = "ASKS";
    
    private String fDepthRespone = "";

    public TOkExMDCollector( Map< String, String > aParametersMap ) {
        super( );

        fParametersMap = aParametersMap;
        try{
            fMarketDepth = Integer.parseInt( aParametersMap.get( "MarketDepth" ) );
        } catch ( Exception e ){
            fMarketDepth = 10;
        }
        fIsClosed = false;
        fIsOpened = false;
        fMessageNumber = 1;
        fUpdateIdMap  = new HashMap<>();
    }

    /**
    * Initializes the depth cache by using the REST API.
    */
    public void initializeDepthCache( String aSymbol ) {

     //   client.getMyTrades(symbol)
        HashMap<String, NavigableMap<BigDecimal, BigDecimal>> lDepthCache = new HashMap<>();
     //   fUpdateIdMap.put( aSymbol, fLastUpdateId );
        NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>();        
        lDepthCache.put(ASKS, asks);
        NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());  
        lDepthCache.put(BIDS, bids);

        this.fDepthCache.put( aSymbol, lDepthCache );
    }

    @Override
    public void run( ) {

        fDepthRespone = "wss://real.okex.com:10441/websocket";
     
       // lDepthRespone += String.format("%s@depth/", ("ETHBTC").toLowerCase());
      //  lDepthRespone += String.format("%s@trade/", ("ETHBTC").toLowerCase());
      //  initializeDepthCache( ("ETHBTC") );
        String lMessageToSend="";
        ConnectToServer();
        if( fDepthRespone != null && fInstruments!=null ){            
            for( String[] lRes:fInstruments ){
                initializeDepthCache( lRes[1] );
                String lIsin = lRes[1];
                
                fWsDepthPoint.sendMessage( "[{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_depth'},{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_deals'}]");
 
            //    System.out.println("[{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_depth'},{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_deals'}]");
                
              //  fWsDepthPoint.sendMessage( "[{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_deals'}]");
    
            }              
        }
        
    //      fWsDepthPoint.sendMessage( lMessageToSend);
    //      lMessageToSend = lMessageToSend.substring(0, lMessageToSend.length()-1);
    //      lMessageToSend+="]";
    //     fWsDepthPoint.sendMessage( lMessageToSend); 
    //   fWsDepthPoint.sendMessage( "{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_depth'}" ); 
    //   fWsDepthPoint.sendMessage( "{'event':'addChannel','channel':'ok_sub_spot_"+lIsin+"_deals'}");
        fPingThread = new Thread(new Runnable(){
            @Override
            public void run() {
                while( fIsClosed == false ){
                    
                    try {
                        Thread.sleep(1000*30);
                        
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TOkExMDCollector.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    fWsDepthPoint.sendMessage("{'event':'ping'}");
                    Log("okex ping!");
                }
            }
            
        });
        
        fPingThread.start();
        
    }
    
    private void Reconnect(){
        Log( "reconnecting" );
        fDepthCache = new HashMap<>();
        run();
    }
    
    private void ConnectToServer(){
        
        if( fWsDepthPoint != null ){
            fWsDepthPoint.disconnect();
        }
        
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            Log( ex.getLocalizedMessage() );
        }

        fWsDepthPoint = new TNormalWebSocket( fDepthRespone, this );
        while( !fWsDepthPoint.isConnected( ) ){
            try {
                Thread.sleep( fConnectionTryingDelay );
            } catch (InterruptedException ex) {
                
            }
            if( fConnectionTryingDelay < 60000L ){
                fConnectionTryingDelay *= 2;
            }  
            fWsDepthPoint = new TNormalWebSocket( fDepthRespone, this );
        }
        fConnectionTryingDelay = 1000L;
        
    }
    
    long fConnectionTryingDelay=1000L;
    /**
    * Updates an order book (bids or asks) with a delta received from the server.
    * Whenever the qty specified is ZERO, it means the price should was removed from the order book.
    */
    private void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> aLastOrderBookEntries, List<TOrderBookEntry> aOrderBookDeltas, NavigableMap<BigDecimal, BigDecimal> aOppositeEntries, String aMainParsing ) {

        for (TOrderBookEntry orderBookDelta : aOrderBookDeltas) {

            BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
            BigDecimal qty = new BigDecimal(orderBookDelta.getQty());

            if (qty.compareTo(BigDecimal.ZERO) == 0) {
              // qty=0 means remove this level
                if( aLastOrderBookEntries.get( price ) == null ){
                    Log( "cant remove price!no such element!price is " + price );
                }  
                aLastOrderBookEntries.remove( price );
            } else {
                aLastOrderBookEntries.put(price, qty);

                Set< Map.Entry<BigDecimal, BigDecimal > > Keys = aOppositeEntries.entrySet();

                Map.Entry[] lPrices = Keys.toArray(new Map.Entry[]{});

                for(int i=0; i<lPrices.length; i++){
                     BigDecimal lPrice = (BigDecimal)lPrices[i].getKey();
                    if( aMainParsing.equals( BIDS ) ){
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
        Set< Map.Entry<BigDecimal, BigDecimal > > lAskEntries = aAsks.entrySet();

        int lDepth = ( fMarketDepth < lAskEntries.size( ) )?fMarketDepth:( lAskEntries.size( ) );
        Iterator< Map.Entry<BigDecimal, BigDecimal >>lIt=lAskEntries.iterator();
        for ( int i=0; i < lDepth && lIt.hasNext(); i++ ) {  

            Map.Entry<BigDecimal, BigDecimal> lEntry = lIt.next();
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
        Set< Map.Entry<BigDecimal, BigDecimal > > lBidEntries = aBids.entrySet();
        lDepth = ( fMarketDepth < lBidEntries.size( ) )?fMarketDepth:( lBidEntries.size( ) );
        lIt=lBidEntries.iterator();
               
        for ( int i=0; i < lDepth && lIt.hasNext(); i++ ) {     
            Map.Entry<BigDecimal, BigDecimal> lEntry = lIt.next();
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
                    if( lResuilt[ 3 ].equals( "okex" ) ){
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
      //  System.out.println(aMessage);
        Log( aMessage, 2 );
        
        if( aMessage.contains( "Closing!" ) ){
            fIsOpened = false;
            Reconnect();
        }else if( aMessage.contains( "Error!" ) ){
            Reconnect();
        }else if( aMessage.contains( "Opened!" ) ){
            fIsOpened = true;
        }else{

            try{
                JSONArray lResponseArray = new JSONArray( aMessage );
                for( int i=0; i<lResponseArray.length(); i++ ){
                    JSONObject lResponse = lResponseArray.getJSONObject(i);

                    if( lResponse.getString( "channel" ).contains( "_depth" ) ){
                        String lChanel = lResponse.getString( "channel" );
                        String lSymbol = lChanel.substring(("ok_sub_spot_").length(), lChanel.length()-6);
                        JSONObject lData = lResponse.getJSONObject("data");
                        ParseDepthUpdate( lData, lSymbol );
                    }

                    if( lResponse.getString( "channel" ).contains( "_deals" ) ){
                        String lChanel = lResponse.getString( "channel" );
                        String lSymbol = lChanel.substring(("ok_sub_spot_").length(), lChanel.length()-6);
                        JSONArray lData = lResponse.getJSONArray("data");
                        ParseTradeResponse( lData, lSymbol );
                    } 
                }
            }catch(Exception e){
                Log( "bad Response!"+aMessage );
            }
        }
    }

    private void ParseDepthUpdate( JSONObject aResponse, String aSymbol ){
        String lRespSymbol = aSymbol;
     
        if( fMinAmoundMap.get( lRespSymbol ) != null ){

            if(aResponse.has("asks")){
                JSONArray lAsks = aResponse.getJSONArray( "asks" );
                updateOrderBook( fDepthCache.get(lRespSymbol).get(ASKS), getOrdersList( lAsks ), fDepthCache.get(lRespSymbol).get(BIDS), ASKS );
            }
            
            if(aResponse.has("bids")){
                JSONArray lBids = aResponse.getJSONArray( "bids" );
                updateOrderBook( fDepthCache.get(lRespSymbol).get(BIDS), getOrdersList( lBids ), fDepthCache.get(lRespSymbol).get(ASKS), BIDS );
            }
            sendSnapShot( lRespSymbol, fDepthCache.get(lRespSymbol).get(ASKS), fDepthCache.get(lRespSymbol).get(BIDS) );

        } else {
            Log( "unknown symbol!" );
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
    
    private void ParseTradeResponse( JSONArray aResponse, String aSymbol ){

        if( fMinAmoundMap.get( aSymbol ) != null ){
            int lLength = aResponse.length();
            for(int i=0; i<lLength; i++){
                JSONArray lResponse = aResponse.getJSONArray(i);
                try{
                    int lSide = lResponse.getString( 4 ).equals("bid")?(-1):(1);
                    double lPrice = Double.parseDouble( lResponse.getString( 1 ));
                    int lVolume = (int)( Double.parseDouble( lResponse.getString( 2 ) )/fMinAmoundMap.get( aSymbol ) );
                    
                    fMarketEventQueue.AddRecord( new TTradeMarketEvent(aSymbol, lPrice, lVolume, lSide, "okex") );
                
                } catch (Exception e ) {
                    Log( "Bad trade: " +  aResponse + "\t" + e.getLocalizedMessage());
                }
            }
            int t=0;
        }
    }
    
}
