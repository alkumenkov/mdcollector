/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

import junit.framework.TestCase;
/**
 *
 * @author wellington
 */
public class TBitmexCollectorTest extends TestCase{
    
    public void testOnMessage(){
        System.out.println("parseResponse");

        String fResuilt = "{\"table\":\"orderBook10\",\"action\":\"update\",\"data\":[{\"symbol\":\"XBTUSD\",\"bids\":[[11012.5,41002],[11012,1651],[11011.5,2701],[11011,5000],[11010.5,10000],[11010,208380],[11009,5000],[11008.5,20100],[11008,15000],[11007.5,40]],\"asks\":[[11013,31452],[11013.5,50000],[11014,4200],[11014.5,1768],[11015,1766],[11015.5,800],[11016,21351],[11016.5,7129],[11018,10000],[11019,2353]],\"timestamp\":\"2018-02-19T10:19:35.140Z\"}]}";
   
        TBitmexCollector lCol = new TBitmexCollector();
        lCol.onMessage( fResuilt );
        
    }
    
}
