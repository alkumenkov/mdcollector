/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.dbasecollector.mkdatasources;

/**
 *
 * @author wellington
 */
public class TOrderBookEntry {
    
    private String fPrice;
    private String fQty;

    public TOrderBookEntry(  String aPrice, String aQty ){
        fPrice  = aPrice;
        fQty = aQty;
    }
  
    public String getPrice() {
        return fPrice;
    }

    public void setPrice(String price) {
        this.fPrice = price;
    }

    public String getQty() {
        return fQty;
    }

    public void setQty(String qty) {
        this.fQty = qty;
    }

    @Override
    public String toString() {
        return "price "+fPrice+" qty "+fQty;
    }
}
