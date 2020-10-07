/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
public class FFDCData extends GenericData {

    public FFDCData() {
        super(13);
    }

    public static final String[] NAMES_LC = {
                                              LogFieldConstants.DATETIME,
                                              LogFieldConstants.MESSAGE,
                                              LogFieldConstants.CLASSNAME,
                                              LogFieldConstants.EXCEPTIONNAME,
                                              LogFieldConstants.PROBEID,
                                              LogFieldConstants.THREADID,
                                              LogFieldConstants.STACKTRACE,
                                              LogFieldConstants.OBJECTDETAILS,
                                              LogFieldConstants.SEQUENCE,
                                              LogFieldConstants.HOSTNAME,
                                              LogFieldConstants.WLPUSERDIR,
                                              LogFieldConstants.SERVERNAME,
                                              LogFieldConstants.TYPE
    };

    public static final String[] NAMES_JSON = {
                                                LogFieldConstants.IBM_DATETIME,
                                                LogFieldConstants.MESSAGE,
                                                LogFieldConstants.IBM_CLASSNAME,
                                                LogFieldConstants.IBM_EXCEPTIONNAME,
                                                LogFieldConstants.IBM_PROBEID,
                                                LogFieldConstants.IBM_THREADID, //long
                                                LogFieldConstants.IBM_STACKTRACE,
                                                LogFieldConstants.IBM_OBJECTDETAILS,
                                                LogFieldConstants.IBM_SEQUENCE,
                                                LogFieldConstants.HOST,
                                                LogFieldConstants.IBM_USERDIR,
                                                LogFieldConstants.IBM_SERVERNAME,
                                                LogFieldConstants.TYPE
    };

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES_JSON);
    private static NameAliases logstashNameAliases = new NameAliases(NAMES_LC);

    private static NameAliases[] nameAliases = { jsonLoggingNameAliases, logstashNameAliases };

    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliases() {
        jsonLoggingNameAliases.resetAliases();
    }

    private void setPair(int index, String s) {
        setPair(index, NAMES_JSON[index], s);
    }

    private void setPair(int index, int i) {
        setPair(index, NAMES_JSON[index], i);
    }

    private void setPair(int index, long l) {
        setPair(index, NAMES_JSON[index], l);
    }

    //@formatter:off
    public void setDatetime(long l)             { setPair(0, l); }
    public void setMessage(String s)            { setPair(1, s); }
    public void setClassName(String s)          { setPair(2, s); }
    public void setExceptionName(String s)      { setPair(3, s); }
    public void setProbeId(String s)            { setPair(4, s); }
    public void setThreadId(long l)             { setPair(5, l); }
    public void setStacktrace(String s)         { setPair(6, s); }
    public void setObjectDetails(String s)      { setPair(7, s); }
    public void setSequence(String s)           { setPair(8, s); }

    public long getDatetime()             { return getLongValue(0); }
    public String getMessage()            { return getStringValue(1); }
    public String getClassName()          { return getStringValue(2); }
    public String getExceptionName()      { return getStringValue(3); }
    public String getProbeId()            { return getStringValue(4); }
    public long getThreadId()             { return getLongValue(5); }
    public String getStacktrace()         { return getStringValue(6); }
    public String getObjectDetails()      { return getStringValue(7); }
    public String getSequence()           { return getStringValue(8); }

    public static String getDatetimeKey(int format)             { return nameAliases[format].aliases[0]; }
    public static String getMessageKey(int format)              { return nameAliases[format].aliases[1]; }
    public static String getClassNameKey(int format)            { return nameAliases[format].aliases[2]; }
    public static String getExceptionNameKey(int format)        { return nameAliases[format].aliases[3]; }
    public static String getProbeIdKey(int format)              { return nameAliases[format].aliases[4]; }
    public static String getThreadIdKey(int format)             { return nameAliases[format].aliases[5]; }
    public static String getStacktraceKey(int format)           { return nameAliases[format].aliases[6]; }
    public static String getObjectDetailsKey(int format)        { return nameAliases[format].aliases[7]; }
    public static String getSequenceKey(int format)             { return nameAliases[format].aliases[8]; }
    public static String getHostKey(int format)                 { return nameAliases[format].aliases[9]; }
    public static String getUserDirKey(int format)              { return nameAliases[format].aliases[10]; }
    public static String getServerNameKey(int format)           { return nameAliases[format].aliases[11]; }
    public static String getTypeKey(int format)                 { return nameAliases[format].aliases[12]; }

}
