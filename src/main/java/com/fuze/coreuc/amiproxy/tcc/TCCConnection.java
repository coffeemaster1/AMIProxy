package com.fuze.coreuc.amiproxy.tcc;

import com.fuze.coreuc.amiproxy.manager.AsteriskToTCCListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import com.fuze.coreuc.amiproxy.manager.AMIToTCCProxy;
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
    private String tccIP;
    private boolean connected;
    private AsteriskToTCCListener asteriskListener;
    private AMIToTCCProxy asteriskProxy;
    private String EOL = "\r\n";

    private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    TCCConnection(Socket socket, String hostname, String username, String password) throws IOException, NoSuchAlgorithmException {

        this.TCCServerSocket = socket;
        this.CallServerHostname = hostname;
        this.CallServerUsername = username;
        this.CallServerPassword = password;
        this.tccIP = TCCServerSocket.getInetAddress().toString();
        this.outputStream = TCCServerSocket.getOutputStream();
        this.inputStream = TCCServerSocket.getInputStream();
        this.out = new PrintStream(outputStream, true);
        this.in = new BufferedReader (new InputStreamReader(inputStream));
        this.MD5 = MessageDigest.getInstance("MD5");
        this.connected = true;

    }

    TCCConnection(TCCConnection tcc){
        this.TCCServerSocket = tcc.TCCServerSocket;
        this.CallServerHostname = tcc.CallServerHostname;
        this.CallServerUsername = tcc.CallServerUsername;
        this.CallServerPassword = tcc.CallServerPassword;
        this.tccIP = TCCServerSocket.getInetAddress().toString();
        this.outputStream = tcc.outputStream;
        this.inputStream = tcc.inputStream;
        this.out = tcc.out;
        this.in = tcc.in;
        this.MD5 = tcc.MD5;
        this.connected = tcc.connected;
    }

    void setupConnection() throws IOException {
        HashMap<String, String> response;

        String stringChallenge = getChallenge();
        String MD5Hash = getMD5(stringChallenge);

        LOGGER.info("Connection Setup: " + TCCServerSocket.toString() + " : " + in);

        writeToServer("Asterisk Call Manager/1.1");
        response = readFromServer();

        String[][] digestChallenge = {{"Response", "Success"}, {"ActionID", response.get("actionid")}, {"Challenge", stringChallenge}};

        writeArrayToServer(digestChallenge);

        response = readFromServer();

        if (response.get("key").equals(MD5Hash)) {
            LOGGER.info("MD5 Matched");
            String[][] authSuccess = {{"Response", "Success"}, {"ActionID", response.get("actionid")}, {"Message", "Authentication accepted"}};
            writeArrayToServer(authSuccess);

        }

    }

    boolean connectionActive() {
        return connected;
    }

    private void writeArrayToServer(String[][] action) {
        StringBuilder output = new StringBuilder();

        for ( String[] value : action) {
            output.append(value[0]).append(": ").append(value[1]).append(EOL);
        }
        output.append("\r\n");

        out.print(output);

    }

    public void writeMapToServer (Map<String, String> action){
        StringBuilder output = new StringBuilder();

        for(Map.Entry<String, String> entry : action.entrySet()) {
            output.append(entry.getKey()).append(": ").append(entry.getValue()).append(EOL);
        }
        output.append(EOL);

        out.print(output);

    }

    private void writeToServer(String output) {

        out.print(output + EOL);
    }

    public void sendActionArray (ArrayList<String> action) {

        StringBuilder rawAction = new StringBuilder();

        for (String e : action) {
            if (e.contains("Data: ")) {
                rawAction.append(e.replace("Data: ", "")).append(EOL);
            }
            else rawAction.append(e).append(EOL);
        }
        rawAction.append(EOL);

        LOGGER.info("Attempting Send to TCC Server: " + this.tccIP);

        out.print(rawAction.toString());
        out.flush();

    }

    public void sendAction (HashMap<String, String> action) {

        StringBuilder rawAction = new StringBuilder();

        for (Map.Entry<String, String> entry : action.entrySet()){
            if (entry.getKey().contains("Data")) {
                rawAction.append(entry.getValue()).append(EOL);
            }
            else {
                rawAction.append(entry.getKey()).append(": ").append(entry.getValue()).append(EOL);
            }
        }

        rawAction.append(EOL);

        out.print(rawAction.toString());

    }

    ArrayList<String> readArrayFromServer () throws IOException {
        ArrayList<String> action = new ArrayList<>();
        String line;

        try {
            while ((line = in.readLine()) != null) {
                if (line.equals(EOL) || line.isEmpty()) {
                    break;
                }
                action.add(line);
            }
            if (line == null && action.isEmpty()) {
                closeConnection();
            }

            LOGGER.info("Read action array from TCC server: " + this.tccIP);
        }
        catch (IOException e){
            LOGGER.error("Error reading from TCC connection: " + this.tccIP);
            e.printStackTrace();
            closeConnection();
        }
        return action;
    }

    private HashMap<String, String> readFromServer() throws IOException {

        HashMap<String, String> action = new HashMap<>();
        String[] split;
        String line;

        try {
            while ((line = in.readLine()) != null) {
                if (line.equals(EOL) || line.isEmpty()) {
                    break;
                }
                split = splitFast(line);
                action.put(split[0], split[1]);
            }

            if (line == null && action.isEmpty()) {
                closeConnection();
            }
            LOGGER.info("Successfully Read Action from TCC");
        }
        catch (IOException e){
            LOGGER.error("Error reading from TCC connection: " + this.tccIP);
            e.printStackTrace();
            closeConnection();
        }
        return action;
    }

    private String getMD5(String stringChallenge) {

        MD5.update(stringChallenge.getBytes(),0, stringChallenge.length());
        MD5.update(CallServerPassword.getBytes(), 0, CallServerPassword.length());

        return toHexString(MD5.digest());
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

    private String getChallenge() {

        Integer challenge = ThreadLocalRandom.current().nextInt(1000000, 9999999 + 1);

        return challenge.toString();
    }

    private String[] splitFast(String string) {

        int index = string.indexOf(':');

        return new String[]{ string.substring( 0, index), string.substring(index + 2) };
    }

    String getIP() {
        return tccIP;
    }

    private void closeConnection() throws IOException {
        LOGGER.info("Closing TCC connection to " + this.tccIP + "...");
        removeListenerFromProxy();
        out.close();
        in.close();
        TCCServerSocket.close();
        connected = false;
    }

    void setProxy(AMIToTCCProxy proxy) {
        this.asteriskProxy = proxy;
    }

    void setListener(AsteriskToTCCListener listener) {
        this.asteriskListener = listener;
    }

    private void removeListenerFromProxy() {
        asteriskProxy.removeTCCListener(asteriskListener);
    }

}
