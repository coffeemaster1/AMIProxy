package com.fuze.coreuc.amiproxy.tcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import java.util.concurrent.ThreadLocalRandom;


public class TCCConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCConnection.class);

    private Socket TCCServerSocket;
    private String CallServerHostname;
    private String CallServerUsername;
    private String CallServerPassword;
    private OutputStream outputStream;
    private InputStream inputStream;
    private PrintStream out;
    private BufferedReader in;
    private MessageDigest MD5;
    private boolean connected;
    private String EOL = "\r\n";

    private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    public TCCConnection(Socket socket, String hostname, String username, String password) throws IOException, NoSuchAlgorithmException {

        this.TCCServerSocket = socket;
        this.CallServerHostname = hostname;
        this.CallServerUsername = username;
        this.CallServerPassword = password;
        this.outputStream = TCCServerSocket.getOutputStream();
        this.inputStream = TCCServerSocket.getInputStream();
        this.out = new PrintStream(outputStream, true);
        this.in = new BufferedReader (new InputStreamReader(inputStream));
        this.MD5 = MessageDigest.getInstance("MD5");
        this.connected = true;

    }

    public TCCConnection(TCCConnection tcc){
        this.TCCServerSocket = tcc.TCCServerSocket;
        this.CallServerHostname = tcc.CallServerHostname;
        this.CallServerUsername = tcc.CallServerUsername;
        this.CallServerPassword = tcc.CallServerPassword;
        this.outputStream = tcc.outputStream;
        this.inputStream = tcc.inputStream;
        this.out = tcc.out;
        this.in = tcc.in;
        this.MD5 = tcc.MD5;
        this.connected = tcc.connected;
    }

    public void setupConnection () throws IOException {
        HashMap<String, String> response;

        String stringChallenge = getChallenge();
        String MD5Hash = getMD5(stringChallenge);

        LOGGER.info("Connection Setup: " + TCCServerSocket.toString() + " : " + in);

        writeToServer("Asterisk Call Manager/1.1");
        response = readFromServer();

        String[][] digestChallenge = {{"Response", "Success"}, {"ActionID", response.get("actionid")}, {"Challenge", stringChallenge}};

        writeArrayToServer(digestChallenge);

        response = readFromServer();


        LOGGER.info("MD5Hash: " + MD5Hash);
        if (response.get("key").equals(MD5Hash)) {
            LOGGER.info("MD5 Matched");
            String[][] authSuccess = {{"Response", "Success"}, {"ActionID", response.get("actionid")}, {"Message", "Authentication accepted"}};
            writeArrayToServer(authSuccess);

        }

    }

    public boolean connectionActive () {
        return connected;
    }

    public void writeArrayToServer (String[][] action) {
        String output = "";

        for ( String[] value : action) {
            output = output + value[0] + ": " + value[1] + "\r\n";
        }
        output = output + "\r\n";

        out.print(output);

    }

    public void writeMapToServer (Map<String, String> action){
        String output = "";

        for(Map.Entry<String, String> entry : action.entrySet()) {
            output = output + entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        output = output + "\r\n";

        out.print(output);

    }

    public void writeToServer (String output) {

        out.print(output + "\r\n");
    }

    public void sendActionArray (ArrayList<String> action) {

        StringBuilder rawAction = new StringBuilder();

        for (String e : action) {
            if (e.contains("Data: ")) {
                rawAction.append(e.replace("Data: ", "") + EOL);
            }
            else {
                rawAction.append(e + EOL);
            }
        }
        rawAction.append(EOL);

        LOGGER.info("Attempting Send to TCC Server...");

        out.print(rawAction.toString());
        out.flush();

        LOGGER.info("Packet sent to TCC Server..." + action.get(0));

    }

    public void sendAction (HashMap<String, String> action) {

        //LOGGER.info("");
        //LOGGER.info("Sending Event to TCC Server:");
        //action.forEach((k, v) -> LOGGER.info(k + ": " + v));
        //LOGGER.info("");


        StringBuilder rawAction = new StringBuilder();

        for (Map.Entry<String, String> entry : action.entrySet()){
            if (entry.getKey().contains("Data")) {
                rawAction.append(entry.getValue() + EOL);
            }
            else {
                rawAction.append(entry.getKey() + ": " + entry.getValue() + EOL);
            }
        }

        rawAction.append(EOL);

        out.print(rawAction.toString());

    }

    public HashMap<String, String> readFromServer () throws IOException {
        HashMap<String, String> action = new HashMap<>();
        String[] split;
        String line;

        while ((line = in.readLine()) != null) {
            if (line.equals("\r\n") || line.isEmpty()) {
                break;
            }
            //split = line.split(": ");
            split = splitFast(line);
            //LOGGER.info(line);
            action.put(split[0], split[1]);
        }

        if (line == null && action.isEmpty()) {
            connected = false;
            LOGGER.info("Reader closed connection");
        }

        LOGGER.info ("Successfully Read Action from TCC") ;

        return action;
    }

    public String getMD5 (String stringChallenge) {

        String digest = "";
        byte[] bytes;

        MD5.update(stringChallenge.getBytes(),0, stringChallenge.length());
        MD5.update(CallServerPassword.getBytes(), 0, CallServerPassword.length());

        return toHexString(MD5.digest());
    }

    public static String toHexString(byte[] b)
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

    public String getChallenge () {

        Integer challenge = ThreadLocalRandom.current().nextInt(1000000, 9999999 + 1);
        String stringChallenge = challenge.toString();

        return stringChallenge;
    }

    public final String[] splitFast(String string) {

        int index = string.indexOf(':');
        String[] splitResult = { string.substring( 0, index), string.substring(index + 2) };

        return splitResult;
    }

}
