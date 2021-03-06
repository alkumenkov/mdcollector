package com.senatrex.dbasecollector.mkdatasources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.senatrex.dbasecollector.marketevents.TFixMarketEvent;
import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import com.senatrex.dbasecollector.marketinstruments.TStock;
import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;
import com.senatrex.dbasecollector.ptimeutilities.TTimeUtilities;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
//import com.senatrex.dbasecollector.queues.TAsyncLogQueue;
import com.senatrex.dbasecollector.queues.TMarketEventQueue;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>
 * Class gets market data from quik fix server<br>
 * updated 10 авг. 2015 г.18:04:28
 * @author Alexander Kumenkov
 *  </p>
 */
public class TFixCollector extends TAbstractMkDataCollector{
	
	private Map< String, String > fParametersMap;
	private boolean fIsClosed;
	private int fMessageNumber;
	private SocketChannel fSocket;
        private boolean fNeedReconnect=false;
        private Thread fHeartBeatThread = null;
        
	public TFixCollector( Map< String, String > aParametersMap ) {
            super( );
            fParametersMap = aParametersMap;
            fIsClosed = false;
            fMessageNumber = 1;
            if( aParametersMap != null ){
                fMarketDepth = Integer.parseInt( fParametersMap.get("MarketDepth") );
            }
            Log( "constructor finished!" );
	}
        
