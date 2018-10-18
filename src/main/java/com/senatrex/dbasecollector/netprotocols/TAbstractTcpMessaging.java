package com.senatrex.dbasecollector.netprotocols;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import com.senatrex.dbasecollector.marketinstruments.TAbstractInstrument;
import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

/**
 * <p>
 * Exemplar of this class sends answers with mkdata to followers <br>
 * updated 11.10.2018 г.16:31:04
 * @author Alexander Kumenkov
 *  </p>
 */
public abstract class TAbstractTcpMessaging {
	
	protected Socket fSocket;
	protected boolean fIsClientAvailable;
	protected PrintWriter fPrintWriter;
	
	/**
	 * Constructor initializes values
	 * @param aSocket via it all data will be sent to client
	 */
	public TAbstractTcpMessaging( Socket aSocket ) {
            fIsClientAvailable = true;
            fSocket = aSocket;
            try {
                fPrintWriter = new PrintWriter(fSocket.getOutputStream(),true);
            } catch ( IOException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace( );
                fIsClientAvailable = false;
                TAsyncLogQueue.getInstance( ).AddRecord( "client disconnected", 0 );
                try {
                        fSocket.close( );
                } catch ( IOException e1 ) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace( );
                }
            }
                
           /* fPingThread = new Thread( new Runnable() {
                @Override
                public void run() {
                    while( fIsClientAvailable ){
                        try {
                            Thread.sleep( fPingFrequency );
                        } catch (InterruptedException ex) {
                        }
                        fPrintWriter.println( "<PING>" );
                    }
                }
            }); 
            
            fPingThread.start();*/
	}
	long fPingFrequency = 1000;//TODO make changeble
        Thread fPingThread;
        
	/**
	 * Method shows if messaging available for dialog
         * @return true, if available
	 */
	public boolean isAvailable( ) {
            return fIsClientAvailable;
	}
	
	/**
	 * Method generates message for client using each protocol
	 * @param aAbstractInstrument its values will be compiled to answer
	 */
	public abstract void compileMessage( TAbstractInstrument  aAbstractInstrument );
	
	/**
	 * Method sends message to client. If not success, client becomes disabled and deletes from Abstract Instrument
	 * @param aMessage message to client
	 */
	protected void sendMessage( String aMessage ) {		
            fPrintWriter.println( aMessage );	
	}
}