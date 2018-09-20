package com.senatrex.tcpsample.ptcpclasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * <p>
 * class creates TCP client which sends to server "hello, server!" and gets servers answer <br>
 * updated 10 ���� 2015 �.16:22:17 
 *  </p>
 */
public class TTcpClient{
	private static final int PORT = 2017;
	private static final String HOST = "localhost";
	private static final int BUFFER_LENGTH = 1000;
	private Socket fSocket;

	/**
	 * <p>
	 * Constructor initializes socket
	 *  </p>
	 */
	public TTcpClient(){
		try {
			fSocket = new Socket( HOST, PORT );			
		} catch ( UnknownHostException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>
	 * Send message to server
	 * @param aMessageToServer Message, which sends to server
	 *  </p>
	 */
	public void runClientWriter( byte[] aMessageToServer ){		
		try {
			PrintWriter lWriter = new PrintWriter(fSocket.getOutputStream(),true);
			lWriter.println( new String( aMessageToServer ) );
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace( );
			try {
				fSocket.close( );
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace( );
			}
		} 
	}
	
	/**
	 * <p>
	 * Receive message from server
	 * @return Message, returned from server
	 *  </p>
	 */
	public byte[ ] runClientReader( ){		
		try {
			InputStream lStream = fSocket.getInputStream( );
			byte[ ] lBuffer = new byte[ BUFFER_LENGTH ];
			lStream.read( lBuffer );
			return lBuffer;
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace( );
			try {
				fSocket.close( );
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace( );
			}
			return null;
		} 
	}
	
	public void runClientLoopReader(){
		try {
			InputStream lStream = fSocket.getInputStream( );
			
			BufferedReader ins = new BufferedReader(new InputStreamReader(lStream));
			for(int i=0; i<3;i++) {
				String lOut = ins.readLine( );
				System.out.println( lOut );
			}
			fSocket.close();
			System.out.println("closed!");
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace( );
			try {
				fSocket.close( );
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace( );
			}
		}
		
	}
}