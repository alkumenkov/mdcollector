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
	
        @Override
	public void run( ) {
		TLocalMdataTable.getInstance().addListener( this.fSocket );
	}
}