package com.senatrex.firebirdsample.pdbaseworking;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class which working with string array as a table.
 * @author Kumenkov Alexander
 * @since 2015-06-29
 * 
 */
public class TTableUtilities {
    
	/**
     * Columns.
     */	
	private String[] fColumns;
	
	/**
     * Values.
     */
	private String[][] fValues;
	
	/**
     * Constructor.
     *
     * @param aIntArray Массив, который будет представлен как таблица. Нулевая строка будет fColumns
     */
	public TTableUtilities( String[ ][ ] aIntArray ){
		if( aIntArray.length > 0 ){
			fColumns = aIntArray[ 0 ];
			fValues = new String[ aIntArray.length - 1 ][ fColumns.length ];			
			for( int i = 0; i < fValues.length; i++ ) {
				fValues[ i ] = aIntArray[ i + 1 ];
			}
		}
	}
	
	//данные с тестов, провести регрессионнывй анализ
	/**
     * Получает номер колонны по названию.
     * 
     * @param aColumnName название колонны.
     * 
     * @return Номер.
     */
	public int GetColumnIndex( String aColumnName ) {
		int oResuilt = -1;
		for( int i = 0; i < fColumns.length; i++ ) {	
			if( fColumns[ i ].equalsIgnoreCase( aColumnName ) ) {
				oResuilt = i;
			}
		}
		return oResuilt;
	}
	
	/**
     * Gets setted columns with values.
     * @param aColumns names of columns to be received.
     * @return Table with name of columns in zero row.
     */
	public String[ ][ ] ReturnSubTable( String[ ] aColumns ) {
		
		String[ ][ ] oResuilt = new String[ 0 ][ 0 ];
		for ( int i = 0; i < aColumns.length; i++ ){
			if( this.GetColumnIndex( aColumns[ i ] ) == -1 ){
				return oResuilt;
			}			
		}
		
		oResuilt = new String[ fValues.length + 1 ][ aColumns.length ] ;
		oResuilt[ 0 ] = aColumns;
		for( int i = 0; i < fValues.length; i++ ) {
			String [ ] lValuesToAdd=new String [ aColumns.length ];
			for( int j = 0; j < aColumns.length; j++ ){
				lValuesToAdd[ j ] = fValues[ i ][ this.GetColumnIndex( aColumns[ j ] ) ];				
			}
			oResuilt[ i + 1 ] = lValuesToAdd;	
		}
		return oResuilt;
	}
	
	/**
     * Rename column names.
     * @param aColumns Array with new column names. Must be the same size.
     */
	public void RenameColumns( String[ ] aColumns ) {		
		if( aColumns.length == fColumns.length ) {
		for ( int i=0; i<aColumns.length; i++ ) {	
				fColumns[ i ]=aColumns[ i ];				
			}			
		}
	}

	private String ConvertArrIntoString( String [ ] aArrToConvert, String aStartValue, String aSplitValue, String aEndValue ) {
		StringBuilder oRes = new StringBuilder( );
		oRes.append( aStartValue );
		int lArrToConvertLen = aArrToConvert.length;
		for( int i = 0; i < lArrToConvertLen; i++ ) {
			oRes = oRes.append( aArrToConvert[ i ] );
			if( i < lArrToConvertLen-1 ) {
				oRes = oRes.append( aSplitValue );
			}			
		}
		oRes = oRes.append( aEndValue );
		return oRes.toString( );
	}
	
	/**
     * Make insert-query to database.
     * 
     * @param aTableName Table name which query addressed to.
     * 
     * @return insert query.
     */	
	public String GetQueryForDB( String aTableName ) {
		
		String lQuery = "insert into " + aTableName;
		lQuery = lQuery + ConvertArrIntoString( fColumns," (",",",")" );
		
		String[ ] lVauesToAdd = new String[ fValues.length ];
		for ( int i = 0; i < lVauesToAdd.length; i++ ) {
			lVauesToAdd[ i ] = ConvertArrIntoString ( fValues[ i ], " (", ",", ")" );
		}
		
		lQuery = lQuery + ConvertArrIntoString( lVauesToAdd, " values ",",",";" );
		return lQuery;		
	}
	
	/**
     * Join one table to another.
     * 
     * @param aStringArr string array to be joined. Must have same row-size.
     */
	public void cBind( String[ ][ ] aStringArr ){	
		if( aStringArr.length == fValues.length + 1 ) {
			
			String[ ] oNewArrColumn = new String[ fColumns.length + aStringArr[ 0 ].length ];
			
			for( int i = 0; i < fColumns.length; i++ ) {
				oNewArrColumn[ i ] = fColumns[ i ];
			}
			
			for( int i = 0; i < aStringArr[ 0 ].length; i++ ) {
				oNewArrColumn[ i + fColumns.length ] = aStringArr[ 0 ][ i ];
			}
			
			fColumns = oNewArrColumn;
			
			String[ ][ ] oNewArr = new String[ fValues.length ][ fValues[ 0 ].length + aStringArr[ 0 ].length ];
			for( int i = 0; i < fValues.length; i++ ) {				
				for( int j = 0; j < fValues[ 0 ].length; j++ ) {
					oNewArr[ i ][ j ] = fValues[ i ][ j ];				
				}
				
				for( int k = 0; k < aStringArr[ 0 ].length; k++ ) {
					oNewArr[ i ][ k + fValues[ 0 ].length ] = aStringArr[ i + 1 ][ k ];				
				}		
			}		
			fValues = oNewArr;	
		}
		else{
			System.out.println( "error binding Array" );
		}
	}
 
	/**
     * Primitive filtration.
     * 
     * @param aFieldName Nme of field, which values will be filtered.
     * @param  aCriterion value, which will be filtered.
     * 
     * @return table with filtered values.
     */
	public String[ ][ ] ReturnSubRowsTable( String aFieldName, String aCriterion ) {
		int lColumnIndex = 0;
		for( int i = 0; i < fColumns.length; i++ ){
			if( fColumns[ i ].equals( aFieldName ) ){
				lColumnIndex = i;
			}
		}		
		List< String[ ] > oResuilt = new ArrayList< String [ ] >( );
		oResuilt.add( fColumns );
		for( int i = 0; i < fValues.length; i++ ) {
			if( fValues[ i ][ lColumnIndex ].equals( aCriterion ) ) {
				oResuilt.add( fValues[ i ] );
			}
		}
		String[ ][ ] oTableString = oResuilt.toArray( new String[ 1 ][ ] );
		Arrays.sort( oTableString, new SorterByIndexValue( 0 ) );
		return oTableString;
	}
	
	
	public static class SorterByIndexValue implements Comparator< String[ ] > {
		private int fIndex;
		public SorterByIndexValue( int aIndex ) {
			fIndex = aIndex;
		}
	    public int compare( String aOneString[], String[] aAnotherString ) {
	    	  return aOneString[ fIndex ].compareTo( aAnotherString[ fIndex ] );	    	
	    }	   
	}
}
