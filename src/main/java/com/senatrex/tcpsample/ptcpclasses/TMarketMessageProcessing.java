package com.senatrex.tcpsample.ptcpclasses;

import java.net.Socket;

import com.senatrex.dbasecollector.pmainpac.TLocalMdataTable;

public class TMarketMessageProcessing extends TAbstractMessageProcessing {
	
	/**
	 * @param aSocket
	 */
	public TMarketMessageProcessing(Socket aSocket ) {
            super(aSocket);
            // TODO Auto-generated constructor stub
	}
	
        Thread fPingThread;
        long fPingFrequency = 1000;
        
        @Override
	public void run( ) {
            fPingThread = new Thread( new Runnable() {
                @Override
                public void run() {
                    while( fIsClientAvailable ){
                        try {
                            Thread.sleep( fPingFrequency );
                        } catch (InterruptedException ex) {
                        }
                        sendMessage( "<PING>" );
                    }
                }
            }); 
            
            fPingThread.start();
            TLocalMdataTable.getInstance().addListener( this.fSocket );
            
	}
}