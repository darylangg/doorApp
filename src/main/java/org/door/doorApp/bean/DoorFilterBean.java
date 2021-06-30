package org.door.doorApp.bean;

import com.google.protobuf.InvalidProtocolBufferException;
import org.door.common.protobuf.DoorDataProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DoorFilterBean {
    private Logger log = LoggerFactory.getLogger(DoorFilterBean.class);

    private DoorDataProto.DoorData outgoingData = null;

    private HashMap<String, DoorDataProto.DoorReading> mappings = new HashMap<>();

//    private HashMap<String, Long> lastHeartBeats = new HashMap<>();
    private static DoorFilterBean instance = null;

    public synchronized static DoorFilterBean getInstance() {
        if (instance == null) {
            instance = new DoorFilterBean();
        }
        return instance;
    }

    public void checkIfNew(byte[] data) throws InvalidProtocolBufferException {
        DoorDataProto.DoorData incomingData = DoorDataProto.DoorData.parseFrom(data);
        List<DoorDataProto.DoorReading> incomingDataReadings = incomingData.getDoorReadingList();

        //TODO implement heartbeat
//        HeartbeatBean.getInstance().updateSystemHeartbeat(incomingData);

        // initialization
        if (mappings.size() == 0){
            outgoingData = incomingData;
            for (DoorDataProto.DoorReading reading: incomingDataReadings) {
                String id = reading.getDoorName();
                mappings.put(id, reading);
            }
        }
        else {
            // get all new data
            List<DoorDataProto.DoorReading> newData = new ArrayList<>();

            // check against existing data
            for (DoorDataProto.DoorReading incomingReading: incomingDataReadings) {
                String id = incomingReading.getDoorName();
                DoorDataProto.DoorReading existingReading = mappings.get(id);

                // if they are the same
                if (incomingReading.equals(existingReading)){
//                    log.info("No change for Door "+ id);
                } else {
                    // not the same
                    newData.add(incomingReading);
                    mappings.put(id, incomingReading);
                }

            }

            // handle new data
            if (newData.size() > 0){
                // simulate error writing
                outgoingData = getLatestDataFromNewArr(newData, incomingData.getTimestamp(), incomingData.getStatusCode());
            }
        }
    }

    public boolean hasUpdates(){
        return outgoingData != null;
    }

    public void clearUpdates(){
        outgoingData = null;
    }

    public byte[] getLatestUpdate(){
        byte[] retData = outgoingData.toByteArray();
        clearUpdates();
        return retData;
    }

    public void setOutgoingData(DoorDataProto.DoorData incomingData){
        outgoingData = incomingData;
    }

    public HashMap<String, DoorDataProto.DoorReading> getMappings() {
        return mappings;
    }

    public DoorDataProto.DoorData getLatestDataFromNewArr(List<DoorDataProto.DoorReading> incReadings, long timestamp, long statusCode){
        DoorDataProto.DoorData.Builder retDataBuilder = DoorDataProto.DoorData.newBuilder()
                .setTimestamp(timestamp)
                .setStatusCode(statusCode);
        for (DoorDataProto.DoorReading newIncReading: incReadings) {
            retDataBuilder.addDoorReading(newIncReading);
        }
        return retDataBuilder.build();
    }
}
