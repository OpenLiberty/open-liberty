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
                                            LogFieldConstants.SERVERNAME,
                                            LogFieldConstants.TYPE
    };

    private static NameAliases jsonLoggingNameAliases = new NameAliases(NAMES1_1);
    private static NameAliases logstashNameAliases = new NameAliases(NAMES);

    private static NameAliases[] nameAliases = { jsonLoggingNameAliases, logstashNameAliases };

    // For renaming fields - only applicable to regular JSON logging and not logstash collector
    public static void newJsonLoggingNameAliases(Map<String, String> newAliases) {
        jsonLoggingNameAliases.newAliases(newAliases);
    }

    public static void resetJsonLoggingNameAliases() {
        jsonLoggingNameAliases.resetAliases();
    }

    public AuditData() {
        super(14);
    }

    //@formatter:off
    public static String getDatetimeKey(int format)   { return nameAliases[format].aliases[0]; }
    public static String getSequenceKey(int format)   { return nameAliases[format].aliases[1]; }
    public static String getThreadIDKey(int format)   { return nameAliases[format].aliases[2]; }
    public static String getHostKey(int format)       { return nameAliases[format].aliases[3]; }
    public static String getUserDirKey(int format)    { return nameAliases[format].aliases[4]; }
    public static String getServerNameKey(int format) { return nameAliases[format].aliases[5]; }
    public static String getTypeKey(int format)       { return nameAliases[format].aliases[6]; }
    //@formatter:on

}
