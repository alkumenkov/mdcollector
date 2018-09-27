/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

import com.alk.netwrappers.TNormalWebSocket;
import com.alk.netwrappers.TWebSocketable;
import com.senatrex.dbasecollector.marketevents.TFixMarketEvent;
import com.senatrex.dbasecollector.marketevents.TTradeMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.marketinstruments.TStock;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author kan
 */
public class TPoloneixMDCollector extends TAbstractMkDataCollector implements TWebSocketable {
    
    private String fServer = "wss://api2.poloniex.com/";
    private TreeMap< String, Double > fMinAmoundMap = new TreeMap<>();
    private TreeMap< Integer, String > fTickerMap = new TreeMap<>();
    
    private TreeMap< String, TIncremetOrderBook> fOrderBooks = new TreeMap<>();
    
    private TNormalWebSocket fWS_MD = null;
    private Thread fParserThread = null;
    private Thread fWatchDogThread = null;
    private ArrayList<String> fEvents = new ArrayList<String>();
    private Object fWaitObject = new Object();
    private boolean fHadNewData = true;
    private long fHeartBeatDelay = 5000;
    
    public TPoloneixMDCollector( Map< String, String > aParametersMap ) {
        super( );
        
        if( aParametersMap.containsKey( "Server" ) ){
            fServer = aParametersMap.get( "Server" );
        }
        TAsyncLogQueue.getInstance( ).AddRecord("Poloneix server: " + fServer);
        
        try{
            fMarketDepth = Integer.parseInt( aParametersMap.get( "MarketDepth" ) );
        } catch ( Exception e ){
            TAsyncLogQueue.getInstance( ).AddRecord( "error initializing MarketDepth into TPoloneixMDCollector! " + e.getLocalizedMessage(), 0 );
            fMarketDepth = 10;
        }
    }
    
