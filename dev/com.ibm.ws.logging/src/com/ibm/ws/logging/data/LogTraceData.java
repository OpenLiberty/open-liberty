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

/**
 *
 */
public class LogTraceData extends GenericData {

    private final static String[] NAMES1_1 = {
                                               "ibm_datetime",
                                               "ibm_messageId",
                                               "ibm_threadId",
                                               "module",
                                               "severity",
                                               "loglevel",
                                               "ibm_methodName",
                                               "ibm_className",
                                               "levelValue",
                                               "threadName",
                                               "correlationId",
                                               "org",
                                               "product",
                                               "component",
                                               "ibm_sequence",
                                               "throwable",
                                               "throwable_localized",
                                               "message",
                                               "formattedMsg",
                                               "extensions",
                                               "objectId"
    };

    public final static String[] NAMES = {
                                           "datetime",
                                           "messageId",
                                           "threadId",
                                           "loggerName",
                                           "severity",
                                           "loglevel",
                                           "methodName",
                                           "className",
                                           "levelValue",
                                           "threadName",
                                           "correlationId",
                                           "org",
                                           "product",
                                           "component",
                                           "sequence",
                                           "throwable",
                                           "throwable_localized",
                                           "message",
                                           "formattedMsg",
                                           "extensions",
                                           "objectId"
    };

    public LogTraceData() {
        super(21);
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
        setPairs(19, kvps);
    }

    public void setObjectId(Integer i) {
        setPair(20, i);
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

    public String getDatetimeKey() {
        return NAMES[0];
    }

    public String getMessageIdKey() {
        return NAMES[1];
    }

    public String getThreadIdKey() {
        return NAMES[2];
    }

    public String getLoggerNameKey() {
        return NAMES[3];
    }

    public String getSeverityKey() {
        return NAMES[4];
    }

    public String getLoglevelKey() {
        return NAMES[5];
    }

    public String getMethodNameKey() {
        return NAMES[6];
    }

    public String getClassNameKey() {
        return NAMES[7];
    }

    public String getLevelValueKey() {
        return NAMES[8];
    }

    public String getThreadNameKey() {
        return NAMES[9];
    }

    public String getCorrelationIdKey() {
        return NAMES[10];
    }

    public String getOrgKey() {
        return NAMES[11];
    }

    public String getProductKey() {
        return NAMES[12];
    }

    public String getComponentKey() {
        return NAMES[13];
    }

    public String getSequenceKey() {
        return NAMES[14];
    }

    public String getThrowableKey() {
        return NAMES[15];
    }

    public String getThrowableLocalizedKey() {
        return NAMES[16];
    }

    public String getMessageKey() {
        return NAMES[17];
    }

    public String getFormattedMsgKey() {
        return NAMES[18];
    }

    public String getExtensionsKey() {
        return NAMES[19];
    }

    public String getObjectIdKey() {
        return NAMES[20];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[0];
    }

    public String getMessageIdKey1_1() {
        return NAMES1_1[1];
    }

    public String getThreadIdKey1_1() {
        return NAMES1_1[2];
    }

    public String getLoggerNameKey1_1() {
        return NAMES1_1[3];
    }

    public String getSeverityKey1_1() {
        return NAMES1_1[4];
    }

    public String getLoglevelKey1_1() {
        return NAMES1_1[5];
    }

    public String getMethodNameKey1_1() {
        return NAMES1_1[6];
    }

    public String getClassNameKey1_1() {
        return NAMES1_1[7];
    }

    public String getLevelValueKey1_1() {
        return NAMES1_1[8];
    }

    public String getThreadNameKey1_1() {
        return NAMES1_1[9];
    }

    public String getCorrelationIdKey1_1() {
        return NAMES1_1[10];
    }

    public String getOrgKey1_1() {
        return NAMES1_1[11];
    }

    public String getProductKey1_1() {
        return NAMES1_1[12];
    }

    public String getComponentKey1_1() {
        return NAMES1_1[13];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[14];
    }

    public String getThrowableKey1_1() {
        return NAMES1_1[15];
    }

    public String getThrowableLocalizedKey1_1() {
        return NAMES1_1[16];
    }

    public String getMessageKey1_1() {
        return NAMES1_1[17];
    }

    public String getFormattedMsgKey1_1() {
        return NAMES1_1[18];
    }

    public String getExtensionsKey1_1() {
        return NAMES1_1[19];
    }

    public String getObjectIdKey1_1() {
        return NAMES1_1[20];
    }

    public long getDatetime() {
        return getLongValue(0);
    }

    public String getMessageId() {
        return getValue(1);
    }

    public int getThreadId() {
        return getIntValue(2);
    }

    public String getModule() {
        return getValue(3);
    }

    public String getSeverity() {
        return getValue(4);
    }

    public String getLoglevel() {
        return getValue(5);
    }

    public String getMethodName() {
        return getValue(6);
    }

    public String getClassName() {
        return getValue(7);
    }

    public int getLevelValue() {
        return getIntValue(8);
    }

    public String getThreadName() {
        return getValue(9);
    }

    public String getCorrelationId() {
        return getValue(10);
    }

    public String getOrg() {
        return getValue(11);
    }

    public String getProduct() {
        return getValue(12);
    }

    public String getComponent() {
        return getValue(13);
    }

    public String getSequence() {
        return getValue(14);
    }

    public String getThrowable() {
        return getValue(15);
    }

    public String getThrowableLocalized() {
        return getValue(16);
    }

    public String getMessage() {
        return getValue(17);
    }

    public String getFormattedMsg() {
        return getValue(18);
    }

    public KeyValuePairList getExtensions() {
        return getValues(19);
    }

    public int getObjectId() {
        return getIntValue(20);
    }

    public String getValue(int index) {
        KeyValuePair kvp = (KeyValuePair) getPairs().get(index);
        return kvp.getValue();
    }

    public int getIntValue(int index) {
        KeyValuePair kvp = (KeyValuePair) getPairs().get(index);
        return Integer.parseInt(kvp.getValue());
    }

    public long getLongValue(int index) {
        KeyValuePair kvp = (KeyValuePair) getPairs().get(index);
        return Long.parseLong(kvp.getValue());
    }

    public KeyValuePairList getValues(int index) {
        return (KeyValuePairList) getPairs().get(index);
    }

}
