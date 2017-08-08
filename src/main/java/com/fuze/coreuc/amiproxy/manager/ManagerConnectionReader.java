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

        try {
            if (!managerConnection.connectionActive()) {
                managerConnection.initiateConnection();
                managerConnection.login();
            }
            while (managerConnection.connectionActive()) {
                event = managerConnection.readArrayFromServer();
                if (!event.isEmpty()) {
                    managerListener.onEvent(new ArrayList<>(event));
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}
