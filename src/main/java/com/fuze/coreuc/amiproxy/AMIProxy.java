package com.fuze.coreuc.amiproxy;

import com.fuze.coreuc.amiproxy.manager.*;
import com.fuze.coreuc.amiproxy.tcc.TCCConnectionListener;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import com.fuze.coreuc.amiproxy.config.MyAppConfig;

import java.io.IOException;

public class AMIProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMIProxy.class);

    public static void main(String[] args) throws Exception {

        // Remove existing handlers attached to j.u.l root logger
        // and add SLF4JBridgeHandler to j.u.l's root logger (absolute logging redirection)
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final MyAppConfig cfg = ConfigFactory.create(MyAppConfig.class);

        TCCConnectionListener TCCListener;
        AMIToTCCProxy proxy = new AMIToTCCProxy();

        try {
            AsteriskManagerConnection ami = new AsteriskManagerConnection(
                    cfg.host(), cfg.user(), cfg.password(), cfg.amiPort());
            ami.initiateConnection();
            ManagerReadListener listener = new ManagerReadListener(proxy);
            ManagerConnectionReader reader = new ManagerConnectionReader(ami, listener);
            ManagerConnectionWriter writer = new ManagerConnectionWriter(ami);

            reader.setDaemon(true);
            reader.start();

            try {
                TCCListener = new TCCConnectionListener(
                        cfg.host(), cfg.user(), cfg.password(), cfg.tccPort());
                TCCListener.addProxy(proxy);
                TCCListener.addWriter(writer);
                TCCListener.listen();
            }
            catch (IOException e) {
                LOGGER.error("Could not open TCC Listener connection...");
                e.printStackTrace();
            }
        } catch (IOException e) {
            LOGGER.error("Could not open connection to asterisk...");
            e.printStackTrace();
        }
    }
}
