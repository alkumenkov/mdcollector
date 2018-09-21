package com.senatrex.dbasecollector.pmainpac;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Queue;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ini4j.Ini;

import com.senatrex.dbasecollector.instrumentsdealers.*;
import com.senatrex.dbasecollector.mkdatasources.TAbstractMkDataCollector;
import com.senatrex.dbasecollector.mkdatasources.TBinanceMDCollector;
import com.senatrex.dbasecollector.mkdatasources.TBitmexCollector;
import com.senatrex.dbasecollector.mkdatasources.TBlCollector;
import com.senatrex.dbasecollector.mkdatasources.TPoloneixMDCollector;

import com.senatrex.dbasecollector.mkdatasources.TFixCollector;
import com.senatrex.dbasecollector.mkdatasources.TOkExMDCollector;
import com.senatrex.dbasecollector.mkdatasources.TTestingCollector;
import com.senatrex.dbasecollector.mkdatasources.TTickFixCollector;
import com.senatrex.dbasecollector.ptimeutilities.TTimeUtilities;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import com.senatrex.dbasecollector.queues.TDBaseQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Main Class!
 *
 */


public class App 
{
    public int fVariable;
    
    public static void main( String[] args ) throws Throwable {
    	Ini lIniObject = new Ini( );
        Map< String, String > lDBaseParams = null;
        
        lDBaseParams = new TreeMap<>();
     
        Map< String, String > lGeneralParams = null;
        String lCollectorNamesStr ="";
        
    	try {
    		
            //TAlarmSignal lTAlarmSignal = new TAlarmSignal();
            TAsyncLogQueue.getInstance().AddRecord( "Started!", 0 );

            lIniObject.load( new FileReader( new File( "my.ini") ) );

            lDBaseParams = lIniObject.get( "sql" );
            TAsyncLogQueue.getInstance( ).AddRecord( "ini parameters loaded!", 0 );
            TAsyncLogQueue.getInstance( ).AddRecord( "initializing dbase queue", 0 );
    
            if( lDBaseParams.containsKey( "enabled" ) ){
                boolean lBaseEnabled = Boolean.parseBoolean( lDBaseParams.get( "enabled" ) );
                TDBaseQueue.getInstance().initDBase( "jdbc:postgresql://" + lDBaseParams.get( "host" ) + ":" + lDBaseParams.get( "port" ) + "/" + lDBaseParams.get( "base" ), 
                            lDBaseParams.get( "user" ), lDBaseParams.get( "pass" ), lBaseEnabled );
            } else {
                TDBaseQueue.getInstance().initDBase( "jdbc:postgresql://" + lDBaseParams.get( "host" ) + ":" + lDBaseParams.get( "port" ) + "/" + lDBaseParams.get( "base" ), 
                            lDBaseParams.get( "user" ), lDBaseParams.get( "pass" ) );
            }
            TDBaseQueue.getInstance().setBuffParams( Integer.parseInt( lDBaseParams.get( "DBBUF" ) ) );
            TAsyncLogQueue.getInstance( ).AddRecord( "initialized", 0 );

            lGeneralParams = lIniObject.get( "general" );
            if( lGeneralParams.containsKey( "loglevel" ) ){
                int lLogLevel = Integer.parseInt( lGeneralParams.get( "loglevel" ) );
                TAsyncLogQueue.getInstance().setLogLevel( lLogLevel );
            }

            lCollectorNamesStr = lGeneralParams.get( "collector" );
            TAsyncLogQueue.getInstance( ).AddRecord( "setting timezone...", 0 );

    //	String lTimezoneKey="";
            String lTimezoneKey = lGeneralParams.get( "localtime" );
            if( lTimezoneKey != null && lTimezoneKey.equals( "UTC" ) ) {
                    TTimeUtilities.fSimpleTimeZone =  new SimpleTimeZone( SimpleTimeZone.UTC_TIME, "UTC" );
                    TAsyncLogQueue.getInstance( ).AddRecord( lTimezoneKey, 0 );
            } else {
                    TTimeUtilities.fSimpleTimeZone = TimeZone.getDefault();
                    TAsyncLogQueue.getInstance( ).AddRecord( "default", 0 );
            }
            
        } catch( Exception e ) {
            TAsyncLogQueue.getInstance().AddRecord( e.getClass( ).getName( ) + ": " + e.getMessage( ), 0 );
    	}
        
        String[] lColectorNames = lCollectorNamesStr.split(",");

        if( lDBaseParams != null && lGeneralParams != null && lColectorNames.length>0 ){
        	
            TAsyncLogQueue.getInstance( ).AddRecord( "starting main processes", 0 );

            List< TAbstractMkDataCollector > lTAbstractCollectors = new ArrayList<>();

            for ( String lCollectorName:lColectorNames ){

                TAbstractMkDataCollector lTAbstractCollector = null;

                if( lCollectorName.equals( "binance" ) ) {
                    Map< String, String > lCollectorParams = lIniObject.get( "binance" );
                    String[ ][ ]lInstruments = ( new TImportBcsDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass") ) ).initializeSystem( );
                    lTAbstractCollector = new TBinanceMDCollector( lCollectorParams );
                    lTAbstractCollector.addInstruments( lInstruments );      
                }

                if( lCollectorName.equals( "okex" ) ) {
                    Map< String, String > lCollectorParams = lIniObject.get( "okex" );
                    String[ ][ ]lInstruments = ( new TImportBcsDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass") ) ).initializeSystem( );
                    lTAbstractCollector = new TOkExMDCollector( lCollectorParams );
                    lTAbstractCollector.addInstruments( lInstruments );      
                }

                if( lCollectorName.equals( "poloneix" ) ) {
                    Map< String, String > lCollectorParams = lIniObject.get( "poloneix" );
                    if( lCollectorParams == null ){
                        TAsyncLogQueue.getInstance( ).AddRecord( "Section poloneix is empty", 0 );
                    } else {
                        String[ ][ ]lInstruments = ( new TImportBcsDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass") ) ).initializeSystem( );
                        lTAbstractCollector = new TPoloneixMDCollector( lCollectorParams );
                        lTAbstractCollector.addInstruments( lInstruments );                            
                    }
                }

                if( lCollectorName.equals( "fixd" ) ) {
                    Map< String, String > lCollectorParams = lIniObject.get( "fixd" );
                    String[ ][ ]lInstruments = ( new TImportBcsDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass") ) ).initializeSystem( );
                    lTAbstractCollector = new TFixCollector( lCollectorParams );
                    lTAbstractCollector.addInstruments( lInstruments );      
                }

                if( lCollectorName.equals( "fixtick" ) ) {
                    Map< String, String > lCollectorParams = lIniObject.get( "fixtick" );
                    String[ ][ ]lInstruments = ( new TImportUsaDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass"), Integer.parseInt(lCollectorParams.get("MarketDepth") ) ) ).initializeSystem( );
                    lTAbstractCollector = new TTickFixCollector( lCollectorParams );
                    lTAbstractCollector.addInstruments( lInstruments );
                }

                if( lCollectorName.equalsIgnoreCase( "bloom" ) ) {
                    String[ ][ ]lInstruments = ( new TBloombergDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass") ) ).initializeSystem( );
                    lTAbstractCollector = new TBlCollector( );
                    //lTAbstractCollector = new TTestingCollector( );
                    lTAbstractCollector.addInstruments( lInstruments );	 
                    TAsyncLogQueue.getInstance( ).AddRecord( "instruments added to collector: "+lInstruments.length, 0 );
                }

                if( lCollectorName.equalsIgnoreCase( "test" ) ) {
                    String[ ][ ]lInstruments = ( new TBloombergDealer( "org.postgresql.Driver", "jdbc:postgresql://"+lDBaseParams.get("host") + ":" + lDBaseParams.get("port") + "/"+lDBaseParams.get("base"), lDBaseParams.get("user"), lDBaseParams.get("pass") ) ).initializeSystem( );
                    lTAbstractCollector = new TTestingCollector( );
                    lTAbstractCollector.addInstruments( lInstruments );	 
                    TAsyncLogQueue.getInstance( ).AddRecord( "instruments downloaded: "+lInstruments.length, 0 );
                }

                if( lTAbstractCollector != null ){
                    lTAbstractCollectors.add( lTAbstractCollector );
                }
            }

            if(lTAbstractCollectors.isEmpty()){
                TAsyncLogQueue.getInstance( ).AddRecord( "Connectors list is Empty!", 0 );
                System.exit(0);
            }

            String lAppName = lGeneralParams.get( "name" );

            if( lAppName != null ) {
                TUserInterFace.main(new String[ ]{ lAppName } );
            } else {
                TUserInterFace.main(new String[ ]{ "default" } );
            }

            ExecutorService lConnectorsExecutor = Executors.newFixedThreadPool( lTAbstractCollectors.size() );
            lTAbstractCollectors.forEach( ( lCollector ) -> {
                lConnectorsExecutor.submit( lCollector );
            } );

            TAsyncLogQueue.getInstance( ).AddRecord( "started", 0 );
            TStatusClock.initCollector( lTAbstractCollectors );
            TStatusClock.startClock( Integer.parseInt( lGeneralParams.get( "clockDelay" ) ) );

            Thread.sleep( 2000 );
            //initialize tcp server
            TAsyncLogQueue.getInstance( ).AddRecord( "staring server thread", 0 );
            Thread lThread = new Thread( new ThreadServer( Integer.parseInt( lGeneralParams.get( "port" ) ) ) );
            lThread.start();

            TAsyncLogQueue.getInstance( ).AddRecord( "started", 0 );

            lThread.join();
            lConnectorsExecutor.shutdownNow();
                
        } else {
            TAsyncLogQueue.getInstance( ).AddRecord( "Check initialize params!", 0 );
        }
    }
}
