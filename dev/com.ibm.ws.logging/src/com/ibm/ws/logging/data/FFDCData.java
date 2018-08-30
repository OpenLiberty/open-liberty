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

import java.util.ArrayList;

import com.ibm.ws.logging.collector.LogFieldConstants;

/**
 *
 */
public class FFDCData extends GenericData {

    private static final int NUMBER_OF_FIELDS = 13;

    private static final String[] NAMES = {
                                            LogFieldConstants.DATETIME,
                                            LogFieldConstants.DATEOFFIRSTOCCURENCE,
                                            LogFieldConstants.COUNT,
                                            LogFieldConstants.MESSAGE,
                                            LogFieldConstants.CLASSNAME,
                                            LogFieldConstants.LABEL,
                                            LogFieldConstants.EXCEPTIONNAME,
                                            LogFieldConstants.PROBEID,
                                            LogFieldConstants.SOURCEID,
                                            LogFieldConstants.THREADID,
                                            LogFieldConstants.STACKTRACE,
                                            LogFieldConstants.OBJECTDETAILS,
                                            LogFieldConstants.SEQUENCE
    };

    private static final String[] NAMES1_1 = {
                                               LogFieldConstants.IBM_DATETIME,
                                               LogFieldConstants.DATEOFFIRSTOCCURENCE,
                                               LogFieldConstants.COUNT,
                                               LogFieldConstants.MESSAGE,
                                               LogFieldConstants.IBM_CLASSNAME,
                                               LogFieldConstants.LABEL,
                                               LogFieldConstants.IBM_EXCEPTIONNAME,
                                               LogFieldConstants.IBM_PROBEID,
                                               LogFieldConstants.SOURCEID,
                                               LogFieldConstants.IBM_THREADID, //long
                                               LogFieldConstants.IBM_STACKTRACE,
                                               LogFieldConstants.IBM_OBJECTDETAILS,
                                               LogFieldConstants.IBM_SEQUENCE
    };

    private static KeyValuePair.ValueTypes[] types = {
                                                       KeyValuePair.ValueTypes.LONG,
                                                       KeyValuePair.ValueTypes.LONG,
                                                       KeyValuePair.ValueTypes.INTEGER,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING,
                                                       KeyValuePair.ValueTypes.STRING
    };

    public FFDCData() {
        super(initializeDefaultList());
    }

    private static ArrayList<KeyValuePair> initializeDefaultList() {
        ArrayList<KeyValuePair> defaultPairs = new ArrayList<KeyValuePair>(NUMBER_OF_FIELDS);
        for (int index = 0; index < NUMBER_OF_FIELDS; index++) {
            switch (types[index]) {
                case INTEGER:
                    defaultPairs.add(new KeyValueIntegerPair(NAMES1_1[index], null));
                    break;
                case BOOLEAN:
                    defaultPairs.add(new KeyValueBooleanPair(NAMES1_1[index], null));
                    break;
                case FLOAT:
                    defaultPairs.add(new KeyValueFloatPair(NAMES1_1[index], null));
                    break;
                case LIST:
                    defaultPairs.add(new KeyValuePairList(NAMES1_1[index]));
                    break;
                case LONG:
                    defaultPairs.add(new KeyValueLongPair(NAMES1_1[index], null));
                    break;
                case STRING:
                    defaultPairs.add(new KeyValueStringPair(NAMES1_1[index], null));
                    break;
            }
        }
        return defaultPairs;
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

    private String getStringValue(int index) {
        KeyValueStringPair kvp = (KeyValueStringPair) getPairs().get(index);
        return kvp.getStringValue();
    }

    private int getIntValue(int index) {
        KeyValueIntegerPair kvp = (KeyValueIntegerPair) getPairs().get(index);
        return kvp.getIntValue();
    }

    private long getLongValue(int index) {
        KeyValueLongPair kvp = (KeyValueLongPair) getPairs().get(index);
        return kvp.getLongValue();
    }

    public void setDatetime(long l) {
        setPair(0, l);
    }

    public void setDateOfFirstOccurence(long l) {
        setPair(1, l);
    }

    public void setCount(int i) {
        setPair(2, i);
    }

    public void setMessage(String s) {
        setPair(3, s);
    }

    public void setClassName(String s) {
        setPair(4, s);
    }

    public void setLabel(String s) {
        setPair(5, s);
    }

    public void setExceptionName(String s) {
        setPair(6, s);
    }

    public void setProbeId(String s) {
        setPair(7, s);
    }

    public void setSourceId(String s) {
        setPair(8, s);
    }

    public void setThreadId(long l) {
        setPair(9, l);
    }

    public void setStacktrace(String s) {
        setPair(10, s);
    }

    public void setObjectDetails(String s) {
        setPair(11, s);
    }

    public void setSequence(String s) {
        setPair(12, s);
    }

    public long getDatetime() {
        return getLongValue(0);
    }

    public long getDateOfFirstOccurence() {
        return getLongValue(1);
    }

    public int getCount() {
        return getIntValue(2);
    }

    public String getMessage() {
        return getStringValue(3);
    }

    public String getClassName() {
        return getStringValue(4);
    }

    public String getLabel() {
        return getStringValue(5);
    }

    public String getExceptionName() {
        return getStringValue(6);
    }

    public String getProbeId() {
        return getStringValue(7);
    }

    public String getSourceId() {
        return getStringValue(8);
    }

    public long getThreadId() {
        return getLongValue(9);
    }

    public String getStacktrace() {
        return getStringValue(10);
    }

    public String getObjectDetails() {
        return getStringValue(11);
    }

    public String getSequence() {
        return getStringValue(12);
    }

    public String getDatetimeKey() {
        return NAMES[0];
    }

    public String getDateOfFirstOccurenceKey() {
        return NAMES[1];
    }

    public String getCountKey() {
        return NAMES[2];
    }

    public String getMessageKey() {
        return NAMES[3];
    }

    public String getClassNameKey() {
        return NAMES[4];
    }

    public String getLabelKey() {
        return NAMES[5];
    }

    public String getExceptionNameKey() {
        return NAMES[6];
    }

    public String getProbeIdKey() {
        return NAMES[7];
    }

    public String getSourceIdKey() {
        return NAMES[8];
    }

    public String getThreadIdKey() {
        return NAMES[9];
    }

    public String getStacktraceKey() {
        return NAMES[10];
    }

    public String getObjectDetailsKey() {
        return NAMES[11];
    }

    public String getSequenceKey() {
        return NAMES[12];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[0];
    }

    public String getDateOfFirstOccurenceKey1_1() {
        return NAMES1_1[1];
    }

    public String getCountKey1_1() {
        return NAMES1_1[2];
    }

    public String getMessageKey1_1() {
        return NAMES1_1[3];
    }

    public String getClassNameKey1_1() {
        return NAMES1_1[4];
    }

    public String getLabelKey1_1() {
        return NAMES1_1[5];
    }

    public String getExceptionNameKey1_1() {
        return NAMES1_1[6];
    }

    public String getProbeIdKey1_1() {
        return NAMES1_1[7];
    }

    public String getSourceIdKey1_1() {
        return NAMES1_1[8];
    }

    public String getThreadIdKey1_1() {
        return NAMES1_1[9];
    }

    public String getStacktraceKey1_1() {
        return NAMES1_1[10];
    }

    public String getObjectDetailsKey1_1() {
        return NAMES1_1[11];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[12];
    }
}
