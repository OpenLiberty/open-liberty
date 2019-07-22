/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.Map;

import com.ibm.ws.logging.collector.LogFieldConstants;

/**
 *
 */
public class AuditData extends GenericData {
    public static final String[] NAMES1_1 = {
                                              LogFieldConstants.IBM_DATETIME,
                                              LogFieldConstants.IBM_SEQUENCE,
                                              LogFieldConstants.IBM_THREADID,
                                              LogFieldConstants.HOST,
                                              LogFieldConstants.IBM_USERDIR,
                                              LogFieldConstants.IBM_SERVERNAME
    };

    private final static String[] NAMES = {
                                            LogFieldConstants.IBM_DATETIME,
                                            LogFieldConstants.IBM_SEQUENCE,
                                            LogFieldConstants.IBM_THREADID,
                                            LogFieldConstants.HOSTNAME,
                                            LogFieldConstants.WLPUSERDIR,
                                            LogFieldConstants.SERVERNAME
    };

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES1_1);

    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public AuditData() {
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
//
//    public void setDatetime(long l) {
//        setPair(12, l);
//    }
//
//    public void setSequence(String s) {
//        setPair(13, s);
//    }
//
//    public long getDatetime() {
//        return getLongValue(12);
//    }
//
//    public String getSequence() {
//        return getStringValue(13);
//    }

    public String getDatetimeKey() {
        return NAMES[0];
    }

    public String getSequenceKey() {
        return NAMES[1];
    }

    public String getThreadIDKey() {
        return NAMES[2];
    }

    public String getHostKey() {
        return NAMES[3];
    }

    public String getUserDirKey() {
        return NAMES[4];
    }

    public String getServerNameKey() {
        return NAMES[5];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[0];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[1];
    }

    public String getThreadIDKey1_1() {
        return NAMES1_1[2];
    }

    public String getHostKey1_1() {
        return NAMES1_1[3];
    }

    public String getUserDirKey1_1() {
        return NAMES1_1[4];
    }

    public String getServerNameKey1_1() {
        return NAMES1_1[5];
    }

    public String getDatetimeKeyJSON() {
        return jsonLoggingNameAliases.getAlias(0);
    }

    public String getSequenceKeyJSON() {
        return jsonLoggingNameAliases.getAlias(1);
    }

    public String getThreadIDJSON() {
        return jsonLoggingNameAliases.getAlias(2);
    }

    public String getHostJSON() {
        return jsonLoggingNameAliases.getAlias(3);
    }

    public String getUserDirJSON() {
        return jsonLoggingNameAliases.getAlias(4);
    }

    public String getServerNameJSON() {
        return jsonLoggingNameAliases.getAlias(5);
    }

}
