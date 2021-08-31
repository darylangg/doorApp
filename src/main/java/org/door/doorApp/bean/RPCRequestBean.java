package org.door.doorApp.bean;

import com.google.protobuf.InvalidProtocolBufferException;
import org.door.common.protobuf.DoorDataProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class RPCRequestBean {
    private Logger log = LoggerFactory.getLogger(RPCRequestBean.class);
    private DoorDataProto.RPCRequest latestRequest;
    private DoorDataProto.RPCReply latestReply;
    private boolean replied = false;

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public void setCorrelationID(String correlationID) {
        this.correlationID = correlationID;
    }

    private String replyTo;
    private String correlationID;

    private static RPCRequestBean instance = null;

    public synchronized static RPCRequestBean getInstance() {
        if (instance == null) {
            instance = new RPCRequestBean();
        }
        return instance;
    }

    public void processRequest(byte[] data) throws InvalidProtocolBufferException {
        latestRequest = DoorDataProto.RPCRequest.parseFrom(data);
    }

    public void processReply(byte[] data) throws InvalidProtocolBufferException {
        DoorDataProto.RPCReply incReply = DoorDataProto.RPCReply.parseFrom(data);
        if (latestRequest.getRequestID().equals(incReply.getRequestID())){
            replied = true;
            latestReply = incReply;
        }
        latestRequest = null;
    }

    public boolean requestCompleted(){
        return replied;
    }

    public byte[] getLatestReply(){
        byte[] out = latestReply.toByteArray();
        latestReply = null;
        replied = false;
        return out;
    }

    public void JSONtoRequestProto(HashMap<String, List<String>> data){
        for (String groupName : data.keySet()){
            latestRequest = latestRequest.toBuilder().addAllDoorName(data.get(groupName)).build();
        }
    }

    public List<HashMap<String,String>> prepareDBPayload(byte[] data) throws InvalidProtocolBufferException {
        List<HashMap<String,String>> retPayload = new ArrayList<>();
        DoorDataProto.RPCRequest request = DoorDataProto.RPCRequest.parseFrom(data);
        List<String> doorList = request.getDoorNameList();
        for (String doorName : doorList){
            HashMap<String,String> body = new HashMap<>();
            body.put("request_id", request.getRequestID() + "_" + doorName);
            body.put("device_id", doorName);
            body.put("user_id", "daryl");
            body.put("door_status", request.getStatusToSet().toString());
            retPayload.add(body);
        }
        return retPayload;
    }

    public boolean isGroupQuery(){
        return latestRequest.getDoorNameList().size() == 0;
    }

    public String getGroupName(){
        return latestRequest.getGroupName();
    }

    public byte[] getLatestRequest(){
        return latestRequest.toByteArray();
    }
}
