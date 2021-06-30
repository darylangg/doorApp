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

    private static RPCRequestBean instance = null;

    public synchronized static RPCRequestBean getInstance() {
        if (instance == null) {
            instance = new RPCRequestBean();
        }
        return instance;
    }

    public void processRequest(byte[] data) throws InvalidProtocolBufferException {
        long now = Instant.now().toEpochMilli()/1000;
        String requestID = "door_"+now;
        latestRequest = DoorDataProto.RPCRequest.parseFrom(data);
        latestRequest = latestRequest.toBuilder()
                .setRequestID(requestID)
                .build();
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
