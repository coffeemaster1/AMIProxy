package com.fuze.coreuc.amiproxy.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class EventTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventTranslator.class);
    private ConcurrentHashMap<String, HashMap<String, String>> bridgeList = new ConcurrentHashMap<>();
    private static final ArrayList<String> changedEvents = new ArrayList<>(Arrays.asList("DialBegin", "DialEnd", "QueueCallerJoin",
            "QueueCallerLeave", "AgentCalled", "AttendedTransfer", "BlindTransfer", "Hangup", "BridgeEnter", "BridgeLeave"));

    public ArrayList<String> translateEvent (ArrayList<String> event) {

        String first = splitFastFirst(event.get(0));

        if (!inEventList(first)) {
            return event;
        }
        if (first.equals("DialBegin")) {
            return transformDial(event);
        }
        else if (first.equals("DialEnd")) {
            return transformDialEnd(event);
        }
        else if (first.equals("QueueCallerJoin")) {
            return transformJoin(event);
        }
        else if (first.equals("QueueCallerLeave")) {
            return transformLeave(event);
        }
        else if (first.equals("AgentCalled")) {
            return transformAgentCalled(event);
        }
        else if (first.equals("AttendedTransfer")) {
            return transformTransfer(event);
        }
        else if (first.equals("BlindTransfer")) {
            return transformTransfer(event);
        }
        else if (first.equals("Event: Hangup")) {
            return transformHangup(event);
        }
        else if (first.equals("BridgeEnter")) {
            HashMap<String, String> thisBridge = eventHasher(event);
            String bridgeID = thisBridge.get("BridgeUniqueid");

            if (!bridgeList.containsKey(bridgeID)) {
                bridgeList.put(bridgeID, thisBridge);
                event.clear();
                event.add("Event: Skip");
                return event;
            }
            else if (bridgeList.containsKey(bridgeID)) {
                bridgeList.put(bridgeID + "-1", thisBridge);
                return transformBridge(thisBridge, bridgeList.get(bridgeID));
            }
        }

        return event;
    }

    public HashMap<String, String> eventHasher ( ArrayList<String> event ) {

        HashMap<String, String> hash = new HashMap<>();
        ArrayList<String> split;

        for (String l : event) {
            split = new ArrayList<>(Arrays.asList(splitFast(l)));
            hash.put(split.get(0), split.get(1));
        }

        return hash;

    }

    public ArrayList<String> transformBridge ( HashMap<String, String> hashedBridge1, HashMap<String, String> hashedBridge2) {

        ArrayList<String> newEvent = new ArrayList<>();

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Bridge");
        newEvent.add("Privilege: " + hashedBridge1.get("Privilege"));
        newEvent.add("Bridgestate: Link");
        newEvent.add("Channel2: " + hashedBridge2.get("Channel"));
        newEvent.add("Channel1: " + hashedBridge1.get("Channel"));
        newEvent.add("Bridgetype: " + hashedBridge1.get("BridgeType"));
        newEvent.add("CallerID2: " + hashedBridge2.get("CallerIDNum"));
        newEvent.add("CallerID1: " + hashedBridge1.get("CallerIDNum"));
        newEvent.add("Uniqueid2: " + hashedBridge2.get("Uniqueid"));
        newEvent.add("Uniqueid1: " + hashedBridge1.get("Uniqueid"));
        newEvent.add("LinkedID1: " + hashedBridge1.get("Linkedid"));
        newEvent.add("LinkedID2: " + hashedBridge2.get("Linkedid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformHangup ( ArrayList<String> event ) {


        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Hangup");
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("LinkedID: " + hashedEvent.get("Linkedid"));
        newEvent.add("Channel: " + hashedEvent.get("Channel"));
        newEvent.add("Uniqueid: " + hashedEvent.get("Uniqueid"));
        newEvent.add("CallerIDNum: " + hashedEvent.get("CallerIDNum"));
        newEvent.add("CallerIDName: " + hashedEvent.get("CallerIDName"));
        newEvent.add("ConnectedLineNum: " + hashedEvent.get("ConnectedLineNum"));
        newEvent.add("ConnectedLineName: " + hashedEvent.get("ConnectedLineName"));
        newEvent.add("Cause: " + hashedEvent.get("Cause"));
        newEvent.add("Cause-txt: " + hashedEvent.get("Cause-txt"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformTransfer ( ArrayList<String> event ) {

        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Transfer");
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("LinkedID1: " + hashedEvent.get("OrigTransfererUniqueid"));
        newEvent.add("LinkedID2: " + hashedEvent.get("TransferTargetUniqueid"));
        if (hashedEvent.get("OrigTransfererChannel").contains("SIP")) {
            newEvent.add("TransferMethod: SIP");
        }
        newEvent.add("TransferType: " + hashedEvent.get("Event").replace("Transfer", ""));
        newEvent.add("Channel: " + hashedEvent.get("OrigTransfererChannel"));
        newEvent.add("Uniqueid: " + hashedEvent.get("OrigTransfererUniqueid"));
        newEvent.add("TargetChannel: " + hashedEvent.get("TransferTargetChannel"));
        newEvent.add("TargetUniqueid: " + hashedEvent.get("TransferTargetUniqueid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformAgentCalled ( ArrayList<String> event ) {

        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: AgentCalled");
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("ChannelCalling: " + hashedEvent.get("Channel"));
        newEvent.add("DestinationChannel: " + hashedEvent.get("DestChannel"));
        newEvent.add("Queue: " + hashedEvent.get("Queue"));
        newEvent.add("AgentCalled: " + hashedEvent.get("Interface"));
        newEvent.add("AgentName: " + hashedEvent.get("MemberName"));
        newEvent.add("Priority: " + hashedEvent.get("Priority"));
        newEvent.add("Extension: " + hashedEvent.get("Exten"));
        newEvent.add("Context: " + hashedEvent.get("Context"));
        newEvent.add("ConnectedLineName: " + hashedEvent.get("ConnectedLineName"));
        newEvent.add("ConnectedLineNum: " + hashedEvent.get("ConnectedLineNum"));
        newEvent.add("CallerIDName: " + hashedEvent.get("CallerIDName"));
        newEvent.add("CallerIDNum: " + hashedEvent.get("CallerIDNum"));
        newEvent.add("Uniqueid: " + hashedEvent.get("Uniqueid"));
        newEvent.add("LinkedID: " + hashedEvent.get("Linkedid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformLeave ( ArrayList<String> event ) {

        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Leave");
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("Channel: " + hashedEvent.get("Channel"));
        newEvent.add("Position: " + hashedEvent.get("Position"));
        newEvent.add("Queue: " + hashedEvent.get("Queue"));
        newEvent.add("Count: " + hashedEvent.get("Count"));
        newEvent.add("Uniqueid: " + hashedEvent.get("Uniqueid"));
        newEvent.add("LinkedID: " + hashedEvent.get("Linkedid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformJoin (ArrayList<String> event) {

        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Join");
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("Channel: " + hashedEvent.get("Channel"));
        newEvent.add("Position: " + hashedEvent.get("Position"));
        newEvent.add("Queue: " + hashedEvent.get("Queue"));
        newEvent.add("Count: " + hashedEvent.get("Count"));
        newEvent.add("ConnectedLineNum: " + hashedEvent.get("ConnectedLineNum"));
        newEvent.add("ConnectedLineName: " + hashedEvent.get("ConnectedLineName"));
        newEvent.add("CallerIDName: " + hashedEvent.get("CallerIDName"));
        newEvent.add("CallerIDNum: " + hashedEvent.get("CallerIDNum"));
        newEvent.add("Uniqueid: " + hashedEvent.get("Uniqueid"));
        newEvent.add("LinkedID: " + hashedEvent.get("Linkedid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformDialEnd (ArrayList<String> event) {

        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Dial");
        newEvent.add("DialStatus: " + hashedEvent.get("DialStatus"));
        newEvent.add("LinkedID: " + hashedEvent.get("Linkedid"));
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("Channel: " + hashedEvent.get("Channel"));
        newEvent.add("SubEvent: End");
        newEvent.add("UniqueID: " + hashedEvent.get("Uniqueid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;

    }

    public ArrayList<String> transformDial (ArrayList<String> event) {

        ArrayList<String> newEvent = new ArrayList<>();
        HashMap<String, String> hashedEvent = eventHasher(event);

        LOGGER.info("");
        LOGGER.info("Translating Event");

        newEvent.add("Event: Dial");
        newEvent.add("Privilege: " + hashedEvent.get("Privilege"));
        newEvent.add("DestUniqueID: " + hashedEvent.get("DestUniqueid"));
        newEvent.add("SubEvent: Begin");
        newEvent.add("Channel: " + hashedEvent.get("Channel"));
        newEvent.add("Destination: " + hashedEvent.get("DestChannel"));
        newEvent.add("Dialstring: " + hashedEvent.get("DialString"));
        newEvent.add("LinkedID1: " + hashedEvent.get("Linkedid"));
        newEvent.add("ConnectedLineName: " + hashedEvent.get("ConnectedLineName"));
        newEvent.add("ConnectedLineNum: " + hashedEvent.get("ConnectedLineNum"));
        newEvent.add("CallerIDName: " + hashedEvent.get("CallerIDName"));
        newEvent.add("LinkedID2: " + hashedEvent.get("Linkedid"));
        newEvent.add("CallerIDNum: " + hashedEvent.get("CallerIDNum"));
        newEvent.add("UniqueID: " + hashedEvent.get("Uniqueid"));

        newEvent.forEach(l -> LOGGER.info(l));

        return newEvent;
    }

    public final String[] splitFast(String string) {

        int index = string.indexOf(':');
        String[] splitResult = { string.substring( 0, index), string.substring(index + 2) };

        return splitResult;
    }

    public final String splitFastFirst(String string) {

        int index = string.indexOf(':');
        String[] splitResult = { string.substring( 0, index), string.substring(index + 2) };

        return splitResult[1];
    }

    public boolean inEventList (String event) {

        for (String s : changedEvents) {
            if (s.equals(event)) {
                return true;
            }
        }
        return false;
    }

}
