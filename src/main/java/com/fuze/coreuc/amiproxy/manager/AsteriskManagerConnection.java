package com.fuze.coreuc.amiproxy.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;

import java.util.HashMap;

public class AsteriskManagerConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsteriskManagerConnection.class);

    private String managerHostname;
    private String managerUsername;
    private String managerPassword;
    private Integer managerPort;
    private Socket managerConnection;
    private PrintStream connectionWrite;
    private BufferedReader connectionRead;
    private MessageDigest MD5;
    private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    private boolean isConnected;
    private Integer connectionAttempts = 0;
    private String EOL = "\r\n";

    public AsteriskManagerConnection (String hostname, String username, String password) {
        this.managerHostname = hostname;
        this.managerUsername = username;
        this.managerPassword = password;
        this.managerPort = 5038;
        this.isConnected = false;
    }

    public AsteriskManagerConnection (String hostname, String username, String password, Integer port) {
        this.managerHostname = hostname;
        this.managerUsername = username;
        this.managerPassword = password;
        this.managerPort = port;
        this.isConnected = false;
    }

    public void initiateConnection () throws IOException {

        try {
            managerConnection = new Socket(managerHostname, managerPort);
            isConnected = true;
            connectionWrite = new PrintStream(managerConnection.getOutputStream(), true);
            connectionRead = new BufferedReader(new InputStreamReader(managerConnection.getInputStream()));
        }

        catch (IOException e) {
            LOGGER.error("Could not open socket...");
            connectionAttempts++;
            throw e;
        }
    }

    void login() throws IOException, NoSuchAlgorithmException {
        HashMap<String, String> readEvent;
        String [][] actionArray;

        if (!isConnected) {
            initiateConnection();
        }
        readEvent = readFromServer();
        if (readEvent.get("Action").contains("Asterisk Call Manager")) {
            actionArray = new String[][]{{"Action", "Challenge"}, {"AuthType", "MD5"}};
            sendAction(createAction(actionArray));
            readEvent = readFromServer();
            MD5 = MessageDigest.getInstance("MD5");
            actionArray = new String[][]{{"Action", "Login"}, {"AuthType", "MD5"}, {"Username", managerUsername}, {"Key", getMD5(readEvent.get("Challenge"))}};
            sendAction(createAction(actionArray));
            readEvent = readFromServer();
            if (!readEvent.get("Response").equals("Success")) {
                LOGGER.error("Could not authenticate with server!");
                connectionAttempts++;
                closeConnection();
            }
        }
        connectionAttempts++;
    }

    CopyOnWriteArrayList<String> readArrayFromServer() throws IOException {

        CopyOnWriteArrayList<String> action = new CopyOnWriteArrayList<>();
        String line;

        LOGGER.info("Reading from Server");


        while ((line = connectionRead.readLine()) != null){
            if (line.isEmpty()){
                break;
            }
            if (line.contains("Asterisk Call Manager")){
                action.add("Action: " + line);
                break;
            }
            if (line.contains("Event: Shutdown")){
                break;
            }
            if (!line.contains(": ")) {
                action.add("Data: " + line);
                continue;
            }
            LOGGER.info (line);
            action.add(line);
        }
        if (line.contains("Event: Shutdown") || action.isEmpty()) {
            closeConnection();
        }
        LOGGER.info("");
        return action;
    }

    private HashMap<String, String> readFromServer() throws IOException {

        HashMap<String, String> action = new HashMap<>();
        ArrayList<String> split;
        String line;

        while ((line = connectionRead.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            if (line.contains("Asterisk Call Manager")) {
                action.put("Action", line);
                break;
            }
            if (!line.contains(": ")) {
                action.put("Data", line);
                continue;
            }
            if (line.contains("Event: Shutdown")){
                break;
            }
            split = new ArrayList<>(Arrays.asList(splitFast(line)));

            if (split.size() < 2) {
                split.add(" ");
            }
            action.put(split.get(0), split.get(1));
        }

        if (action.isEmpty() || line.contains("Event: Shutdown")) {
            closeConnection();
        }
        return action;
    }

    void sendAction(HashMap<String, String> action) {

        StringBuilder rawAction = new StringBuilder();
        action.forEach((k, v) -> rawAction.append(k).append(": ").append(v).append(EOL));
        rawAction.append(EOL);

        connectionWrite.print(rawAction.toString());

    }

    void sendActionArray (ArrayList<String> action) {
        StringBuilder rawAction = new StringBuilder();
        action.forEach(l ->
                rawAction.append(l)
                .append(EOL));
        rawAction.append(EOL);

        connectionWrite.print(rawAction.toString());
    }

    private HashMap<String, String> createAction(String[][] action) {

        HashMap<String, String> mappedAction = new HashMap<>();

        for ( String[] line : action) {
            mappedAction.put(line[0], line[1]);
        }

        return mappedAction;
    }

    private static String toHexString(byte[] b)
    {
        final StringBuilder sb;

        sb = new StringBuilder(b.length * 2);
        for (byte aB : b)
        {
            sb.append(hexChar[(aB & 0xf0) >>> 4]);
            sb.append(hexChar[aB & 0x0f]);
        }
        return sb.toString();
    }

    private String getMD5(String stringChallenge) {

        byte[] bytes;

        MD5.update(stringChallenge.getBytes(),0, stringChallenge.length());
        MD5.update(managerPassword.getBytes(), 0, managerPassword.length());

        return toHexString(MD5.digest());

    }

    private void closeConnection() throws IOException {
        LOGGER.info("Closing Asterisk connection");
        connectionWrite.close();
        connectionRead.close();
        managerConnection.close();
        isConnected = false;
    }

    boolean connectionActive() {
        return this.isConnected;
    }

    private String[] splitFast(String string) {

        int index = string.indexOf(':');

        return new String[]{ string.substring( 0, index), string.substring(index + 2) };
    }

    Integer getConnectionAttempts() {
        return this.connectionAttempts;
    }


}
