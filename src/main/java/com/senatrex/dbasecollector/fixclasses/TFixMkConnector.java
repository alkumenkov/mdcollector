package com.senatrex.dbasecollector.fixclasses;

/**
 * <p>
 * Exemplar of this class provides market data<br>
 * updated 04 авг. 2015 г.16:56:47
 * @author Alexander Kumenkov
 * </p>
 */
public class TFixMkConnector{
	
	private int fPort = 2016;
	private String fHost = "localhost";
	
	/**
	 * Constructor initializes values
	 * @param aHost host with quik market data server
	 * @param aPort port with quik market data server
	 */
	public TFixMkConnector( String aHost, int aPort ) {
		fPort = aPort;
		fHost = aHost;
	}
	
	public void login( String lYourName, String lTarget ) {
		
		String lMessageToServer = "8=FIX.4.29=7635=A98=0108=30141=Y34=149=SYNTRX52=20150813-08:10:46.04656=BCSMDPUMP10=210";	
		
	}
	
	public void  generateLoginMessage() {
		
	}
}