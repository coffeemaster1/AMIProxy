package com.fuze.coreuc.amiproxy.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;

public class ManagerConnectionReader extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerConnectionReader.class);

    private AsteriskManagerConnection managerConnection;
    private ManagerReadListener managerListener;


    public ManagerConnectionReader (AsteriskManagerConnection conn, ManagerReadListener listener) {
        managerListener = listener;
        managerConnection = conn;
    }

    @Override
    public void run () {
        CopyOnWriteArrayList<String> event;

        while (managerConnection.getConnectionAttempts() < 4) {
            try {

                if (!managerConnection.connectionActive()) {
                    managerConnection.initiateConnection();
                }
                managerConnection.login();
                while (managerConnection.connectionActive()) {
                    event = managerConnection.readArrayFromServer();
                    if (!event.isEmpty() && !event.get(0).equals("Event: Skip")) {
                        managerListener.onEvent(new ArrayList<>(event));
                    }
                }
                Thread.sleep(15000);
            } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
