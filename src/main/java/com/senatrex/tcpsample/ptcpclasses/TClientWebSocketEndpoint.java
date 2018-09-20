/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.senatrex.tcpsample.ptcpclasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.GrizzlyEngine;

/**
 *
 * @author wellington
 */

public class TClientWebSocketEndpoint extends Endpoint {
    
    public Session fSession;
 
    private ClientManager fClient;
    private MessageHandler.Whole<String> fCallBacker;
            
    public TClientWebSocketEndpoint(){
        super();
        fClient = null;
        fCallBacker = null;
    }
     
    ClientEndpointConfig fCec;
    
    public void initialize( String aAddress, MessageHandler.Whole<String> aCallBacker ){

        fClient = ClientManager.createClient();
        fCallBacker = aCallBacker;
                
        //System.getProperties().put("javax.net.debug", "all");
        final SSLContextConfigurator lDefaultConfig = new SSLContextConfigurator();

        lDefaultConfig.retrieve(System.getProperties());
            // or setup SSLContextConfigurator using its API.

        SSLEngineConfigurator lSslEngineConfigurator =
            new SSLEngineConfigurator(lDefaultConfig, true, false, false);
        fClient.getProperties().put(GrizzlyEngine.SSL_ENGINE_CONFIGURATOR,
            lSslEngineConfigurator);
        
        fCec = ClientEndpointConfig.Builder.create().build();

        try {
            fSession = fClient.connectToServer( this, fCec, new URI( aAddress ) );// or "wss://echo.websocket.org"//"wss://www.bitmex.com/realtime"

      //      fSession.getBasicRemote().sendText("{\"op\": \"subscribe\", \"args\": [\"orderBook10:XBTUSD\",\"orderBook10:BCHH18\"]}");
       //     System.out.println("sended");
    
        } catch (Exception e) {
            e.printStackTrace();
        } 
        int t=0;
        
    }

    public boolean sendText( String aText ){
        boolean lSucces = false;

        if( fSession != null ) {
            try {
                fSession.getBasicRemote().sendText( aText );
               // fSession.getBasicRemote().sendText("{\"op\": \"subscribe\", \"args\": [\"orderBook10:XBTUSD\",\"orderBook10:BCHH18\"]}");
            } catch ( IOException ex ) {
                Logger.getLogger( TClientWebSocketEndpoint.class.getName() ).log( Level.SEVERE, null, ex );
            }
            lSucces = true;
        }
       
        return lSucces;
    }
    
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("Opened!");
        fSession = session;
        fSession.addMessageHandler( fCallBacker );
        
    }
    
    @Override
    public void onError(Session session, Throwable thr) {
        System.out.println("error naxui!");
        System.out.println(thr);
     /*   try {
            session.close();
            //    super.onError(session, thr); //To change body of generated methods, choose Tools | Templates.
        } catch (IOException ex) {
            Logger.getLogger(ClientWebSocketEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    @Override
    public void onClose(Session sn, CloseReason cr) {
        System.out.println("closing blyat!");
        System.out.println(cr);
        super.onClose( sn,cr ); //To change body of generated methods, choose Tools | Templates.
    }
    
    }

