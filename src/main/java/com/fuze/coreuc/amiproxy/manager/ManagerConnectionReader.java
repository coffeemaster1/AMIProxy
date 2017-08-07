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
            managerConnection.initiateConnection();
            if (managerConnection.login()) {
                while (managerConnection.connectionActive()) {
                    event = managerConnection.readArrayFromServer();
                    managerListener.onEvent(new ArrayList<>(event));
                    //managerListener.onEvent(event);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


}
