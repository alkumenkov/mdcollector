package com.senatrex.tcpsample.ptcpclasses;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.senatrex.dbasecollector.queues.TAsyncLogQueue;


/**
 * <p>
 * class creates TCP server waiting for clients, shows messages from clients and answers them "hello, client!" <br>
 * updated 11 ���� 2015 �.12:00:34
 *  </p>
 */
public class TTcpServer{
	private int fPort;
	private ServerSocket fServSocket;
	
	/**
	 * <p>
	 * Constructor Initializes server socket
	 *  </p>
	 */
	public TTcpServer( int aPort ){
		try {
			//if( fServSocket == null ) {
				fPort = aPort;
				fServSocket = new ServerSocket( fPort );
			//}
				TAsyncLogQueue.getInstance( ).AddRecord( "server initialized!", 0 );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>
	 * waiting for client connections, if some client connected, starts dialog with him <br>
	 * each dialog declared by TMessageProcessing object, starts in new thread
	 *  </p>
	 */
	public void runServer( ){
		try {			
			
                        TAsyncLogQueue.getInstance().AddRecord( "Server started", 0 );
			while( !fServSocket.isClosed() ){	//-for multiple connections		

                        TAsyncLogQueue.getInstance().AddRecord( "server cycle", 1 );
			//	synchronized( fServSocket ) {
                        Socket lSocket = fServSocket.accept( );
                        TAsyncLogQueue.getInstance().AddRecord( "Client connected", 0 );
                        lSocket.setKeepAlive( false );
                        TAbstractMessageProcessing lMessageProcessing = new TMarketMessageProcessing( lSocket);
                        Thread lThread = new Thread( lMessageProcessing );
                        lThread.start( );
					
			//	}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}