    @Override
    public void run( ){
        ConnectToServer();
        if( fWS_MD != null && fInstruments!=null ){
            for( String[] lRes:fInstruments ){
//                initializeDepthCache( lRes[1] );
                String lReq = "{\"command\": \"subscribe\", \"channel\": \""+lRes[1]+"\" }";//, \"depth\": 10
                TAsyncLogQueue.getInstance( ).AddRecord( "Sent to poloneix: " + lReq );
                fWS_MD.sendMessage( 
                    lReq
                );
            }
        }
        
        TAsyncLogQueue.getInstance( ).AddRecord( "Poloneix parser-thread running..." );
        fParserThread = new Thread(new Runnable(){
            @Override
            public void run() {
                while( fIsClosed == false ){
                    try {							
                        synchronized( fWaitObject ) {
                            fWaitObject.wait( );
                        }
                    } catch ( InterruptedException e ) {}
                    
                    fHadNewData = true;
                    
                    String lMessage;
                    
                    while( !fEvents.isEmpty() ){
                        synchronized( fWaitObject ) {
                            lMessage = fEvents.get(0);
                            fEvents.remove(0);
                        }
                        
                        try{
                            JSONArray lResponseArray = new JSONArray( lMessage );
   
                            Integer lChannelID = lResponseArray.getInt(0);
                            if( lChannelID < 1000 ){
                                JSONArray lDataArray = lResponseArray.getJSONArray(2);
                                //System.out.println( lChannelID.toString() + " array size:" + String.valueOf( lDataArray.length() ) );
                                
                                String lSymbol = fTickerMap.get( lChannelID );
                                Double lMinAmound = 1.0;
                                
                                if( lSymbol != null && fMinAmoundMap.containsKey(lSymbol) ) {
                                   lMinAmound = fMinAmoundMap.get( lSymbol );
                                }
                                
                                boolean lSendMD = false;

                                for( int i = 0; i < lDataArray.length(); i++ ) {
                                    JSONArray lRowArray = lDataArray.getJSONArray(i);
//                                    System.out.println( "\t" + lRowArray );
                                    //["o",1,"232.51733620","0.09066897"]
                                    int lType = lRowArray.getString(0).charAt(0);
                                    
                                    switch( lType ) {
                                        ///////////////
                                        case 111:{//o
                                            //System.out.println( "OB" );

                                            int lSide = lRowArray.getInt(1);
                                            BigDecimal lPrice = new BigDecimal( lRowArray.getString(2) );
                                            String lQtyRaw = lRowArray.getString(3);
                                            BigDecimal lQty = new BigDecimal( Double.parseDouble( lQtyRaw ) / lMinAmound );
                                            
//                                            System.out.println( lQtyRaw + "\t" + lQty );
                                            
                                            TIncremetOrderBook lOB = fOrderBooks.get(lSymbol);
                                            
                                            if( lSide == 0 ){
                                                if( lQty.compareTo(BigDecimal.ZERO) == 0 ) {
                                                    lSendMD = lOB.Delete_Ask( lPrice ) | lSendMD;
                                                } else {
                                                    lSendMD = lOB.Add_Ask(lPrice, lQty.setScale(0, RoundingMode.HALF_UP)) | lSendMD;
                                                }
                                                
                                            } else {
                                                if( lQty.compareTo(BigDecimal.ZERO) == 0 ) {
                                                    lSendMD = lOB.Delete_Bid( lPrice ) | lSendMD ;
                                                } else {
                                                    lSendMD = lOB.Add_Bid(lPrice, lQty.setScale(0, RoundingMode.HALF_UP)) | lSendMD;
                                                }                                                
                                            }
                                            
                                        } break;
                                        ///////////////
                                        case 116://t
                                            
                                            if( lSymbol != null ) {
                                                //System.out.println( "tick" );
                                                //["t","10387283",1,"233.53784533","0.01279508",1537778985]
                                                //["t", "<trade id>", <1 for buy 0 for sell>, "<size>", "<price>", <timestamp>]
                                                Double lPrice = Double.parseDouble( lRowArray.getString(3) );
                                                int lVolume = (int)( Double.parseDouble( lRowArray.getString(4) ) / lMinAmound );
                                                int lSide = (lRowArray.getInt( 2 )==1)?(-1):(1);

                                                fMarketEventQueue.AddRecord( new TTradeMarketEvent( lSymbol, lPrice, lVolume, lSide, "poloneix") );
                                            } else {
                                                TAsyncLogQueue.getInstance( ).AddRecord( "Unknown message ticker id: " + lChannelID );
                                            }
                                            
                                            break;
                                        ///////////////
                                        case 105://i
//                                            System.out.println( "info" );
                                            JSONObject lInfo = lRowArray.getJSONObject(1);
                                            String lNewSymbol = lInfo.getString("currencyPair");
                                            
                                            lMinAmound = fMinAmoundMap.get( lNewSymbol );
                                            
                                            fTickerMap.put( lChannelID, lNewSymbol );
                                            TIncremetOrderBook lOB = fOrderBooks.get(lNewSymbol);
                                            
                                            JSONObject lAsks_json = lInfo.getJSONArray("orderBook").getJSONObject(0);
                                            Iterator<String> lkeysAsk = lAsks_json.keys();
                                            while (lkeysAsk.hasNext()) {
                                                String lPrice = lkeysAsk.next();
                                                String lQtyRaw = lAsks_json.getString(lPrice);
                                                BigDecimal lQty = new BigDecimal( Double.parseDouble( lQtyRaw ) / lMinAmound );
                                                
                                                assert lQtyRaw != "0.00000000";
                                                
                                                lOB.Add_Ask( new BigDecimal(lPrice), lQty.setScale(0, RoundingMode.HALF_UP) );
                                            }
                                            
                                            JSONObject lBids_json = lInfo.getJSONArray("orderBook").getJSONObject(1);
                                            Iterator<String> lkeysBid = lBids_json.keys();
                                            while (lkeysBid.hasNext()) {
                                                String lPrice = lkeysBid.next();
                                                String lQtyRaw = lBids_json.getString(lPrice);
                                                BigDecimal lQty = new BigDecimal( Double.parseDouble( lQtyRaw ) / lMinAmound );
                                                
                                                assert lQtyRaw != "0.00000000";
                                                
                                                lOB.Add_Bid( new BigDecimal(lPrice), lQty.setScale(0, RoundingMode.HALF_UP) );
                                            }
                                            
                                            lSendMD = true;
                                            lSymbol = lNewSymbol;

                                            break;
                                        ///////////////
                                        default:
                                            TAsyncLogQueue.getInstance( ).AddRecord( "Unknown message type: " + lRowArray );
                                            break;
                                    }
                                }
                                
                                if( lSendMD ) {
                                    TIncremetOrderBook lOB = fOrderBooks.get(lSymbol);
                                    
                                    ArrayList<TMarketOperation> lAsks = lOB.getAsk();
                                    ArrayList<TMarketOperation> lBids = lOB.getBig();
                                    
                                    fMarketEventQueue.AddRecord( new TFixMarketEvent( lSymbol, lAsks, lBids ) );
                                }
                                
                            } else if( lChannelID == 1010 ) {
                                //System.out.println( "ping" );
                            }
                            
                        } catch(Exception e) {
                            TAsyncLogQueue.getInstance().AddRecord( "bad Response: "+lMessage+' '+e.getLocalizedMessage() );
                        }
                    }
                }
            }
        });
        fParserThread.start();
        
        TAsyncLogQueue.getInstance( ).AddRecord( "Poloneix WatchDog-thread running..." );
        fWatchDogThread = new Thread(new Runnable(){
            
            private long GetRoundedTime(){
                long timeMs = System.currentTimeMillis();
                return Math.round( (double)( (double)timeMs/(double)(fHeartBeatDelay) ) ) * (fHeartBeatDelay);
            }
            
            long fLastTime = GetRoundedTime();
            
            @Override
            public void run() {
                
                while( fIsClosed == false ) {
                
                    if( fLastTime != GetRoundedTime() ) {
                        if( !fHadNewData ){
                            ConnectToServer();
                        }
                        
                        fLastTime = GetRoundedTime();
                        fHadNewData = false;
                    }
                    
                    try { Thread.sleep( 1000 );} catch ( InterruptedException ex ) {;}
                }
            }
        });
        fWatchDogThread.start();
    }
    
