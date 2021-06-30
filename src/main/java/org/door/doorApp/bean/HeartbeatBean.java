package org.door.doorApp.bean;

import org.door.common.protobuf.HeartbeatProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class HeartbeatBean {
    private static Logger log = LoggerFactory.getLogger(HeartbeatBean.class);
    private static HeartbeatBean instance = null;

    private enum services{
        EXTERNAL_API,
        WEB,
        HAPI_GENERAL_API,
        HAPI_VERTICAL_API
    }

    // map services to {"timestamp": xxx , "connected" : true}
    private static HashMap<services, HashMap<String, Object>> latestHeartbeats = new HashMap<services, HashMap<String, Object>>();

    public synchronized static HeartbeatBean getInstance() {
        if (instance == null) {
            instance = new HeartbeatBean();

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            for (services service : services.values()){
                HashMap<String, Object> heartbeatEntry = new HashMap<String,Object>();
                heartbeatEntry.put("timestamp", timestamp.getTime());
                heartbeatEntry.put("connected", false);
                latestHeartbeats.put(service, heartbeatEntry);
            }
        }
        updateHeartbeats();
        return instance;
    }

    public boolean servicesConnected(){
        boolean result = true;
        for (services service: latestHeartbeats.keySet()){
            boolean connected = (boolean) latestHeartbeats.get(service).get("connected");
            if (!connected){
                log.info("Service disconnected: " + service);
                result = false;
            }
        }
        return result;
    }

    public static void updateHeartbeats(){
        long threshold = 10000; // heartbeat expiry in milliseconds
        for (services service: latestHeartbeats.keySet()){
            long lastTimestamp =(long) latestHeartbeats.get(service).get("timestamp");
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            long currentTimestamp = timestamp.getTime();

            if (currentTimestamp - lastTimestamp > threshold){ // reset the connection
                latestHeartbeats.get(service).put("timestamp", currentTimestamp);
                latestHeartbeats.get(service).put("connected", false);
            }
        }
    }

    public void processHeartbeatProto(HeartbeatProto.Heartbeat incHeartbeat){
        long currentTimestamp = incHeartbeat.getTimestamp();
        Map<String, HeartbeatProto.ConnectionStatus> connectionMapping = incHeartbeat.getConnectionsMap();
        for (services service: services.values()){
            String serviceString = service.toString();
            if (connectionMapping.containsKey(serviceString)){
                latestHeartbeats.get(service).put("timestamp", currentTimestamp);
                latestHeartbeats.get(service).put("connected", connectionMapping.get(serviceString).equals(HeartbeatProto.ConnectionStatus.CONNECTED));
            }
        }
    }

    public void processHeartbeat(String serviceString){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        services service = services.valueOf(serviceString);
        latestHeartbeats.get(service).put("timestamp", timestamp.getTime());
        latestHeartbeats.get(service).put("connected", true);
    }

    public byte[] packHeartbeatProto(){
        HeartbeatProto.Heartbeat.Builder heartbeatBuilder = HeartbeatProto.Heartbeat.newBuilder();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        heartbeatBuilder.setTimestamp(timestamp.getTime());
        heartbeatBuilder.putConnections("APP", HeartbeatProto.ConnectionStatus.CONNECTED);

        for (services service : latestHeartbeats.keySet()){
            heartbeatBuilder.putConnections(service.toString(), ((boolean )latestHeartbeats.get(service).get("connected") ? HeartbeatProto.ConnectionStatus.CONNECTED : HeartbeatProto.ConnectionStatus.DISCONNECTED));
        }
        return heartbeatBuilder.build().toByteArray();
    }
}
