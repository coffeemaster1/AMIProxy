package com.fuze.coreuc.amiproxy.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.ArrayList;

public class ManagerConnectionWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerConnectionWriter.class);

    private AsteriskManagerConnection managerConnection;

    public ManagerConnectionWriter (AsteriskManagerConnection conn) {
        this.managerConnection = conn;
    }

    public void proxyEvent (HashMap<String, String> event) {
        LOGGER.info("");
        LOGGER.info("Sending Event from TCC Server");
        event.forEach((k, v) -> LOGGER.info(k + ": " + v));
        LOGGER.info("");

        managerConnection.sendAction(event);
    }

    public void proxyEventArray (ArrayList<String> event) {
        LOGGER.info("");
        LOGGER.info("Sending Array Event from TCC Server");

        event.forEach(LOGGER::info);
        LOGGER.info("");

        managerConnection.sendActionArray(event);
    }

}
