package com.senatrex.dbasecollector.queues;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;



public abstract class TAbstractQueue {
	
	protected Queue< Object >  fQueue= null;
	protected Thread fThread = null;
	protected boolean fTerminate = false;
	protected Object fWaitObject = new Object();
	protected int fSizeQueue;
	
	protected TAbstractQueue(  ) {
		
		fQueue = new ConcurrentLinkedQueue< Object >( );
		fThread = new Thread( new TPopThread( this ) );
		fThread.start( );
		fTerminate = false;
		fSizeQueue =0;
	}
	
	protected class TPopThread implements Runnable {
		
		TAbstractQueue fAbstractQueue;
		public TPopThread( TAbstractQueue aAbstractQueue ) {	
			fAbstractQueue = aAbstractQueue;
		}
		
		public void run( ) {
			while( !fTerminate ||  !fQueue.isEmpty() ) {
				
				if( !fTerminate && fQueue.isEmpty()  ) {
					try {							
						synchronized( fWaitObject ) {
							fWaitObject.wait( );
						}
					} catch ( InterruptedException e ) {
						// TODO Auto-generated catch block
						System.err.println( e.getMessage( ) );
					}
				}				
				fAbstractQueue.doWork( );
				fSizeQueue= fQueue.size();
			}
			System.out.println( "exit from run block" );
		}
	}

	/**
	 * Override this method to do some work needed
	 */	
	protected abstract void doWork( );
	
	/**
	 * <p>
	 * Adds item to Queue. Adds to item time since initializing in nanos
	 * @param aObjectToAdd  Object which will be added to queue
	 *  </p>
	 */	
	public void AddRecord( Object aObjectToAdd ) {
		synchronized( fWaitObject ) {			
			fWaitObject.notify( );
			fQueue.add( aObjectToAdd );
			fSizeQueue= fQueue.size();
			
		}
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
		//fAsyncQueue = null;
		System.out.println( "terminated!" );
	}  
}