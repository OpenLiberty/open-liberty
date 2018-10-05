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

import com.ibm.ws.logging.collector.LogFieldConstants;

/**
 *
 */
public class AccessLogData extends GenericData {
    private final static String[] NAMES1_1 = {
                                               LogFieldConstants.IBM_REQUESTSTARTTIME,
                                               LogFieldConstants.IBM_URIPATH,
                                               LogFieldConstants.IBM_REQUESTMETHOD,
                                               LogFieldConstants.IBM_QUERYSTRING,
                                               LogFieldConstants.IBM_REQUESTHOST,
                                               LogFieldConstants.IBM_REQUESTPORT,
                                               LogFieldConstants.IBM_REMOTEHOST,
                                               LogFieldConstants.IBM_USERAGENT,
                                               LogFieldConstants.IBM_REQUESTPROTOCOL,
                                               LogFieldConstants.IBM_BYTESRECEIVED,
                                               LogFieldConstants.IBM_RESPONSECODE,
                                               LogFieldConstants.IBM_ELAPSEDTIME,
                                               LogFieldConstants.IBM_DATETIME,
                                               LogFieldConstants.IBM_SEQUENCE
    };

    private final static String[] NAMES = {
                                            LogFieldConstants.REQUESTSTARTTIME,
                                            LogFieldConstants.URIPATH,
                                            LogFieldConstants.REQUESTMETHOD,
                                            LogFieldConstants.QUERYSTRING,
                                            LogFieldConstants.REQUESTHOST,
                                            LogFieldConstants.REQUESTPORT,
                                            LogFieldConstants.REMOTEHOST,
                                            LogFieldConstants.USERAGENT,
                                            LogFieldConstants.REQUESTPROTOCOL,
                                            LogFieldConstants.BYTESRECEIVED,
                                            LogFieldConstants.RESPONSECODE,
                                            LogFieldConstants.ELAPSEDTIME,
                                            LogFieldConstants.DATETIME,
                                            LogFieldConstants.SEQUENCE
    };

    public AccessLogData() {
        super(14);
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

    public void setRequestStartTime(long l) {
        setPair(0, l);
    }

    public void setUriPath(String s) {
        setPair(1, s);
    }

    public void setRequestMethod(String s) {
        setPair(2, s);
    }

    public void setQueryString(String s) {
        setPair(3, s);
    }

    public void setRequestHost(String s) {
        setPair(4, s);
    }

    public void setRequestPort(String s) {
        setPair(5, s);
    }

    public void setRemoteHost(String s) {
        setPair(6, s);
    }

    public void setUserAgent(String s) {
        setPair(7, s);
    }

    public void setRequestProtocol(String s) {
        setPair(8, s);
    }

    public void setBytesReceived(long l) {
        setPair(9, l);
    }

    public void setResponseCode(int i) {
        setPair(10, i);
    }

    public void setElapsedTime(long l) {
        setPair(11, l);
    }

    public void setDatetime(long l) {
        setPair(12, l);
    }

    public void setSequence(String s) {
        setPair(13, s);
    }

    public long getRequestStartTime() {
        return getLongValue(0);
    }

    public String getUriPath() {
        return getStringValue(1);
    }

    public String getRequestMethod() {
        return getStringValue(2);
    }

    public String getQueryString() {
        return getStringValue(3);
    }

    public String getRequestHost() {
        return getStringValue(4);
    }

    public String getRequestPort() {
        return getStringValue(5);
    }

    public String getRemoteHost() {
        return getStringValue(6);
    }

    public String getUserAgent() {
        return getStringValue(7);
    }

    public String getRequestProtocol() {
        return getStringValue(8);
    }

    public long getBytesReceived() {
        return getLongValue(9);
    }

    public int getResponseCode() {
        return getIntValue(10);
    }

    public long getElapsedTime() {
        return getLongValue(11);
    }

    public long getDatetime() {
        return getLongValue(12);
    }

    public String getSequence() {
        return getStringValue(13);
    }

    public String getRequestStartTimeKey() {
        return NAMES[0];
    }

    public String getUriPathKey() {
        return NAMES[1];
    }

    public String getRequestMethodKey() {
        return NAMES[2];
    }

    public String getQueryStringKey() {
        return NAMES[3];
    }

    public String getRequestHostKey() {
        return NAMES[4];
    }

    public String getRequestPortKey() {
        return NAMES[5];
    }

    public String getRemoteHostKey() {
        return NAMES[6];
    }

    public String getUserAgentKey() {
        return NAMES[7];
    }

    public String getRequestProtocolKey() {
        return NAMES[8];
    }

    public String getBytesReceivedKey() {
        return NAMES[9];
    }

    public String getResponseCodeKey() {
        return NAMES[10];
    }

    public String getElapsedTimeKey() {
        return NAMES[11];
    }

    public String getDatetimeKey() {
        return NAMES[12];
    }

    public String getSequenceKey() {
        return NAMES[13];
    }

    public String getRequestStartTimeKey1_1() {
        return NAMES1_1[0];
    }

    public String getUriPathKey1_1() {
        return NAMES1_1[1];
    }

    public String getRequestMethodKey1_1() {
        return NAMES1_1[2];
    }

    public String getQueryStringKey1_1() {
        return NAMES1_1[3];
    }

    public String getRequestHostKey1_1() {
        return NAMES1_1[4];
    }

    public String getRequestPortKey1_1() {
        return NAMES1_1[5];
    }

    public String getRemoteHostKey1_1() {
        return NAMES1_1[6];
    }

    public String getUserAgentKey1_1() {
        return NAMES1_1[7];
    }

    public String getRequestProtocolKey1_1() {
        return NAMES1_1[8];
    }

    public String getBytesReceivedKey1_1() {
        return NAMES1_1[9];
    }

    public String getResponseCodeKey1_1() {
        return NAMES1_1[10];
    }

    public String getElapsedTimeKey1_1() {
        return NAMES1_1[11];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[12];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[13];
    }
}
