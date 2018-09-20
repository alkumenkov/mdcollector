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
import com.senatrex.dbasecollector.queues.TMarketEventQueue;


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
	private Socket fSocket;
	PrintWriter fWriter;

	
	public TFixCollector( Map< String, String > aParametersMap ) {
                super( );
		fParametersMap = aParametersMap;
		fIsClosed = false;
		fMessageNumber = 1;
                try{
                    fMarketDepth = Integer.parseInt( fParametersMap.get( "MarketDepth" ) );
                }catch(Exception e){
                    fMarketDepth = 1;
                }
	}
	
	//13.08.2015 11:10:46.236	fixd	Send	8=FIX.4.29=7635=A98=0108=30141=Y34=149=SYNTRX52=20150813-08:10:46.04656=BCSMDPUMP10=2108=FIX.4.29=16035=V146=155=ATAD22=448=US6708312052207=XLON263=1264=10265=0267=2269=0269=1262=fixd#113796848#034=249=SYNTRX52=20150813-08:10:46.21656=BCSMDPUMP10=0478=FIX.4.29=15935=V146=155=GAZ22=448=US36829G1076207=XLON263=1264=10265=0267=2269=0269=1262=fixd#113796848#134=349=SYNTRX52=20150813-08:10:46.21656=BCSMDPUMP10=0328=FIX.4.29=16035=V146=155=HYDR22=448=US4662941057207=XLON263=1264=10265=0267=2269=0269=1
	public void run( ) {

            String lLoginMessage = "";
                    
            if( fParametersMap != null && fInstruments != null ){
                    //String lResuillt = generateMkDataInstrumentRequest(fInstruments[40], 0, fParametersMap );
                lLoginMessage = generateLoginMessage( fParametersMap );
                for( int i=0; i < fInstruments.length; i++ ) {
                    lLoginMessage += generateMkDataInstrumentRequest(fInstruments[i], 0, fParametersMap );
                }

              //  System.out.println(lLoginMessage);
                TAsyncLogQueue.getInstance( ).AddRecord( lLoginMessage, 3 );
                try{
                    fSocket = new Socket( fParametersMap.get( "host" ), Integer.parseInt( fParametersMap.get( "port" ) ) );
                    fSocket.setTcpNoDelay( true );
                    fWriter = new PrintWriter( fSocket.getOutputStream( ), true );

                } catch(Exception e){
                    TAsyncLogQueue.getInstance( ).AddRecord( "error initializing socket! "+e.getLocalizedMessage(), 0 );
                }

                if( fSocket != null && fWriter != null ){
                    Thread lHeartBeat = new Thread (
                    new Runnable(  ) {
                        public void run( ) {
                            while( fIsClosed == false ) {
                                try {
                                        Thread.sleep( 29000 );
                                    } catch ( InterruptedException e ) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace( );
                                    }
                                String lHeartbeatStr = generateHeartBeatMessage( fParametersMap );
                                fWriter.print( new String( lHeartbeatStr ) );
                                fWriter.flush();
                            }
                        }
                    } );

                    fWriter.print( new String( lLoginMessage ) );
                    fWriter.flush();
                    lHeartBeat.start( );
                    
                    InputStream lStream = null;
                    BufferedReader ins = null;
                    try{
                        lStream = fSocket.getInputStream( );	
                        ins = new BufferedReader( new InputStreamReader( lStream ) );
                    } catch ( Exception e ){
                        TAsyncLogQueue.getInstance().AddRecord( "error creating input stream "+e.getLocalizedMessage(), 0 );
                    }
                    
                    if( lStream != null && ins != null ){
                        char[ ] lBuffer = new char[ Integer.valueOf( fParametersMap.get( "RCVBUF" ) ) ];

                        String lLocalBuffer = "";
                        String lIncomeString = "";
                        while( fIsClosed == false ) {
                            int res = -1;
                            try{
                                res = ins.read( lBuffer );
                            } catch( Exception e ){
                                TAsyncLogQueue.getInstance( ).AddRecord( "error reading socket! "+e.getLocalizedMessage(), 0 );
                            }

                            if( res == -1 ){
                                fIsClosed = true;
                            } else {
                                lIncomeString = (new String( lBuffer ) ).substring( 0, res ) ;
                                int lNewMessageIndex = lIncomeString.lastIndexOf( "8=FIX" );
                                if( lNewMessageIndex > -1 ) {
                                    lLocalBuffer += lIncomeString.substring( 0, lNewMessageIndex );
                                    if( lLocalBuffer.length( ) > 0 ) {
                                        TAsyncLogQueue.getInstance().AddRecord( lLocalBuffer, 3 );
                                        if( lLocalBuffer.contains("35=1") ) {
                                            String lHbA = generateHeartBeatAnswear( fParametersMap, getTagValue(lLocalBuffer, "112") );
                                            System.out.println("heart beat answer is "+lHbA );
                                            fWriter.print( lHbA );
                                            fWriter.flush();
                                        }
                                        
                                        parseResponse( lLocalBuffer );
                                        
                                    }
                                    lLocalBuffer = lIncomeString.substring( lIncomeString.lastIndexOf( "8=FIX" ), lIncomeString.length( ) );
                                } else {
                                    lLocalBuffer += lIncomeString;
                                }
                                Arrays.fill(lBuffer, '0');
                            }
                        }
                    }
                } else {
                    TAsyncLogQueue.getInstance().AddRecord( "socket not initialized", 0 );
                }
                    
                try{
                    fSocket.close();
                } catch(Exception e){
                    TAsyncLogQueue.getInstance().AddRecord( e.getMessage(), 0 );
                }
		
            } else {
                TAsyncLogQueue.getInstance().AddRecord( this.toString() + "initializing values null!", 0 );
            }
		 
	}
	
	public void parseResponse( String aResponse ){
             //   TAsyncLogQueue.getInstance( ).AddRecord( aResponse );
            String[] lMessages = aResponse.split("8=FIX.4.2");
            for( int i = 1; i < lMessages.length; i++ ) {

                    if( lMessages[ i ].indexOf("35=W")> -1 ) {

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
                        TAsyncLogQueue.getInstance().AddRecord( "no tag 268! "+aResponse, 0 );
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
                TAsyncLogQueue.getInstance().AddRecord( ex.getMessage() );
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
                    TAsyncLogQueue.getInstance().AddRecord( "error tag parsing!: " + aMessage + " " + aTag, 0 );
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
		return !fSocket.isClosed();
            } else {
                return false;
            }
	}
}