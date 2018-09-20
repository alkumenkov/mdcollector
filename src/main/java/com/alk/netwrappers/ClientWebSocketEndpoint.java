/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alk.netwrappers;

import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.ClientEndpoint;
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

public class ClientWebSocketEndpoint extends Endpoint {
    
    public Session fSession=null;

    ClientManager fClient;
    String fAddress;
    TWebSocketable fWebClient=null;    
    ClientEndpointConfig cec;
public static final Object lSyncObj = new Object();
    public ClientWebSocketEndpoint( String aAddress, TWebSocketable aClient ){
        super();

        fAddress = aAddress;
        fWebClient = aClient;
        fClient = ClientManager.createClient();

        //System.getProperties().put("javax.net.debug", "all");
        final SSLContextConfigurator defaultConfig = new SSLContextConfigurator();

        defaultConfig.retrieve(System.getProperties());
            // or setup SSLContextConfigurator using its API.

        SSLEngineConfigurator sslEngineConfigurator =
            new SSLEngineConfigurator(defaultConfig, true, false, false);
        fClient.getProperties().put(GrizzlyEngine.SSL_ENGINE_CONFIGURATOR,
            sslEngineConfigurator);

        cec = ClientEndpointConfig.Builder.create().build();

        try {
            URI lUri = new URI( aAddress );
            try {
                fSession = fClient.connectToServer( this, cec, lUri );

            } catch (IOException ex) {
                Logger.getLogger(ClientWebSocketEndpoint.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch ( DeploymentException ex) {
            System.out.println( ex.getLocalizedMessage() );
        } catch (URISyntaxException ex) {
            System.out.println( ex.getLocalizedMessage() );
        }
        int t=0;
    }

    public void disconnect(){        
        try {
            fSession.close();
            fClient.getExecutorService().shutdownNow();            
            super.finalize();
        } catch (Exception ex) {
            TAsyncLogQueue.getInstance().AddRecord( ex.getLocalizedMessage() );
        } catch (Throwable ex) {
            TAsyncLogQueue.getInstance().AddRecord( ex.getLocalizedMessage() );
        }     
    }

    public void sendMessage( String aMessageTosend ){
        byte ptext[] = aMessageTosend.getBytes();
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(aMessageTosend);
        
        try{
            if( fSession != null ){
                TAsyncLogQueue.getInstance().AddRecord("sending message! "+aMessageTosend);
                synchronized( lSyncObj ){
                    fSession.getBasicRemote().sendText( aMessageTosend );
                }
            } else {
                fSession = fClient.connectToServer( this, cec, new URI( fAddress ) );// or "wss://echo.websocket.org"//"wss://www.bitmex.com/realtime"
                fSession.getBasicRemote().sendText( aMessageTosend );
                 
            }
        } catch( Exception e ){
            TAsyncLogQueue.getInstance().AddRecord("error sending message!"+e.getLocalizedMessage());
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("Opened!");
        fWebClient.onMessage("Opened!");
        fSession = session;
        
        
        
        fSession.addMessageHandler(new MessageHandler.Whole<String>() {

            @Override
            @OnMessage
            public void onMessage( String message ) {
                synchronized( lSyncObj ){
                    fWebClient.onMessage( message );
                }
            }
        });  
    }

    @Override
    public void onError(Session session, Throwable thr) {       
        fWebClient.onMessage("Error!"+thr);    
    }

    @Override
    public void onClose(Session sn, CloseReason cr) {
             
        super.onClose( sn,cr ); //To change body of generated methods, choose Tools | Templates.
        fWebClient.onMessage("Closing!"+cr);   
    }

}

