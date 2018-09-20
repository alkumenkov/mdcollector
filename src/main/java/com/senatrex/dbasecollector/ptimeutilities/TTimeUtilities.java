package com.senatrex.dbasecollector.ptimeutilities;

import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
/**
 * <p>
 * Class has some useful utilities working with time<br>
 * updated 14 авг. 2015 г.13:59:47
 * @author Alexander Kumenkov
 *  </p>
 */
public class TTimeUtilities{
	public static String ChangeDataStringFormat( String aStringDate, String aCurrentFormat, String aNewFormat ){
		String lResuilt = "";
		try {
		Calendar lDate= java.util.Calendar.getInstance();			
		lDate.setTime( ( new java.text.SimpleDateFormat( aCurrentFormat ) ).parse( ( aStringDate ).trim( ) ) );
		long lLong=lDate.getTime().getTime();
		
		Date lDateStruct = new Date( lLong );			
		java.text.SimpleDateFormat lDateFormat = new java.text.SimpleDateFormat( aNewFormat );
		lResuilt = lDateFormat.format( lDateStruct );
		}
		catch (Exception e){
			System.err.println(e.getClass().getName()+": "+e.getMessage());	
			}
		return lResuilt;
	}
	
	public static TimeZone fSimpleTimeZone;
	
	public static String GetCurrentTime( String aNewFormat ) {
		String lResuilt="";
		
		try{	
			Calendar lDate= java.util.Calendar.getInstance();	
			long lLong=lDate.getTime().getTime();
			Date lDateStruct = new Date( lLong );			
			java.text.SimpleDateFormat lDateFormat = new java.text.SimpleDateFormat( aNewFormat );
			lDateFormat.setTimeZone( fSimpleTimeZone );
			lResuilt = lDateFormat.format( lDateStruct );
		} catch ( Exception e ) {
			System.err.println( e.getClass( ).getName( ) + ": " + e.getMessage( ) );	
			}
		return lResuilt;
	}
	
	public static String MinusCalendarDays(String aStringDate, String aCurrentFormat, int aCountDays){
		String lResuilt = "";		
		//aCurrentFormat = "yyyy-MM-dd";
		try {
			Calendar lDate= java.util.Calendar.getInstance();			
			lDate.setTime( ( new java.text.SimpleDateFormat( aCurrentFormat ) ).parse( ( aStringDate ).trim( ) ) );
			long lLong = lDate.getTime().getTime();

			for( int lDayIndex=0; lDayIndex<aCountDays; lDayIndex++ ) {			
				lLong = lLong - 86400000;							
				lDate.setTimeInMillis( lLong );				
				java.text.SimpleDateFormat lDateFormat = new java.text.SimpleDateFormat( aCurrentFormat );
				lResuilt = lDateFormat.format( lDate.getTime( ) );								
			}	
		}
		catch ( Exception e ) {
			System.err.println( e.getClass( ).getName( ) + ": " + e.getMessage( ) );	
			}
		return lResuilt;
	}
}