    @Override
    public void addInstruments( String[][] aInstruments ){
        TLocalMdataTable lLocalMdataTable = TLocalMdataTable.getInstance( );
        if( aInstruments != null ){
            ArrayList<String[]> lInstruments = new ArrayList<>();
            for( String[] lResuilt:aInstruments ){
                if( lResuilt[ 3 ].equals( "poloneix" ) ){
                    lLocalMdataTable.addInstrument( new TStock( lResuilt[ 0 ], lResuilt[ 1 ], fMarketDepth ) );
                    lInstruments.add( lResuilt );
                    fMinAmoundMap.put( lResuilt[ 1 ], Double.parseDouble( lResuilt[ 4 ] ) );
                    fOrderBooks.put( lResuilt[ 1 ], new TIncremetOrderBook( fMarketDepth ) );
                }
            }

            fInstruments = lInstruments.toArray( new String[ 1 ][ 1 ] );
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
    public void onMessage( String aMessage ) {
        AddMessageToParse( aMessage );
    }
    
    private void ConnectToServer() {
        if( fWS_MD != null ) {
            fWS_MD.disconnect();
            TAsyncLogQueue.getInstance( ).AddRecord( "Poloneix diconnected." );
        }
        
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            TAsyncLogQueue.getInstance().AddRecord( ex.getLocalizedMessage() );
        }

        TAsyncLogQueue.getInstance( ).AddRecord( "Poloneix connecting..." );
        fWS_MD = new TNormalWebSocket( fServer, this );
    }
    
    private void AddMessageToParse( final String aMessage ){
        synchronized( fWaitObject ) {	
            fEvents.add( aMessage );
            fWaitObject.notify( );
        }
    }
}
