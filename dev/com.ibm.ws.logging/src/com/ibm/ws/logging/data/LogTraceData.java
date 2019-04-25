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
package com.ibm.ws.logging.data;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.utils.SequenceNumber;

/**
 *
 */
public class LogTraceData extends GenericData {

    static Pattern messagePattern;
    private long rawSequenceNumber = -1;

    static {
        messagePattern = Pattern.compile("^([A-Z][\\dA-Z]{3,4})(\\d{4})([A-Z])(:)");
    }

    private final static String[] NAMES1_1 = {
                                               LogFieldConstants.IBM_DATETIME,
                                               LogFieldConstants.IBM_MESSAGEID,
                                               LogFieldConstants.IBM_THREADID,
                                               LogFieldConstants.MODULE,
                                               LogFieldConstants.SEVERITY,
                                               LogFieldConstants.LOGLEVEL,
                                               LogFieldConstants.IBM_METHODNAME,
                                               LogFieldConstants.IBM_CLASSNAME,
                                               LogFieldConstants.LEVELVALUE,
                                               LogFieldConstants.THREADNAME,
                                               LogFieldConstants.CORRELATION_ID,
                                               LogFieldConstants.ORG,
                                               LogFieldConstants.PRODUCT,
                                               LogFieldConstants.COMPONENT,
                                               LogFieldConstants.IBM_SEQUENCE,
                                               LogFieldConstants.THROWABLE,
                                               LogFieldConstants.THROWABLE_LOCALIZED,
                                               LogFieldConstants.MESSAGE,
                                               LogFieldConstants.FORMATTEDMSG,
                                               LogFieldConstants.EXTENSIONS_KVPL,
                                               LogFieldConstants.OBJECT_ID
    };

    private final static String[] NAMES = {
                                            LogFieldConstants.DATETIME,
                                            LogFieldConstants.MESSAGEID,
                                            LogFieldConstants.THREADID,
                                            LogFieldConstants.LOGGERNAME,
                                            LogFieldConstants.SEVERITY,
                                            LogFieldConstants.LOGLEVEL,
                                            LogFieldConstants.METHODNAME,
                                            LogFieldConstants.CLASSNAME,
                                            LogFieldConstants.LEVELVALUE,
                                            LogFieldConstants.THREADNAME,
                                            LogFieldConstants.CORRELATION_ID,
                                            LogFieldConstants.ORG,
                                            LogFieldConstants.PRODUCT,
                                            LogFieldConstants.COMPONENT,
                                            LogFieldConstants.SEQUENCE,
                                            LogFieldConstants.THROWABLE,
                                            LogFieldConstants.THROWABLE_LOCALIZED,
                                            LogFieldConstants.MESSAGE,
                                            LogFieldConstants.FORMATTEDMSG,
                                            LogFieldConstants.EXTENSIONS_KVPL,
                                            LogFieldConstants.OBJECT_ID
    };

    public LogTraceData() {
        super(21);
    }

    private void setPair(int index, String s) {
        setPair(index, NAMES1_1[index], s);
    }

    private void setPair(int index, int i) {
        setPair(index, NAMES1_1[index], i);
    }

    private void setPair(int index, long l) {
        setPair(index, NAMES1_1[index], l);
    }

    private KeyValuePairList getValues(int index) {
        return (KeyValuePairList) getPairs()[index];
    }

    public void setDatetime(long l) {
        setPair(0, l);
    }

    public void setMessageId(String s) {
        setPair(1, s);
    }

    public void setThreadId(int i) {
        setPair(2, i);
    }

    public void setModule(String s) {
        setPair(3, s);
    }

    public void setSeverity(String s) {
        setPair(4, s);
    }

    public void setLoglevel(String s) {
        setPair(5, s);
    }

    public void setMethodName(String s) {
        setPair(6, s);
    }

    public void setClassName(String s) {
        setPair(7, s);
    }

    public void setLevelValue(int i) {
        setPair(8, i);
    }

    public void setThreadName(String s) {
        setPair(9, s);
    }

    public void setCorrelationId(String s) {
        setPair(10, s);
    }

    public void setOrg(String s) {
        setPair(11, s);
    }

    public void setProduct(String s) {
        setPair(12, s);
    }

    public void setComponent(String s) {
        setPair(13, s);
    }

    public void setSequence(String s) {
        setPair(14, s);
    }

    public void setThrowable(String s) {
        setPair(15, s);
    }

    public void setThrowableLocalized(String s) {
        setPair(16, s);
    }

    public void setMessage(String s) {
        setPair(17, s);
    }

    public void setFormattedMsg(String s) {
        setPair(18, s);
    }

    public void setExtensions(KeyValuePairList kvps) {
        setPair(19, kvps);
    }

    public void setObjectId(int i) {
        setPair(20, i);
    }

