/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

import com.alk.netwrappers.TNormalWebSocket;
import com.alk.netwrappers.TWebSocketable;
import com.senatrex.dbasecollector.marketinstruments.TStock;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;

/**
 *
 * @author kan
 */
public class TPoloneixMDCollector extends TAbstractMkDataCollector implements TWebSocketable {
    
    private String fServer = "wss://api2.poloniex.com/";
    private TreeMap< String, Double > fMinAmoundMap = new TreeMap<>();
    private TNormalWebSocket fWS_MD;
    private Thread fParserThread;
    private ArrayList<String> fEvents = new ArrayList<String>();
    private Object fWaitObject = new Object();
    
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
                String lReq = "{\"command\": \"subscribe\", \"channel\": \""+lRes[1]+"\"}";
                TAsyncLogQueue.getInstance( ).AddRecord( "Sent to poloneix: " + lReq );
                fWS_MD.sendMessage( 
                    lReq
                );
            }              
        }
        
        fParserThread = new Thread(new Runnable(){
            @Override
            public void run() {
                while( fIsClosed == false ){
                    try {							
                        synchronized( fWaitObject ) {
                            fWaitObject.wait( );
                        }
                    } catch ( InterruptedException e ) {}
                    
                    String lMessage;
                    
                    while( !fEvents.isEmpty() ){
                        synchronized( fWaitObject ) {
                            lMessage = fEvents.get(0);
                            fEvents.remove(0);
                        }
                        
                        try{
                            JSONArray lResponseArray = new JSONArray( lMessage );
                            
                            
                            
                        //TODO: parser    
                            
                            //System.out.println(lResponseArray);
                        }catch(Exception e){
                            TAsyncLogQueue.getInstance().AddRecord( "bad Response: "+lMessage+' '+e.getLocalizedMessage() );
                        }
                    }
                }
            }
        });
        
        fParserThread.start();
        
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
                        //fMinAmoundMap.put( lResuilt[ 1 ], Double.parseDouble( lResuilt[ 4 ] ) );
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
    public void onMessage(String aMessage){
        AddMessageToParse( aMessage );
    }
    
    private void ConnectToServer(){
        if( fWS_MD != null ){
            fWS_MD.disconnect();
        }
        
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            TAsyncLogQueue.getInstance().AddRecord( ex.getLocalizedMessage() );
        }

        fWS_MD = new TNormalWebSocket( fServer, this );
    }
    
    private void AddMessageToParse( String aMessage ){
        synchronized( fWaitObject ) {	
            fEvents.add( aMessage );
            fWaitObject.notify( );
        }
    }
}
