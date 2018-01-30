/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.health.center.data.HCGCData;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.Pair;

/*
 * Utility class for converting events into JSON strings
 * Logstash and logmet collector use these methods for converting the relevant
 * events into json strings
 */
public class CollectorJsonUtils {

    public static final int MAX_USER_AGENT_LENGTH = 2048;

    public static String getEventType(String source, String location) {
        return CollectorJsonHelpers.getEventType(source, location);
    }

    /**
     * Method to return log event data in json format. If the collector version passed is greater than 1.0
     * then the jsonifyEvent call is passed to another version of CollectorJsonUtils.
     *
     * @param event The object originating from logging source which contains necessary fields
     * @param eventType The type of event
     * @param servername The name of the server
     * @param wlpUserDir The name of wlp user directory
     * @param serverHostName The name of server host
     * @param collectorVersion The version number
     * @param tags An array of tags
     * @param maxFieldLength The max character length of strings
     */
    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String collectorVersion, String[] tags,
                                      int maxFieldLength) {

        if (!collectorVersion.equals("1.0")) {

            return CollectorJsonUtils1_1.jsonifyEvent(event, eventType, serverName, wlpUserDir, serverHostName, tags, maxFieldLength);

        } else {

            if (eventType.equals(CollectorConstants.GC_EVENT_TYPE)) {

                if (event instanceof GenericData) {

                    return jsonifyGCEvent(-1, wlpUserDir, serverName, serverHostName, CollectorConstants.GC_EVENT_TYPE, event, tags);

                } else {

                    return jsonifyGCEvent(serverHostName, wlpUserDir, serverName, (HCGCData) event, tags);

                }

            } else if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {

                return jsonifyTraceAndMessage(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.MESSAGES_LOG_EVENT_TYPE, event, tags);

            } else if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE)) {

                return jsonifyTraceAndMessage(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.TRACE_LOG_EVENT_TYPE, event, tags);

            } else if (eventType.equals(CollectorConstants.FFDC_EVENT_TYPE)) {

                return jsonifyFFDC(maxFieldLength, wlpUserDir, serverName, serverHostName, CollectorConstants.FFDC_EVENT_TYPE, event, tags);

            } else if (eventType.equals(CollectorConstants.ACCESS_LOG_EVENT_TYPE)) {

                return jsonifyAccess(-1, wlpUserDir, serverName, serverHostName, CollectorConstants.ACCESS_LOG_EVENT_TYPE, event, tags);

            }
        }
        return "";
    }

    private static String jsonifyGCEvent(String hostName, String wlpUserDir, String serverName, HCGCData hcGCData, String[] tags) {
        String sequenceNum = hcGCData.getSequence();
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;

        sb.append("{");

        //                                           name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        /* Common fields for all event types */
        isFirstField = CollectorJsonHelpers.addCommonFieldsGC(sb, hostName, wlpUserDir, serverName, hcGCData.getTime(), sequenceNum, isFirstField,
                                                              CollectorConstants.GC_EVENT_TYPE);
        /* GC specific fields */
        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, "heap", String.valueOf((long) hcGCData.getHeap()), false, false, false, isFirstField);
        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, "usedHeap", String.valueOf((long) hcGCData.getUsage()), false, false, false, isFirstField);
        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, "maxHeap", String.valueOf(hcGCData.getMaxHeap()), false, false, false, isFirstField);
        isFirstField = isFirstField
                       & !CollectorJsonHelpers.addToJSON(sb, "duration", String.valueOf((long) hcGCData.getDuration() * 1000), false, false, false, isFirstField);
        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, "gcType", hcGCData.getType(), false, false, false, isFirstField);
        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, "reason", hcGCData.getReason(), false, false, false, isFirstField);

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyGCEvent(int maxFieldLength, String wlpUserDir,
                                         String serverName, String hostName, String eventType, Object event, String[] tags) {
        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = CollectorJsonHelpers.addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.IBM_DURATION)) {

                    key = LogFieldConstants.DURATION;
                    long duration = Long.parseLong(value) * 1000;
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, Long.toString(duration), false, true, false, isFirstField, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    key = LogFieldConstants.DATETIME;
                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, isFirstField, false);

                } else {
                    if (key.contains(LogFieldConstants.IBM_TAG)) {
                        key = CollectorJsonHelpers.removeIBMTag(key);
                    }
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, isFirstField, kvp.isNumber());
                }
            }

            if (tags != null) {
                addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
            }
        }
        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyTraceAndMessage(int maxFieldLength, String wlpUserDir,
                                                 String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = CollectorJsonHelpers.addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.LOGLEVEL) || key.equals(LogFieldConstants.COMPONENT) || key.equals(LogFieldConstants.CORRELATION_ID)
                    || key.equals(LogFieldConstants.THREADNAME) || key.equals(LogFieldConstants.LEVELVALUE) || key.equals(LogFieldConstants.PRODUCT)
                    || key.equals(LogFieldConstants.ORG)) {
                }

                else if (key.equals(LogFieldConstants.MESSAGE)) {

                    String formattedValue = CollectorJsonHelpers.formatMessage(value, maxFieldLength);
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, formattedValue, false, true, false, isFirstField, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_THREADID)) {
                    key = LogFieldConstants.THREADID;
                    isFirstField = isFirstField
                                   & !CollectorJsonHelpers.addToJSON(sb, key, DataFormatHelper.padHexString(Integer.parseInt(value), 8), false, true, false, isFirstField,
                                                                     kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {
                    key = LogFieldConstants.DATETIME;
                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, isFirstField, false);

                } else if (key.equals(LogFieldConstants.MODULE)) {
                    key = LogFieldConstants.LOGGERNAME;
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, isFirstField, kvp.isNumber());
                } else {
                    if (key.contains(LogFieldConstants.IBM_TAG)) {
                        key = CollectorJsonHelpers.removeIBMTag(key);
                    }
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, isFirstField, kvp.isNumber());

                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyFFDC(int maxFieldLength, String wlpUserDir,
                                      String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = CollectorJsonHelpers.addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (!key.equals(LogFieldConstants.LABEL) && !(key.equals(LogFieldConstants.SOURCEID))) {

                    if (key.equals(LogFieldConstants.IBM_STACKTRACE)) {
                        key = LogFieldConstants.STACKTRACE;
                        String formattedValue = CollectorJsonHelpers.formatMessage(value, maxFieldLength);
                        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, formattedValue, false, true, false, isFirstField, kvp.isNumber());

                    } else if (key.equals(LogFieldConstants.IBM_THREADID)) {
                        key = LogFieldConstants.THREADID;
                        isFirstField = isFirstField
                                       & !CollectorJsonHelpers.addToJSON(sb, key, DataFormatHelper.padHexString(Integer.parseInt(value), 8), false, true, false, isFirstField,
                                                                         kvp.isNumber());

                    } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {
                        key = LogFieldConstants.DATETIME;
                        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, isFirstField, false);

                    } else {
                        if (key.contains(LogFieldConstants.IBM_TAG)) {
                            key = CollectorJsonHelpers.removeIBMTag(key);
                        }
                        isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, isFirstField, kvp.isNumber());

                    }
                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyAccess(int maxFieldLength, String wlpUserDir,
                                        String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = new StringBuilder();
        boolean isFirstField = true;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        sb.append("{");

        isFirstField = CollectorJsonHelpers.addCommonFields(sb, hostName, wlpUserDir, serverName, isFirstField, eventType);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.IBM_QUERYSTRING)) {

                    key = LogFieldConstants.QUERYSTRING;
                    String jsonQueryString = value;
                    if (jsonQueryString != null) {
                        try {
                            jsonQueryString = URLDecoder.decode(jsonQueryString, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // ignore, use the original value;
                        }
                    }
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, jsonQueryString, false, true, false, isFirstField, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_USERAGENT)) {

                    key = LogFieldConstants.USERAGENT;
                    String userAgent = value;

                    if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
                        userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
                    }

                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, userAgent, false, false, false, isFirstField, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    key = LogFieldConstants.DATETIME;
                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, isFirstField, false);

                } else {

                    key = CollectorJsonHelpers.removeIBMTag(key);

                    isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, isFirstField, kvp.isNumber());

                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static StringBuilder addTagNameForVersion(StringBuilder sb) {

        sb.append(",\"tags\":");

        return sb;
    }
}