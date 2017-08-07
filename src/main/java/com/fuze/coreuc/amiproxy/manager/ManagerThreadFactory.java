package com.fuze.coreuc.amiproxy.manager;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagerThreadFactory implements ThreadFactory{

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private AtomicInteger threadNumber = new AtomicInteger(1);
    private String namePrefix;

    public ManagerThreadFactory() {
        namePrefix = "Manager DaemonPool-"+ poolNumber.getAndIncrement() +'.';
    }

    @Override
    public Thread newThread (Runnable r) {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(namePrefix + threadNumber.getAndIncrement());

        return thread;
    }

}
