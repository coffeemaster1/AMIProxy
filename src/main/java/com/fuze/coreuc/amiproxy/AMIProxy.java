package com.fuze.coreuc.amiproxy;
import com.fuze.coreuc.amiproxy.manager.*;
import com.fuze.coreuc.amiproxy.tcc.TCCConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;


public class AMIProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMIProxy.class);


    public static void main(String[] args) throws Exception {

        TCCConnectionListener TCCListener;

        // Remove existing handlers attached to j.u.l root logger
        // and add SLF4JBridgeHandler to j.u.l's root logger (absolute logging redirection)
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();


        AMIToTCCProxy proxy = new AMIToTCCProxy();
        AsteriskManagerConnection ami = new AsteriskManagerConnection(
                "10.225.123.236", "isymphony", "1symph0ny2526");
        ManagerReadListener listener = new ManagerReadListener(proxy);
        ManagerConnectionReader reader = new ManagerConnectionReader(ami, listener);
        ManagerConnectionWriter writer = new ManagerConnectionWriter(ami);
        //reader.addProxy(proxy);
        reader.setDaemon(true);
        reader.start();


        TCCListener = new TCCConnectionListener(
                "10.225.123.236", "isymphony", "1symph0ny2526");
        TCCListener.addProxy(proxy);
        TCCListener.addWriter(writer);
        TCCListener.listen();


    }
}
