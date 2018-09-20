package com.senatrex.tcpsample.ptcpclasses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.senatrex.dbasecollector.queues.TAsyncLogQueue;

/**
 * <p>
 * class declares dialog between client and server. <br>
 * in this case, server will answer "hello Client" to everyone who connected and
 * tell something<br>
 * updated 11 ���� 2015 �.13:21:11
 * </p>
 */
public abstract class TAbstractMessageProcessing implements Runnable {

    private static final int BUFFER_LENGTH = 1000;
    Socket fSocket;

    /**
     * <p>
     * Constructor initializes socket
     *
     * @param aSocket, via him will be processing dialog between client and
     * server
     * </p>
     */
    public TAbstractMessageProcessing(Socket aSocket) {
        fSocket = aSocket;
    }

    /**
     * <p>
     * getting message from client, show it and answer "hello Client". Override
     * this method to make another dialog logic
     * </p>
     */
    public abstract void run();

    /**
     * <p>
     * @return Client message in bytes
     * </p>
     */
    private byte[] receiveMessage() {
        byte[] lBuffer = new byte[BUFFER_LENGTH];
        try {
            BufferedReader ins = new BufferedReader(new InputStreamReader(fSocket.getInputStream()));
            String lOut = ins.readLine();
            lBuffer = lOut.getBytes();
            TAsyncLogQueue.getInstance().AddRecord( new String(lBuffer), 1);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                fSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }
        return lBuffer;
    }

    /**
     * <p>
     * @param Message for server in bytes
     * </p>
     */
    private void sendMessage(byte[] aMessageToSend) {
        try {
            PrintWriter lWriter = new PrintWriter(fSocket.getOutputStream(), true);
            lWriter.println(new String(aMessageToSend));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
