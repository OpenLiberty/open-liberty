package com.ibm.ws.jbatch.test;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.batch.runtime.BatchStatus;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

/**
 * Helper methods for converting BatchStatus to JSON.
 * 
 * Note: this class is duplicated in com.ibm.ws.jbatch.rest
 */
public class JsonHelper {

    /**
     * @return batchStatus.name(), or null if batchStatus == null.
     */
    public static String getName(BatchStatus batchStatus) {
        return (batchStatus != null) ? batchStatus.name() : null;
    }

    /**
     * @return the BatchStatus with the given null, or null if batchStatusName == null.
     */
    public static BatchStatus valueOfBatchStatus(String batchStatusName) {
        return (!StringUtils.isEmpty(batchStatusName)) ? BatchStatus.valueOf(batchStatusName) : null;
    }

    /**
     * @return a copy of the given jsonObject with the given fields removed.
     * 
     */
    public static JsonObject removeFields(JsonObject jsonObject, String... removeField) {
        JsonObjectBuilder retMe = Json.createObjectBuilder();

        List<String> removeFieldList = Arrays.asList(removeField);

        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if (!removeFieldList.contains(entry.getKey())) {
                retMe.add(entry.getKey(), entry.getValue());
            }
        }

        return retMe.build();
    }

    /**
     * @return the JsonObject read from the given stream.
     */
    public static JsonObject readJsonObject(Reader reader) {
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        return jsonObject;
    }
}