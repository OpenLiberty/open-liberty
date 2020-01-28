/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
                                              LogFieldConstants.IBM_SERVERNAME,
                                              LogFieldConstants.TYPE
    };

    private final static String[] NAMES = {
                                            LogFieldConstants.DATETIME,
                                            LogFieldConstants.SEQUENCE,
                                            LogFieldConstants.THREADID,
                                            LogFieldConstants.HOSTNAME,
                                            LogFieldConstants.WLPUSERDIR,
                                            LogFieldConstants.SERVERNAME
    };

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES1_1);

    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliases() {
        jsonLoggingNameAliases.resetAliases();
    }

    public AuditData() {
        super(14);
    }

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

    //name aliases
    public static String getDatetimeKeyJSON() {
        return jsonLoggingNameAliases.aliases[0];
    }

    public static String getSequenceKeyJSON() {
        return jsonLoggingNameAliases.aliases[1];
    }

    public static String getThreadIDKeyJSON() {
        return jsonLoggingNameAliases.aliases[2];
    }

    public static String getHostKeyJSON() {
        return jsonLoggingNameAliases.aliases[3];
    }

    public static String getUserDirKeyJSON() {
        return jsonLoggingNameAliases.aliases[4];
    }

    public static String getServerNameKeyJSON() {
        return jsonLoggingNameAliases.aliases[5];
    }

    public static String getTypeKeyJSON() {
        return jsonLoggingNameAliases.aliases[6];
    }

}