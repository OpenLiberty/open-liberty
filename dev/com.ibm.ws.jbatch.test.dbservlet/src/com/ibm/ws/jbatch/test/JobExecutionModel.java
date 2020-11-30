package com.ibm.ws.jbatch.test;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Simple JobExecution impl wrapped around a JsonObject.
 */
public class JobExecutionModel implements JobExecution {

    /**
     * Deserialized json.
     */
    private final JsonObject jsonObject;

    /**
     * CTOR.
     */
    public JobExecutionModel(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String getJobName() {
        return jsonObject.getString("jobName", "");
    }

    @Override
    public long getExecutionId() {
        return jsonObject.getJsonNumber("executionId").longValue();
    }

    @Override
    public BatchStatus getBatchStatus() {
        return BatchStatus.valueOf(jsonObject.getString("batchStatus"));
    }

    @Override
    public Date getStartTime() {
        return BatchDateFormat.parseDate(jsonObject.getString("startTime", null));
    }

    @Override
    public Date getEndTime() {
        return BatchDateFormat.parseDate(jsonObject.getString("endTime", null));
    }

    @Override
    public String getExitStatus() {
        return jsonObject.getString("exitStatus", "");
    }

    @Override
    public Date getCreateTime() {
        return BatchDateFormat.parseDate(jsonObject.getString("createTime", null));
    }

    @Override
    public Date getLastUpdatedTime() {
        return BatchDateFormat.parseDate(jsonObject.getString("lastUpdatedTime", null));
    }

    @Override
    public Properties getJobParameters() {
        return parseProperties(jsonObject.getJsonObject("jobParameters"));
    }

    /**
     * @return a Properties object from the given JsonObject.
     */
    protected Properties parseProperties(JsonObject props) {
        if (props == null) {
            return null;
        }

        Properties retMe = new Properties();

        for (Map.Entry<String, JsonValue> entry : props.entrySet()) {
            retMe.setProperty(entry.getKey(), ((JsonString) entry.getValue()).getString());
        }

        return retMe;
    }

    /**
     * @return Stringified JobExecution record in the form "executionId=<id>,jobName=<name>,..."
     */
    @Override
    public String toString() {
        return StringUtils.join(Arrays.asList(
                                              "executionId=" + String.valueOf(getExecutionId()),
                                              "jobName=" + getJobName(),
                                              "createTime=" + BatchDateFormat.formatDate(getCreateTime()),
                                              "startTime=" + BatchDateFormat.formatDate(getStartTime()),
                                              "endTime=" + BatchDateFormat.formatDate(getEndTime()),
                                              "lastUpdatedTime=" + BatchDateFormat.formatDate(getLastUpdatedTime()),
                                              "batchStatus=" + getBatchStatus(),
                                              "exitStatus=" + getExitStatus(),
                                              "jobParameters=" + getJobParameters()
                        ),
                                ",");
    }

}
