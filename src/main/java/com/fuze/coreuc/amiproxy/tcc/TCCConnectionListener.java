package com.fuze.coreuc.amiproxy.tcc;

import com.fuze.coreuc.amiproxy.manager.AMIToTCCProxy;
import com.fuze.coreuc.amiproxy.manager.AsteriskToTCCListener;
import com.fuze.coreuc.amiproxy.manager.ManagerConnectionWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.SynchronousQueue;

public class TCCConnectionListener {

    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private AMIToTCCProxy proxy;
    private ManagerConnectionWriter writer;


    private static final Logger LOGGER = LoggerFactory.getLogger(TCCConnectionListener.class);

    public TCCConnectionListener (String hostname, String username, String password) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.port = 5038;
    }

    public TCCConnectionListener (String hostname, String username, String password, int port) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.port = port;
    }

    public void addProxy (AMIToTCCProxy proxy){
        this.proxy = proxy;
    }

    public void addWriter (ManagerConnectionWriter writer){
        this.writer = writer;
    }

    public void listen () throws IOException {
        Socket serverSocket;
        ServerSocket listener;
        TCCConnection connection;

        try
        {
            listener = new ServerSocket(this.port);
        }
        catch ( IOException e )
        {
            LOGGER.error("Unable start AgiServer: cannot to bind to *:" + port + ".", e);
            throw e;
        }

        while( listener.isBound() ) {

            TCCServerReadThread thread;
            AsteriskToTCCListener tccListener;

            LOGGER.info("Socket: " + listener);
            try {
                serverSocket = listener.accept();

                LOGGER.info("Connection established on port: " + port + " from address: " + serverSocket.getInetAddress());
                LOGGER.info("Launching thread for this server connection... ");

                connection = new TCCConnection(serverSocket, hostname, username, password);
                connection.setupConnection();
                tccListener = new AsteriskToTCCListener(new TCCConnection(connection));
                proxy.addTCCListener(tccListener);
                connection.setProxy(proxy);
                connection.setListener(tccListener);

                thread = new TCCServerReadThread(connection, writer);

                thread.setDaemon(true);
                thread.start();

            }

            catch( IOException e ) {
                LOGGER.error("Could not open socket from connection attempt that was received...");
                throw e;
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("Could not create MD5 algorithm object...");
                e.printStackTrace();
            }
        }
    }
}
