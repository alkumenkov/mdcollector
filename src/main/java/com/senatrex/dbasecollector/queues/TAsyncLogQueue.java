package com.senatrex.dbasecollector.queues;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * <p>
 * class creates Thread safe queue <br>
 * updated 24 авг. 2015 г.14:50:22
 *  </p>
 */
public class TAsyncLogQueue{
	
	private static TAsyncLogQueue fAsyncQueue = null;
	private Queue< String >  fQueue= null;
	private long fTimeFromInitialize = 0L;
	private boolean fTerminate = false;
	private Object fWaitObject = new Object();
	private Thread fThread = null;
	private String fLogFilePath = "log.txt";
	private int fSizeQueue;
	private int fMessageLevel=0;
                
	private TAsyncLogQueue(  ) {
		fSizeQueue = 0;
		fQueue = new ConcurrentLinkedQueue< String >( );
		fTerminate = false;				
		fThread = new Thread( new TPopThread( fLogFilePath ) );
		fThread.start( );
	}
	
        public void setLogLevel( int aMessageLevel ) {
            fMessageLevel = aMessageLevel;
        }
        
	private class TPopThread implements Runnable{

		
		private FileWriter fFileOutput; 
		public TPopThread( String aLogFilePath ) {
			try {
				fFileOutput = new FileWriter( new File( aLogFilePath ) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void run( ) {
			
			
			while( !fTerminate ||  !fQueue.isEmpty() ) {

				if( !fTerminate && fQueue.isEmpty() ) {
					try {							
						synchronized( fWaitObject ) {
							fWaitObject.wait( );
						}
					} catch ( InterruptedException e ) {
						// TODO Auto-generated catch block
						System.err.println( e.getMessage() );
					}
				}				
				doWork( );
			}
			System.out.println( "exit from run block" );
		}
				
		protected void doWork( ) {
	
			if( fQueue.size( ) > 0 ) {
			String lStringToWrite = fQueue.poll( );
			//System.out.println( lStringToWrite );
		
			try {
				fFileOutput.write( lStringToWrite + "\r\n" );
				fFileOutput.flush( );
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace( );
			}
			
			}
		
		}
	}
	
	/**
	 * <p>
	 * In first calling creates safe queue object. Then return queue object
	 * @return Reference to queue Object
	 *  </p>
	 */	
	public static synchronized TAsyncLogQueue getInstance( ) {		
			if( fAsyncQueue == null ) {
				fAsyncQueue = new TAsyncLogQueue( );
			}
			//System.out.println( "returning instance" );
		return fAsyncQueue;
		
	}

	/**
	 * <p>
	 * Adds item to Queue. Adds to item time since initializing in nanos
	 * @param aString  String which will be added to queue
         * @param aMessageLevel level of importsnce of message 
	 *  </p>
	 */	
	public void AddRecord( String aString, int aMessageLevel ) {
            if( aMessageLevel <= fMessageLevel ){
                synchronized( fWaitObject ) {	

                    fWaitObject.notify( );
                    //System.out.println( "adding to queue " + aString );
                    fQueue.add( (new Date()).toString() + "\t" + (System.nanoTime( ) - fTimeFromInitialize) + "\t" + aString );
                    fSizeQueue = fQueue.size();
                }
            }
	}
        
        /**
	 * <p>
	 * Adds item to Queue. Adds to item time since initializing in nanos
	 * @param aString  String which will be added to queue
	 *  </p>
	 */	
	public void AddRecord( String aString ) {
            AddRecord( aString, 0 );
	}

	/**
	 * <p>
	 * Waiting for finishing all popers ant terminates process
	 *  </p>
	 */	
	public void finalize( ) throws Throwable {
		System.out.println( "start terminate" );
		fTerminate = true;	
		synchronized( fWaitObject ) {			
			fWaitObject.notify( );
		}
		
		fThread.join( );
		fAsyncQueue = null;
		System.out.println( "terminated!" );
		
    }  
	
	/**
	 * <p>
	 * Changing log path. Using before first initializing. Default value is "log.txt"
	 * @param aFilePath Path to log file
	 *  </p>
	 */	
	public void ChangeFilePath( String aFilePath ) {		
		fLogFilePath = aFilePath;	
	}
}