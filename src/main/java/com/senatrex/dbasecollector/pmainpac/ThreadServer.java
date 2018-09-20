/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.pmainpac;

import com.senatrex.tcpsample.ptcpclasses.TTcpServer;

/**
 *
 * @author wellington
 */
public class ThreadServer implements Runnable {
    	int fPort;
    	public ThreadServer( int aPort ) {
    		fPort = aPort;
    	}
		public void run( ) {
                    TTcpServer lTcpServer = new TTcpServer( fPort );
                    lTcpServer.runServer( );
		}
    }

