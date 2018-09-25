package com.senatrex.firebirdsample.pdbaseworking;
import java.sql.*;
import java.util.*;

import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

/**
 * <p>
 * Class executes different operation with dbase.<br> 
 * Need to include into project jdbc - driver
 * </p>
 * @author Kumenkov Alexander
 * @since 2015-06-26
 */
public class DBaseWorking {

	private static String fConnectionString = "";
	private static String fLogin = "";
	private static String fPassword = "";
	private static String fLastEcxeption = "";
	private static String fDriverPath = "org.postgresql.Driver";//default driver
	private static Connection fConnection;
	private static boolean fIsKeepAlive;
	public static String GetLastException( ) {
		return fLastEcxeption;
	}

	/**
	 * Constructor
	 * 
	 * @param aConnectionString connection string to dbase.
	 * @param aLogin login.
	 * @param aPassword password.
	 */
	public DBaseWorking( String aConnectionString, String aLogin, String aPassword ) {		
		fConnectionString = aConnectionString;
		fLogin = aLogin;
		fPassword = aPassword;	
		fLastEcxeption = "";
		fIsKeepAlive = false;
	};

	/**
	 * Initializes dbase driver. Default value is "org.postgresql.Driver"
	 * @param aDriver new driver, default is "org.postgresql.Driver"
	 */
	public void InitializeDriver( String aDriver ) {
		fDriverPath = aDriver;		
	};

	/**
	 * Returns
	 * @param aQuery Text of query.
	 * @return Table, where zero-based string is name of columns.
	 */
	public String[ ][ ] GetQueryAsStringArr( String aQuery ) {			
		String [ ][ ] oData = new String[ 1 ][ 1 ];
		Vector< String [ ] > lInputData = GetQueryAsTable( aQuery );
		if( lInputData.size( ) > 0 ) {
			int lVerticalLength = lInputData.size( );    	
			int lGorisontalLength = ( lInputData.get( lVerticalLength - 1 ) ).length;
			oData=new String[ lVerticalLength ][ lGorisontalLength ];
			
			for( int i = 0; i < lVerticalLength; i++ ) {
				String[ ] lStr = lInputData.get( i );
				for( int j = 0; j < lGorisontalLength; j++ ) {
					oData[ i ][ j ] = lStr[ j ];
				}
			}
		}
	return oData; 
	};

	/**
	 * Sends update-query to dbase.
	 * @param aQuery query to execute.
	 * @return true, if success
	 */
	public boolean ExecuteUpdateQuery( String aQuery ){
		Connection lConnection = null;
		Statement lStatement = null;
		Boolean oResuilt = false;
		try{
			Class.forName( fDriverPath );
			if( fIsKeepAlive ) {
                            lConnection = fConnection;
			} else {
                            lConnection = DriverManager.getConnection( fConnectionString, fLogin, fPassword );
			}
						
			lStatement = lConnection.createStatement( );			
			int lUpdateResuilt = lStatement.executeUpdate( aQuery );				
			if( lUpdateResuilt == 1 ) {
                            oResuilt = true;
                            fLastEcxeption = "";
			}	
			
		} catch ( Exception e ) {
			fLastEcxeption =  e.getClass( ).getName( ) + ": " + e.getMessage( );
			TAsyncLogQueue.getInstance().AddRecord( fLastEcxeption+" query is " + aQuery, 0 );
			try {
				fConnection = DriverManager.getConnection( fConnectionString, fLogin, fPassword );
			} catch ( Exception e1 ) {
				fLastEcxeption =  e.getClass( ).getName( ) + ": " + e.getMessage( );
			}
		}
                
                if( !fIsKeepAlive & lConnection != null ) {
                    try{ lConnection.close( ); } catch ( Exception e ) {
			fLastEcxeption =  "ExecuteUpdateQuery: "+e.getClass( ).getName( ) + ": " + e.getMessage( ) + "\t"+aQuery;
			TAsyncLogQueue.getInstance().AddRecord( fLastEcxeption, 0 );
                    }
                }
                
		return oResuilt;
	}
		
	private Vector< String[ ] >  GetQueryAsTable( String aQuery ) {
		Connection lConnection = null;
		Statement lStatement = null;
		Vector< String[ ] > oSqlResuiltVector=new Vector< String[ ] >( );			
		
		try{
			Class.forName( fDriverPath );
			lConnection = DriverManager.getConnection( fConnectionString, fLogin, fPassword );//("jdbc:postgresql://192.168.1.103:5432/accounting","postgres","raduga32145");
			lStatement = lConnection.createStatement( );				
                        String lQuery = aQuery;			
			ResultSet lResultSet = lStatement.executeQuery( lQuery );
			ResultSetMetaData lResultSetMetaData = lResultSet.getMetaData( );	
			int lColumnsNumber = lResultSetMetaData.getColumnCount( );				
			while( !lResultSet.isLast( ) ) {
				String [] lRowsEnum=new String[ lColumnsNumber ];
				for( int i = 0; i < lColumnsNumber; i++ ) {
					if( oSqlResuiltVector.size( ) == 0 ) {
						lRowsEnum[ i ]= lResultSetMetaData.getColumnName( i + 1 );
					} else {
						if( i == 0 ) {
							lResultSet.next( );
						} 
						lResultSet.getRow( );
						lRowsEnum[ i ]= lResultSet.getString( i + 1 );
					}
				}
				oSqlResuiltVector.add( lRowsEnum );
			}
			fLastEcxeption = "";
			lConnection.close( );
		} catch ( Exception e ) {
			fLastEcxeption = "GetQueryAsTable: " +  e.getClass( ).getName( ) + ": " + e.getMessage( ) + aQuery;
			TAsyncLogQueue.getInstance().AddRecord( fLastEcxeption, 0 );
		}
		
		return oSqlResuiltVector;
	};

	public ResultSet GetQueryAsResuiltSet( String aQuery ) {
		
		Connection lConnection = null;
		Statement lStatement = null;
		try {
			Class.forName( fDriverPath );
			lConnection = DriverManager.getConnection( fConnectionString, fLogin, fPassword );//("jdbc:postgresql://192.168.1.103:5432/accounting","postgres","raduga32145");
			lStatement = lConnection.createStatement( );
			String lQuery = aQuery;
			ResultSet lResultSet = lStatement.executeQuery( lQuery );
			fLastEcxeption =  "";
			return lResultSet;
		} catch ( Exception e ) {
			fLastEcxeption =  e.getClass( ).getName( ) + ": " + e.getMessage( ) ;	
			TAsyncLogQueue.getInstance( ).AddRecord( fLastEcxeption, 0 );
		}
		return null;
	}

	/**
	 * @param aIsKeepAlive keep connection alive
	 */
	public void keepConnectionAlive( boolean aIsKeepAlive ) {
		// TODO Auto-generated method stub
		try {
			fConnection = DriverManager.getConnection( fConnectionString, fLogin, fPassword );
			fIsKeepAlive = aIsKeepAlive;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			fLastEcxeption =  e.getClass( ).getName( ) + ": " + e.getMessage( );
			fConnection = null;
		}
	};
}

