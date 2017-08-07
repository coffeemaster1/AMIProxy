package com.fuze.coreuc.amiproxy.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.concurrent.*;

public class ManagerReadListener implements EventListener{

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerReadListener.class);
    private EventTranslator eventTranslator;
    private AMIToTCCProxy tccProxy;
    private ThreadPoolExecutor executor;

    public ManagerReadListener (AMIToTCCProxy proxy) {
        eventTranslator = new EventTranslator();
        executor = new ThreadPoolExecutor(2, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ManagerThreadFactory());
        tccProxy = proxy;
    }

    public void onEvent (ArrayList<String> event) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                tccProxy.proxyEventArray(eventTranslator.translateEvent(event));
            }
        });
    }

}
