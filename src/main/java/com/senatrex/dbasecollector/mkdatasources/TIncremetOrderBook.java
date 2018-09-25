/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

import com.senatrex.dbasecollector.marketinstruments.TMarketOperation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 *
 * @author wellington
 */
public class TIncremetOrderBook {
    private int fMarketDepth;
    
    private NavigableMap<BigDecimal, BigDecimal> fASK = new TreeMap<>();
    private NavigableMap<BigDecimal, BigDecimal> fBID = new TreeMap<>(Comparator.reverseOrder());
    
    public TIncremetOrderBook( final int aMarketDepth ){
        super( );
        fMarketDepth = aMarketDepth;
    }
    
    public boolean Add_Ask( final BigDecimal aPrice, final BigDecimal aQty ) {
        fASK.put(aPrice, aQty);
        assert aQty.compareTo(BigDecimal.ZERO) > 0;
        
        Iterator<BigDecimal> lIT = fASK.keySet().iterator();
        int i=0;
        while( lIT.hasNext() ){
            BigDecimal lPrice = lIT.next();
            if( lPrice == aPrice ){
                break;
            }
            
            if( ++i>=fMarketDepth ){
                return false;
            }
        }
        
        return true;
    }
    
    public boolean Add_Bid( final BigDecimal aPrice, final BigDecimal aQty ) {
        fBID.put(aPrice, aQty);
        assert aQty.compareTo(BigDecimal.ZERO) > 0;
        
        Iterator<BigDecimal> lIT = fBID.keySet().iterator();
        int i=0;
        while( lIT.hasNext() ){
            BigDecimal lPrice = lIT.next();
            if( lPrice == aPrice ){
                break;
            }
            
            if( ++i>=fMarketDepth ){
                return false;
            }
        }
        
        return true;
    }
    
    public boolean Delete_Ask( final BigDecimal aPrice ) {
        fASK.remove(aPrice);
        return true;
    }
    
    public boolean Delete_Bid( final BigDecimal aPrice ) {
        fBID.remove(aPrice);
        return true;
    }
    
    public ArrayList<TMarketOperation> getAsk() {
        ArrayList<TMarketOperation> lResult = new ArrayList<TMarketOperation>(fMarketDepth);
        
        Iterator<BigDecimal> lIT = fASK.keySet().iterator();
        int i=0;
        while( lIT.hasNext() ){
            BigDecimal lPrice = lIT.next();
            lResult.add(i, new TMarketOperation( lPrice.doubleValue(), fASK.get(lPrice).intValue() ) );
            
            if( ++i>=fMarketDepth ){
                break;
            }
        }
        
        return lResult;
    }
    
    public ArrayList<TMarketOperation> getBig() {
        ArrayList<TMarketOperation> lResult = new ArrayList<TMarketOperation>(fMarketDepth);
        
        Iterator<BigDecimal> lIT = fBID.keySet().iterator();
        int i=0;
        while( lIT.hasNext() ){
            BigDecimal lPrice = lIT.next();
            lResult.add(i, new TMarketOperation( lPrice.doubleValue(), fBID.get(lPrice).intValue() ) );
            
            if( ++i>=fMarketDepth ){
                break;
            }
        }
        
        return lResult;
    }
    
    public void printbook( ){
        ArrayList<TMarketOperation> lAsks = this.getAsk();
        ArrayList<TMarketOperation> lBids = this.getBig();

        System.out.println("\r");
        for( int i=lAsks.size()-1; i >= 0 ; i-- ){
            System.out.println( lAsks.get(i).getPrice() + "\t" +  lAsks.get(i).getVolume() );
        }
        System.out.println("===");
        for( int i=0; i < lBids.size(); i++ ){
            System.out.println( lBids.get(i).getPrice() + "\t" +  lBids.get(i).getVolume() );
        }
    }
}
