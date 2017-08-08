package com.fuze.coreuc.amiproxy.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AMIToTCCProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMIToTCCProxy.class);

    private CopyOnWriteArrayList<AsteriskToTCCListener> tccListeners = new CopyOnWriteArrayList<>();

    public void proxyEvent (HashMap<String, String> event) {
        tccListeners.parallelStream().forEach(l -> l.eventReceived(event));
    }

    void proxyEventArray(ArrayList<String> event) {
        tccListeners.parallelStream().forEach(l -> l.eventArrayReceived(event));
    }

    public void addTCCListener (AsteriskToTCCListener listener) {
        tccListeners.add(listener);
    }


    public void removeTCCListener (AsteriskToTCCListener listener) {
        LOGGER.info("Removing TCC server from proxy list...");
        tccListeners.remove(listener);
    }

}