    public String getDatetimeKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.DATETIME) == null) {
            //return default value
            return NAMES[0];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.DATETIME);
        }
    }

    public String getMessageIdKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.MESSAGEID) == null) {
            //return default value
            return NAMES[1];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.MESSAGEID);
        }
    }

    public String getThreadIdKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THREADID) == null) {
            //return default value
            return NAMES[2];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THREADID);
        }
    }

    public String getModuleKey() {
        return NAMES[3];
    }

    public String getSeverityKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.SEVERITY) == null) {
            //return default value
            return NAMES[4];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.SEVERITY);
        }
    }

    public String getLoglevelKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.LOGLEVEL) == null) {
            //return default value
            return NAMES[5];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.LOGLEVEL);
        }
    }

    public String getMethodNameKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.METHODNAME) == null) {
            //return default value
            return NAMES[6];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.METHODNAME);
        }
    }

    public String getClassNameKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.CLASSNAME) == null) {
            //return default value
            return NAMES[7];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.CLASSNAME);
        }
    }

    public String getLevelValueKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.LEVELVALUE) == null) {
            //return default value
            return NAMES[8];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.LEVELVALUE);
        }
    }

    public String getThreadNameKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THREADNAME) == null) {
            //return default value
            return NAMES[9];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THREADNAME);
        }
    }

    public String getCorrelationIdKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.CORRELATION_ID) == null) {
            //return default value
            return NAMES[10];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.CORRELATION_ID);
        }
    }

    public String getOrgKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.ORG) == null) {
            //return default value
            return NAMES[11];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.ORG);
        }
    }

    public String getProductKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.PRODUCT) == null) {
            //return default value
            return NAMES[12];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.PRODUCT);
        }
    }

    public String getComponentKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.COMPONENT) == null) {
            //return default value
            return NAMES[13];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.COMPONENT);
        }
    }

    public String getSequenceKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.SEQUENCE) == null) {
            //return default value
            return NAMES[14];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.SEQUENCE);
        }
    }

    public String getThrowableKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THROWABLE) == null) {
            //return default value
            return NAMES[15];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THROWABLE);
        }
    }

    public String getThrowableLocalizedKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THROWABLE_LOCALIZED) == null) {
            //return default value
            return NAMES[16];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THROWABLE_LOCALIZED);
        }
    }

    public String getMessageKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.MESSAGE) == null) {
            //return default value
            return NAMES[17];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.MESSAGE);
        }
    }

    public String getFormattedMsgKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.FORMATTEDMSG) == null) {
            //return default value
            return NAMES[18];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.FORMATTEDMSG);
        }
    }

    public String getExtensionsKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.EXTENSIONS_KVPL) == null) {
            //return default value
            return NAMES[19];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.EXTENSIONS_KVPL);
        }
    }

    public String getObjectIdKey() {
        if (nameMap == null || nameMap.get(LogFieldConstants.OBJECT_ID) == null) {
            //return default value
            return NAMES[20];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.OBJECT_ID);
        }
    }

    public String getDatetimeKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.IBM_DATETIME) == null) {
            //return default value
            return NAMES1_1[0];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.IBM_DATETIME);
        }
    }

    public String getMessageIdKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.IBM_MESSAGEID) == null) {
            //return default value
            return NAMES1_1[1];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.IBM_MESSAGEID);
        }
    }

    public String getThreadIdKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.IBM_THREADID) == null) {
            //return default value
            return NAMES1_1[2];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.IBM_THREADID);
        }
    }

    public String getModuleKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.MODULE) == null) {
            //return default value
            return NAMES1_1[3];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.MODULE);
        }
    }

    public String getSeverityKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.SEVERITY) == null) {
            //return default value
            return NAMES1_1[4];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.SEVERITY);
        }
    }

    public String getLoglevelKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.LOGLEVEL) == null) {
            //return default value
            return NAMES1_1[5];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.LOGLEVEL);
        }
    }

    public String getMethodNameKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.IBM_METHODNAME) == null) {
            //return default value
            return NAMES1_1[6];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.IBM_METHODNAME);
        }
    }

    public String getClassNameKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.IBM_CLASSNAME) == null) {
            //return default value
            return NAMES1_1[7];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.IBM_CLASSNAME);
        }
    }

    public String getLevelValueKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.LEVELVALUE) == null) {
            //return default value
            return NAMES1_1[8];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.LEVELVALUE);
        }
    }

    public String getThreadNameKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THREADNAME) == null) {
            //return default value
            return NAMES1_1[9];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THREADNAME);
        }
    }

    public String getCorrelationIdKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.CORRELATION_ID) == null) {
            //return default value
            return NAMES1_1[10];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.CORRELATION_ID);
        }
    }

    public String getOrgKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.ORG) == null) {
            //return default value
            return NAMES1_1[11];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.ORG);
        }
    }

    public String getProductKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.PRODUCT) == null) {
            //return default value
            return NAMES1_1[12];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.PRODUCT);
        }
    }

    public String getComponentKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.COMPONENT) == null) {
            //return default value
            return NAMES1_1[13];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.COMPONENT);
        }
    }

    public String getSequenceKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.IBM_SEQUENCE) == null) {
            //return default value
            return NAMES1_1[14];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.IBM_SEQUENCE);
        }
    }

    public String getThrowableKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THROWABLE) == null) {
            //return default value
            return NAMES1_1[15];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THROWABLE);
        }
    }

    public String getThrowableLocalizedKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.THROWABLE_LOCALIZED) == null) {
            //return default value
            return NAMES1_1[16];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.THROWABLE_LOCALIZED);
        }
    }

    public String getMessageKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.MESSAGE) == null) {
            //return default value
            return NAMES1_1[17];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.MESSAGE);
        }
    }

    public String getFormattedMsgKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.FORMATTEDMSG) == null) {
            //return default value
            return NAMES1_1[18];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.FORMATTEDMSG);
        }
    }

    public String getExtensionsKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.EXTENSIONS_KVPL) == null) {
            //return default value
            return NAMES1_1[19];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.EXTENSIONS_KVPL);
        }
    }

    public String getObjectIdKey1_1() {
        if (nameMap == null || nameMap.get(LogFieldConstants.OBJECT_ID) == null) {
            //return default value
            return NAMES1_1[20];
        } else {
            //return mapped value
            return nameMap.get(LogFieldConstants.OBJECT_ID);
        }
    }

    public long getDatetime() {
        return getLongValue(0);
    }

    public String getMessageId() {
        String messageId = getStringValue(1);
        if (messageId == null || messageId.isEmpty()) {
            String message = getMessage();
            if (message != null) {
                messageId = parseMessageId(message);
                this.setPair(1, NAMES1_1[1], messageId);
            }
        }
        return messageId;
    }

    public int getThreadId() {
        return getIntValue(2);
    }

    public String getModule() {
        return getStringValue(3);
    }

    public String getSeverity() {
        return getStringValue(4);
    }

    public String getLoglevel() {
        return getStringValue(5);
    }

    public String getMethodName() {
        return getStringValue(6);
    }

    public String getClassName() {
        return getStringValue(7);
    }

    public int getLevelValue() {
        return getIntValue(8);
    }

    public String getThreadName() {
        return getStringValue(9);
    }

    public String getCorrelationId() {
        return getStringValue(10);
    }

    public String getOrg() {
        return getStringValue(11);
    }

    public String getProduct() {
        return getStringValue(12);
    }

    public String getComponent() {
        return getStringValue(13);
    }

    public String getSequence() {
        String sequenceId = getStringValue(14);
        if (sequenceId == null || sequenceId.isEmpty()) {
            sequenceId = SequenceNumber.formatSequenceNumber(getDatetime(), rawSequenceNumber);
            this.setPair(14, NAMES1_1[14], sequenceId);
        }
        return sequenceId;
    }

    public String getThrowable() {
        return getStringValue(15);
    }

    public String getThrowableLocalized() {
        return getStringValue(16);
    }

    public String getMessage() {
        return getStringValue(17);
    }

    public String getFormattedMsg() {
        return getStringValue(18);
    }

    public KeyValuePairList getExtensions() {
        return getValues(19);
    }

    public int getObjectId() {
        return getIntValue(20);
    }

    public void setRawSequenceNumber(long l) {
        rawSequenceNumber = l;
    }

    public long getRawSequenceNumber(long l) {
        return rawSequenceNumber;
    }

    /**
     * @return the message ID for the given message.
     */
    protected String parseMessageId(String msg) {
        String messageId = null;
        Matcher matcher = messagePattern.matcher(msg);
        if (matcher.find())
            messageId = msg.substring(matcher.start(), matcher.end() - 1);
        return messageId;
    }

    public static String messageFields;

    public static void setMessageFields(String value) {
        messageFields = value;
        nameMap = convertStringtoMap(messageFields);
    }

    public static String getMessageFields() {
        return messageFields;
    }

    static Map<String, String> nameMap = convertStringtoMap(messageFields);

    public static Map<String, String> convertStringtoMap(String value) {
        if (value == null || value == "") {
            return null;
        }
        String[] keyValuePairs = value.split(","); //split the string to create key-value pairs
        Map<String, String> map = new HashMap<>();
        for (String pair : keyValuePairs) //iterate over the pairs
        {
            String[] entry = pair.split(":"); //split the pairs to get key and value
            if (entry.length == 2) {//if the mapped value is not null/correct format
                map.put(entry[0].trim(), entry[1].trim()); //add them to the hashmap and trim whitespaces
            }
        }
        return map;
    }

}
