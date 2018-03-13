/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import com.ibm.ws.logging.data.KeyValuePairList;
import com.ibm.ws.logging.data.Pair;

/**
 *
 */
public class CollectorJsonUtils1_1 {

    public static final int MAX_USER_AGENT_LENGTH = 2048;

    public static String getEventType(String source, String location) {
        return CollectorJsonHelpers.getEventType(source, location);
    }

    /**
     * Method to return log event data in json format. This method is for collector version greater than 1.0
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
    public static String jsonifyEvent(Object event, String eventType, String serverName, String wlpUserDir, String serverHostName, String[] tags,
                                      int maxFieldLength) {

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
        return "";

    }

    private static String jsonifyGCEvent(String hostName, String wlpUserDir, String serverName, HCGCData hcGCData, String[] tags) {
        String sequenceNum = hcGCData.getSequence();

        //                                           name        value     jsonEscapeName? jsonEscapeValue? trim?   isFirst?
        /* Common fields for all event types */
        StringBuilder sb = CollectorJsonHelpers.startGCJson1_1(hostName, wlpUserDir, serverName);
        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(hcGCData.getTime());
        CollectorJsonHelpers.addToJSON(sb, "ibm_datetime", datetime, false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, "ibm_sequence", sequenceNum, false, false, false, false);
        /* GC specific fields */
        CollectorJsonHelpers.addToJSON(sb, "ibm_heap", String.valueOf((long) hcGCData.getHeap()), false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, "ibm_usedHeap", String.valueOf((long) hcGCData.getUsage()), false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, "ibm_maxHeap", String.valueOf(hcGCData.getMaxHeap()), false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, "ibm_duration", String.valueOf((long) hcGCData.getDuration() * 1000), false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, "ibm_gcType", hcGCData.getType(), false, false, false, false);
        CollectorJsonHelpers.addToJSON(sb, "ibm_reason", hcGCData.getReason(), false, false, false, false);

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyGCEvent(int maxFieldLength, String wlpUserDir,
                                         String serverName, String hostName, String eventType, Object event, String[] tags) {
        GenericData genData = (GenericData) event;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        StringBuilder sb = CollectorJsonHelpers.startGCJson1_1(hostName, wlpUserDir, serverName);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.IBM_DURATION)) {

                    long duration = Long.parseLong(value) * 1000;
                    CollectorJsonHelpers.addToJSON(sb, key, Long.toString(duration), false, true, false, false, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                    CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, false, false);

                } else {

                    CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, false, kvp.isNumber());

                }
            }

            if (tags != null) {
                addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
            }
        }
        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyFFDC(int maxFieldLength, String wlpUserDir,
                                      String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        StringBuilder sb = CollectorJsonHelpers.startFFDCJson1_1(hostName, wlpUserDir, serverName);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (!key.equals(LogFieldConstants.LABEL) && !(key.equals(LogFieldConstants.SOURCEID))
                    && !(key.equals(LogFieldConstants.DATEOFFIRSTOCCURENCE)) && !(key.equals(LogFieldConstants.COUNT))) {

                    if (key.equals(LogFieldConstants.IBM_STACKTRACE)) {

                        String formattedValue = CollectorJsonHelpers.formatMessage(value, maxFieldLength);
                        CollectorJsonHelpers.addToJSON(sb, key, formattedValue, false, true, false, false, kvp.isNumber());

                    } else if (key.equals(LogFieldConstants.IBM_THREADID)) {

                        CollectorJsonHelpers.addToJSON(sb, key, DataFormatHelper.padHexString(Integer.parseInt(value), 8), false, true, false, false, false);

                    } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                        String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                        CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, false, false);

                    } else {

                        CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, false, kvp.isNumber());

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

    public static String jsonifyAccess(int maxFieldLength, String wlpUserDir,
                                       String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        ArrayList<Pair> pairs = genData.getPairs();
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        StringBuilder sb = CollectorJsonHelpers.startAccessLogJson1_1(hostName, wlpUserDir, serverName);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.IBM_REQUESTSTARTTIME)) {

                } else if (key.equals(LogFieldConstants.IBM_QUERYSTRING)) {

                    String jsonQueryString = value;
                    if (jsonQueryString != null) {
                        try {
                            jsonQueryString = URLDecoder.decode(jsonQueryString, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // ignore, use the original value;
                        }
                    }
                    CollectorJsonHelpers.addToJSON(sb, key, jsonQueryString, false, true, false, false, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_USERAGENT)) {

                    String userAgent = value;

                    if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
                        userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
                    }

                    CollectorJsonHelpers.addToJSON(sb, key, userAgent, false, false, false, false, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                    CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, false, false);

                } else {

                    CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, false, kvp.isNumber());

                }
            }
        }

        if (tags != null) {
            addTagNameForVersion(sb).append(CollectorJsonHelpers.jsonifyTags(tags));
        }

        sb.append("}");

        return sb.toString();
    }

    private static String jsonifyTraceAndMessage(int maxFieldLength, String wlpUserDir,
                                                 String serverName, String hostName, String eventType, Object event, String[] tags) {

        GenericData genData = (GenericData) event;
        StringBuilder sb = null;
        ArrayList<Pair> pairs = genData.getPairs();
        ArrayList<KeyValuePair> extensions = null;
        KeyValuePairList kvpl = null;
        KeyValuePair kvp = null;
        String key = null;
        String value = null;

        if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE))
            sb = CollectorJsonHelpers.startMessageJson1_1(hostName, wlpUserDir, serverName);
        if (eventType.equals(CollectorConstants.TRACE_LOG_EVENT_TYPE))
            sb = CollectorJsonHelpers.startTraceJson1_1(hostName, wlpUserDir, serverName);

        for (Pair p : pairs) {

            if (p instanceof KeyValuePair) {

                kvp = (KeyValuePair) p;
                key = kvp.getKey();
                value = kvp.getValue();

                if (key.equals(LogFieldConstants.SEVERITY)) {
                }

                else if (key.equals(LogFieldConstants.MESSAGE)) {

                    String formattedValue = CollectorJsonHelpers.formatMessage(value, maxFieldLength);
                    CollectorJsonHelpers.addToJSON(sb, key, formattedValue, false, true, false, false, kvp.isNumber());

                } else if (key.equals(LogFieldConstants.IBM_THREADID)) {

                    CollectorJsonHelpers.addToJSON(sb, key, DataFormatHelper.padHexString(Integer.parseInt(value), 8), false, true, false, false,
                                                   false);

                } else if (key.equals(LogFieldConstants.IBM_DATETIME)) {

                    String datetime = CollectorJsonHelpers.dateFormatTL.get().format(Long.parseLong(value));
                    CollectorJsonHelpers.addToJSON(sb, key, datetime, false, true, false, false, false);

                } else {

                    CollectorJsonHelpers.addToJSON(sb, key, value, false, true, false, false, kvp.isNumber());

                }
            } else if (p instanceof KeyValuePairList) {
                kvpl = (KeyValuePairList) p;
                if (kvpl.getName().equals(LogFieldConstants.EXTENSIONS_KVPL)) {
                    extensions = kvpl.getKeyValuePairs();
                    boolean isExtQuoteless = false;
                    for (KeyValuePair k : extensions) {
                        String extKey = k.getKey();
                        String extValue = k.getValue();
                        // Checking the value of the extension KeyValuePair to make sure the suffix is correct
                        boolean isValidExt = CollectorJsonHelpers.checkExtSuffixValidity(k);
                        if (isValidExt) {
                            isExtQuoteless = CollectorJsonHelpers.checkIfExtIsQuoteless(extKey);
                            extKey = LogFieldConstants.EXT_PREFIX + extKey;
                            isFirstField = isFirstField & !CollectorJsonHelpers.addToJSON(sb, extKey, extValue, false, true, false, isFirstField, isExtQuoteless);
                        }
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

    private static StringBuilder addTagNameForVersion(StringBuilder sb) {

        sb.append(",\"ibm_tags\":");

        return sb;
    }

}