        boolean ConnectToServer(){
            boolean oResuilt = false;
            fSocket=null;
            try{
                fSocket  = SocketChannel.open(); 
                fSocket.connect( new InetSocketAddress( fParametersMap.get( "host" ), Integer.parseInt( fParametersMap.get( "port" ) ) ) );
                fSocket.configureBlocking( false );
                oResuilt = true;
                fNeedReconnect = false;
            } catch ( Exception e ) {
                oResuilt = false;
                Log( e.getLocalizedMessage( ) );
                // TODO Auto-generated catch block
                if( fSocket != null ) {
                    try {
                        fSocket.close();
                    } catch ( IOException e1 ) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                Log( e.getMessage( ) );
            } 
            return oResuilt;
        }
        int fNothingCameTime = 0;
        private boolean runConnector(){
            String lLoginMessage = "";
            boolean oIsNormalStopped = true;
            //String lResuillt = generateMkDataInstrumentRequest(fInstruments[40], 0, fParametersMap );
            lLoginMessage = generateLoginMessage( fParametersMap );
            for( int i=0; i < fInstruments.length; i++ ) {
                lLoginMessage += generateMkDataInstrumentRequest(fInstruments[i], 0, fParametersMap );
            }

          //  System.out.println(lLoginMessage);
            Log( lLoginMessage, 3 );

            if( fSocket != null ){
                if(fHeartBeatThread == null){
                    fHeartBeatThread = new Thread (
                    new Runnable(  ) {
                        public void run( ) {
                            while( fIsClosed == false && !fNeedReconnect ) {
                                try {
                                    Thread.sleep( 29000 );
                                } catch ( InterruptedException e ) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace( );
                                }
                                String lHeartbeatStr = generateHeartBeatMessage( fParametersMap );
                                if( !writeToSocket( lHeartbeatStr ) ){
                                    fNeedReconnect = true;
                                }
                            }
                        }
                    } );

                    writeToSocket( lLoginMessage );

                    fHeartBeatThread.start( );
                }

                char[] lBuffer = new char[ Integer.parseInt( fParametersMap.get( "RCVBUF" ) ) ];
                ByteBuffer buf = ByteBuffer.allocate( Integer.parseInt(fParametersMap.get( "RCVBUF" ) ) );
                String lLocalBuffer = "";
                String lIncomeString = "";

                while ( fIsClosed == false && fNeedReconnect == false ){

                    int res = -1;
                    try{
                        res = fSocket.read(buf);
                        
                    }catch( Exception e ){ 
                        fNeedReconnect = true;
                        break;
                    }

                    if( res <= 0 ){

                        try {
                            if( !fSocket.socket().getLocalAddress().isReachable(5000) ){
                                fNeedReconnect = true;
                                break;
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(TFixCollector.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        try {
                            Thread.sleep( 1000 );
                            fNothingCameTime++;
                        } catch (InterruptedException ex) {
                            
                        }
                        
                        if( fNothingCameTime > 300 ){
                            fNeedReconnect = true;
                            break;
                        }

                    } else {
                        fNothingCameTime=0;
                        //lBuffer = buf.array();
                        lIncomeString = (new String( buf.array() )).substring( 0, res );
                        buf.clear();
                        int lNewMessageIndex = lIncomeString.lastIndexOf( "8=FIX" );
                        if( lNewMessageIndex == 0 && lIncomeString.contains( "10=" ) ) {
                            lNewMessageIndex = lIncomeString.length();
                        }

                        if ( lNewMessageIndex > -1 ) {
                            lLocalBuffer += lIncomeString.substring( 0, lNewMessageIndex );
                            if ( lLocalBuffer.length( ) > 0 ) {
                                if ( lLocalBuffer.contains( "35=1" ) ) {
                                    String lHbA = generateHeartBeatAnswear( fParametersMap, getTagValue( lLocalBuffer, "112" ) );
                                    System.out.println( "My heart beat answer is " + lHbA );
                                    writeToSocket( lHbA );
                                }

                                try {  
                                    parseResponse( lLocalBuffer );
                                } catch (Exception e) {
                                    Log( e.getClass()+":"+e.getLocalizedMessage()+" "+lLocalBuffer );
                                }
                            }
                            lLocalBuffer = lIncomeString.substring(
                                    lIncomeString.lastIndexOf("10="),
                                    lIncomeString.length( ) );
                        } else {
                            lLocalBuffer += lIncomeString;
                        }
                        Arrays.fill(lBuffer, '0');
                    }
                }

            } else {
                Log( "socket not initialized", 0 );
            }

            try{
                fSocket.close();
            } catch(Exception e){
                Log( e.getMessage(), 0 );
            }
            
            return (!fNeedReconnect);
        }
        
	//13.08.2015 11:10:46.236	fixd	Send	8=FIX.4.29=7635=A98=0108=30141=Y34=149=SYNTRX52=20150813-08:10:46.04656=BCSMDPUMP10=2108=FIX.4.29=16035=V146=155=ATAD22=448=US6708312052207=XLON263=1264=10265=0267=2269=0269=1262=fixd#113796848#034=249=SYNTRX52=20150813-08:10:46.21656=BCSMDPUMP10=0478=FIX.4.29=15935=V146=155=GAZ22=448=US36829G1076207=XLON263=1264=10265=0267=2269=0269=1262=fixd#113796848#134=349=SYNTRX52=20150813-08:10:46.21656=BCSMDPUMP10=0328=FIX.4.29=16035=V146=155=HYDR22=448=US4662941057207=XLON263=1264=10265=0267=2269=0269=1
	public void run( ) {

            while( !fIsClosed ){
                Log("connecting");
                boolean lServerConnected = ConnectToServer();
                boolean IsNormalClosed = false;
                if( lServerConnected ){
                    Log("connected!");
                    fSleepTime = 1000L;
                    IsNormalClosed = runConnector();
                }
                fHeartBeatThread = null;
                if( !IsNormalClosed ){
                    if( fSleepTime < 60000L ){
                        fSleepTime *= 2;
                    }
                    try {
                        Thread.sleep(fSleepTime);
                    } catch (InterruptedException ex) {
                    }
                }
                
            }
	}
        
        long fSleepTime = 1000L;
        
        private boolean writeToSocket( String aMessage ) {
            ByteBuffer buf = ByteBuffer.allocate( aMessage.getBytes().length );
            buf.clear();
            buf.put( aMessage.getBytes( ) );
            buf.flip();
            boolean oSended=false;
            while(!fNeedReconnect && buf.hasRemaining( ) ) {
                try {
                    fSocket.write( buf );
                    oSended=true;
                    Log( "sending " + aMessage );
                } catch (IOException ex) {
                    Log(  ex.getLocalizedMessage( ) );
                    oSended = false;
                    break;
                }
            }

            TAsyncLogQueue.getInstance( ).AddRecord( "sended " + aMessage + " resuilt "+ oSended );
            return oSended;
        }
	
	public void parseResponse( String aResponse ){
             //   TAsyncLogQueue.getInstance( ).AddRecord( aResponse );
            String[] lMessages = aResponse.split("8=FIX.4.2");
            for( int i = 1; i < lMessages.length; i++ ) {
                
                    if( lMessages[ i ].contains( "35=W" ) ) {

                    String lExchangeName = getTagValue( lMessages[ i ], "48" );
                    ArrayList<TMarketOperation> lAsks = new ArrayList<TMarketOperation>();
                    ArrayList<TMarketOperation> lBids = new ArrayList<TMarketOperation>();
                    
                    String lTagValStr = getTagValue( lMessages[ i ], "268" );

                    if( lTagValStr.length() > 0 ) {

                        int lOperationCount = Integer.parseInt( lTagValStr );                        
                        for( int lOperationIndex = 0; lOperationIndex < lOperationCount; lOperationIndex++ ) {
                            int lBlockIndex = lMessages[ i ].indexOf( "269=" );

                            if( lBlockIndex > -1 ) {
                                lMessages[ i ] = lMessages[ i ].substring( lBlockIndex, lMessages[ i ].length( ) );

                                String lTypeOfOperation = getTagValue( lMessages[ i ], "269" );
                                TMarketOperation lMarketOperation = new TMarketOperation();                                            
                                try{
                                    lMarketOperation.setPrice( Double.parseDouble( getTagValue( lMessages[ i ], "270" ) ) );
                                    lMarketOperation.setVolume( Integer.parseInt( getTagValue( lMessages[ i ], "271" ) ) );
                                } catch (Exception e ) {
                                    lMarketOperation.setPrice( 0.0 );
                                    lMarketOperation.setVolume( 0 );
                                }

                                if( lTypeOfOperation.equals("1") ) {
                                    lAsks.add( lMarketOperation );	
                                }

                                if( lTypeOfOperation.equals("0") ) {
                                    lBids.add( lMarketOperation );	
                                }
                                lMessages[ i ] = lMessages[ i ].substring( "269".length(), lMessages[ i ].length( ) );
                            }
                        }

                        fMarketEventQueue.AddRecord( new TFixMarketEvent( lExchangeName, lAsks, lBids ) );
                    //System.out.println( lMessages[ i ] );
                    } else {
                        Log( "no tag 268! "+aResponse, 0 );
                    }
                }		
            }
	}
	
	private String generateHeartBeatMessage( Map< String, String > aParametersMap ) {
		
		String lMarketMessageBody = "49="+aParametersMap.get( "sender" ) + "56=" + aParametersMap.get( "targer" )+"34=" + fMessageNumber + "52=" + TTimeUtilities.GetCurrentTime( "yyyyMMdd-HH:mm:ss.SSS" ) + "";
		int lLength = lMarketMessageBody.length( ) + 5;
		
		String oResuilt = "8=FIX.4.29=" + lLength + "35=0"+lMarketMessageBody;
		String lEndStr = countSumTag( oResuilt );
		oResuilt += lEndStr;
		fMessageNumber++;
		return oResuilt;
		
	}
	
	private String generateHeartBeatAnswear( Map< String, String > aParametersMap, String aTagValue ) {
		
		String lMarketMessageBody = "49="+aParametersMap.get( "sender" ) + "56=" + aParametersMap.get( "targer" )+"34=" + fMessageNumber + "52=" + TTimeUtilities.GetCurrentTime( "yyyyMMdd-HH:mm:ss.SSS" ) + "112="+aTagValue+"";
		int lLength = lMarketMessageBody.length( ) + 5;
		String oResuilt = "8=FIX.4.29=" + lLength + "35=0"+lMarketMessageBody;
		String lEndStr = countSumTag( oResuilt );
		oResuilt += lEndStr;
		fMessageNumber++;
		return oResuilt;
		
	}
	
        public void closeCollector(){
            fIsClosed = true; 
            try {
                TMarketEventQueue.getInstance().finalize();
            } catch ( Throwable ex ) {
                Log( ex.getMessage() );
            }
        }
        
	/**
	 * Method initializes table of instruments
	 * @param aMessage message, where tag situated
	 * @param aTag which value need to be parsed
	 * @return value of setted tag
	 */
	private String getTagValue( String aMessage, String aTag ) {
            int lTagPosition =aMessage.indexOf( aTag + "=" );
            String oResuilt = "";
            if( lTagPosition>-1 ){
                int lStartIndex =  lTagPosition+aTag.length()+1;
                int lEndIndex = aMessage.indexOf( "", lTagPosition );
                if( lStartIndex > -1 && lEndIndex > -1 && lStartIndex <= lEndIndex ) {
                    oResuilt = aMessage.substring( lStartIndex, lEndIndex );
                } else {
                    System.out.println( "no tag!see log" );
                    Log( "error tag parsing!: " + aMessage + " " + aTag, 0 );
                }
            }
            return oResuilt;
	}
        
//9=25035=W49=BCSMDPUMP56=SYNTRX52=20150817-15:56:35.25534=77262=fixd#113796848#048=USD000UTSTOM55=USD000UTSTOM22=4207=CETS268=4269=0290=1270=65.235271=6269=0290=2270=65.234271=17269=1290=1270=65.236271=42269=1290=2270=65.24271=1510=035	
	private String countSumTag( String aStringToCount ) {
		char[ ] lCharArray = aStringToCount.toCharArray( );
		int lSumm = 0;
		for( int i=0; i<lCharArray.length; i++ ) {
			lSumm += ( int )( lCharArray[ i ] );
		}
		lSumm = lSumm % 256;
		char[] oRes = new char[3];
		oRes[0] = (char) ( lSumm / 100 + 48 );
		oRes[1] = (char) ( ( lSumm % 100 ) /10 + 48);
		oRes[2] = (char) ( lSumm % 10 + 48 );
		return "10=" + ( new String( oRes ) ) + "";
	}
	
	private String generateMkDataInstrumentRequest( String[] aInstruments, int aNumber, Map< String, String > aParametersMap ) { //tag 55		
		String lMarketMessageBody = "146=155=" + aInstruments[ 2 ] + "22=" + aParametersMap.get( "IDSource" ) + "48=" + aInstruments[ 1 ] + "207=" + aInstruments[ 3 ] + "263=1264="+aParametersMap.get( "MarketDepth" )+"265=0267=2269=0269=1262=fixd#113796848#" + aNumber + "34=" + fMessageNumber + "49="+aParametersMap.get( "sender" )+"52=" + TTimeUtilities.GetCurrentTime( "yyyyMMdd-HH:mm:ss.SSS" ) + "56=" + aParametersMap.get( "targer" ) + "";
		int lLength = lMarketMessageBody.length( ) + 5;
		String oResuilt = "8=FIX.4.29=" + lLength + "35=V"+lMarketMessageBody;
		String lEndStr = countSumTag( oResuilt );
		oResuilt += lEndStr;
		fMessageNumber++;
		return oResuilt;
	}
	
	private String generateLoginMessage( Map< String, String > aParametersMap ) { //tag 55
		String lMarketMessageBody = "98=0108=30141=Y34=" + fMessageNumber + "49="+aParametersMap.get( "sender" )+"52=" + TTimeUtilities.GetCurrentTime( "yyyyMMdd-HH:mm:ss.SSS" ) + "56=" + aParametersMap.get( "targer" ) + "";
		int lLength = lMarketMessageBody.length( ) + 5;
		
		String oResuilt = "8=FIX.4.29=" + lLength + "35=A"+lMarketMessageBody;
		String lEndStr = countSumTag( oResuilt );
		oResuilt += lEndStr;
		fMessageNumber++;
		return oResuilt;
	}
		
	/**
	 * Method adds instruments for download market data
	 * @param aInstruments array of instruments
	 */
	public void addInstruments( String[][] aInstruments ){
            TLocalMdataTable lLocalMdataTable = TLocalMdataTable.getInstance( );
            if( aInstruments != null ){
                    ArrayList<String[]> lInstruments = new ArrayList<>();
                    for( String[] lResuilt:aInstruments ){
                        if( lResuilt[ 3 ].equals( "CETS" ) || lResuilt[ 3 ].equals( "MICEX" ) || lResuilt[ 3 ].equals( "XLON" ) || lResuilt[ 3 ].equals( "QME" ) || lResuilt[ 3 ].equals( "FORTS" ) ){
                            lLocalMdataTable.addInstrument( new TStock( lResuilt[ 0 ], lResuilt[ 1 ], fMarketDepth ) );
                            lInstruments.add( lResuilt );
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
            if( fSocket != null ){
		return fSocket.isConnected();
            } else {
                return false;
            }
	}
}