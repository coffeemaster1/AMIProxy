package com.fuze.coreuc.amiproxy.tcc;

import com.fuze.coreuc.amiproxy.manager.ManagerConnectionWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public class TCCServerReadThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCServerReadThread.class);

    private ManagerConnectionWriter amiWriter;
    private TCCConnection serverConnection;
    private String tccServerIP;

    TCCServerReadThread(TCCConnection conn, ManagerConnectionWriter writer) {
        this.serverConnection = conn;
        this.amiWriter = writer;
        this.tccServerIP = serverConnection.getIP();
    }

    @Override
    public void run (){
        ArrayList<String> event;

        while (serverConnection.connectionActive()) {
            try {
                event = serverConnection.readArrayFromServer();
                if (event.isEmpty()) {
                    continue;
                }
                amiWriter.proxyEventArray(event);
            } catch (IOException e) {
                LOGGER.error("Error reading event from TCC server");
                e.printStackTrace();
                break;
            }
        }
        LOGGER.info("TCC connection to " + serverConnection.getIP() + " was lost...");

    }

    String getReaderIP() {
        return tccServerIP;
    }
}
