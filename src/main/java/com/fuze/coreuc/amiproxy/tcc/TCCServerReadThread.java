package com.fuze.coreuc.amiproxy.tcc;

import com.fuze.coreuc.amiproxy.manager.ManagerConnectionWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.util.HashMap;

public class TCCServerReadThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCServerReadThread.class);

    private SynchronousQueue queue;
    private ManagerConnectionWriter amiWriter;
    private TCCConnection serverConnection;

    public TCCServerReadThread(TCCConnection conn, ManagerConnectionWriter writer) {
        this.serverConnection = conn;
        this.amiWriter = writer;
    }

    @Override
    public void run (){

        HashMap<String, String> event = new HashMap<>();

        while (serverConnection.connectionActive()) {
            try {
                event = serverConnection.readFromServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!event.isEmpty()) {
                amiWriter.proxyEvent(event);
            }

        }

        LOGGER.info("connection dead");


    }
}
