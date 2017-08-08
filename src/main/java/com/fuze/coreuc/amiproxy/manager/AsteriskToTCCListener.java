package com.fuze.coreuc.amiproxy.manager;

import com.fuze.coreuc.amiproxy.tcc.TCCConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.ArrayList;


public class AsteriskToTCCListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsteriskToTCCListener.class);

    private TCCConnection tccOut;

    public AsteriskToTCCListener (TCCConnection out) {
        this.tccOut = out;
    }

    void eventReceived(HashMap<String, String> event) {
        tccOut.sendAction(event);
    }

    void eventArrayReceived(ArrayList<String> event) {
        tccOut.sendActionArray(event);
    }

}
