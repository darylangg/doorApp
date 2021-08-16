package org.door.common.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.door.doorApp.bean.DoorFilterBean;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QueuePropertiesReader {
    private static String properties= "";
    private static QueuePropertiesReader instance = null;

    public synchronized static QueuePropertiesReader getInstance() throws IOException {
        if (instance == null) {
            instance = new QueuePropertiesReader();
            properties = properties + setProperties();
        }
        return instance;
    }

    public static String setProperties() throws IOException {
        String propertiesString = "";
        try {
            HashMap result = new ObjectMapper().readValue(new File("src/main/java/org/door/doorApp/configuration/queueProperties.json"), HashMap.class);
            for (Object key : result.keySet()){
                String propertyName = (String) key;
                String propertyVal = (String) result.get(key);
                propertiesString = propertiesString + "&" + propertyName + "=" + propertyVal;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return propertiesString;
    }

    public String getProperties(){
        return properties;
    }
}
