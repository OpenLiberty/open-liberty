/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A HpelPlainFormatter subclass implementation that provides formatting of RepositoryLogRecord to a format
 * referred to as "Advanced".
 */
public class HpelAdvancedFormatter extends HpelPlainFormatter {
    private static final String svAdvancedPadding = "          ";
    private String svLineSeparatorPlusAdvancedPadding = lineSeparator + svAdvancedPadding;

    private static final class ExtensionMapping {
        final String newKey;
        final String oldKey;

        ExtensionMapping(String newKey, String oldKey) {
            this.newKey = newKey;
            this.oldKey = oldKey;
        }

        String getExtension(RepositoryLogRecord record) {
            String result = record.getExtension(newKey);
            if (result == null) {
                result = record.getExtension(oldKey);
            }
            return result;
        }
    }

    private static final ArrayList<ExtensionMapping> COMPATIBLE_EXTENSIONS = new ArrayList<ExtensionMapping>();
    private static final int ORG_INDEX = 1; // Index of ORGANIZATION key in specialExtensions array. It contains organization name
    private static final int PRODUCT_INDEX = 2; // Index of PRODUCT key in specialExtensions array. It contains product name
    private static final int COMPONENT_INDEX = 3; // Index of COMPONENT key in specialExtensions array. It contains component name
    private static final int THREAD_INDEX = 4; // Index of PTHREADID key in specialExtensions array. It contains thread name
    private static final Set<String> PRIVATE_EXTENSIONS = new HashSet<String>();
    static {
        // Reserved names which can't be used for extensions (used by CBE impl)
        // be sure to add these to WsLogger list of reserved names (in static class init).
        final String EDE_CORRELATIONID_NAME = "correlationId";
        final String EDE_ORGANIZATION_NAME = "organization";
        final String EDE_VERSION_NAME = "version";
        final String EDE_LOCALIZABLE_NAME = "localizable";
        final String EDE_RAWDATA_NAME = "rawData";
        final String EDE_COMPONENT_NAME = "component";
        final String EDE_PROCESSID_NAME = "processId";
        final String EDE_PROCESSNAME_NAME = "processName";
        final String EDE_PRODUCT_NAME = "product";
        final String EDE_FLATSTACKTRACE_NAME = "flatStackTrace";

        // Z/OS reserved names - LIDB2667.13
        final String EDE_ADDRESSSPACEID_NAME = "addressSpaceId";
        final String EDE_JOBNAME_NAME = "jobName";
        final String EDE_SERVER_NAME = "serverName";
        final String EDE_SYSTEMJOBID_NAME = "jobId";
        final String EDE_SYSTEMNAME_NAME = "systemName";
        final String EDE_TCBADDRESS_NAME = "tcbAddress";
        final String EDE_TID_NAME = "pthreadId";
        final String EDE_CORRELATOR_NAME = "ORBRequestId";

        for (String[] keys : new String[][] {
                                             { RepositoryLogRecord.CORRELATIONID, EDE_CORRELATIONID_NAME },
                                             { RepositoryLogRecord.ORGANIZATION, EDE_ORGANIZATION_NAME },
                                             { RepositoryLogRecord.PRODUCT, EDE_PRODUCT_NAME },
                                             { RepositoryLogRecord.COMPONENT, EDE_COMPONENT_NAME },
                                             { RepositoryLogRecord.PTHREADID, EDE_TID_NAME }
        }) {
            COMPATIBLE_EXTENSIONS.add(new ExtensionMapping(keys[0], keys[1]));
            PRIVATE_EXTENSIONS.add(keys[0]);
            PRIVATE_EXTENSIONS.add(keys[1]);
        }
        for (String key : new String[] {
                                        EDE_VERSION_NAME,
                                        EDE_LOCALIZABLE_NAME,
                                        EDE_RAWDATA_NAME,
                                        EDE_PROCESSID_NAME,
                                        EDE_PROCESSNAME_NAME,
                                        EDE_FLATSTACKTRACE_NAME,
                                        EDE_ADDRESSSPACEID_NAME,
                                        EDE_JOBNAME_NAME,
                                        EDE_SERVER_NAME,
                                        EDE_SYSTEMJOBID_NAME,
                                        EDE_SYSTEMNAME_NAME,
                                        EDE_TCBADDRESS_NAME,
                                        EDE_CORRELATOR_NAME
        }) {
            PRIVATE_EXTENSIONS.add(key);
        }
    }

    /**
     * Formats a RepositoryLogRecord into a localized advanced format output String.
     * 
     * @param record the RepositoryLogRecord to be formatted
     * @param locale the Locale to use for localization when formatting this record.
     * 
     * @return the formated string output.
     */
    @Override
    public String formatRecord(RepositoryLogRecord record, Locale locale) {
        if (null == record) {
            throw new IllegalArgumentException("Record cannot be null");
        }
        StringBuilder sb = new StringBuilder(300);
        String lineSeparatorPlusPadding = "";

        createEventHeader(record, sb);
        sb.append(lineSeparator);
        sb.append(svAdvancedPadding);
        lineSeparatorPlusPadding = svLineSeparatorPlusAdvancedPadding;

        // add the localizedMsg
        sb.append(formatMessage(record, locale));

        // if we have a stack trace, append that to the formatted output.
        if (record.getStackTrace() != null) {
            sb.append(lineSeparatorPlusPadding);
            sb.append(record.getStackTrace());
        }
        return sb.toString();
    }

    /**
     * Footers do not exist for the Advanced format, so return empty string.
     */
    @Override
    public String getFooter() {
        return "";
    }

    /**
     * Generates the advanced format event header information that is common to all subclasses of RasEvent starting
     * at position 0 of the specified StringBuffer.
     * <p>
     * Advanced format specifies that the header information will include a formatted timestamp followed by an 8 (hex)
     * digit thread Id, followed by type character, followed by a correlation Id field, followed by the className,
     * methodName (if non-null), organization, product and component name (if non-null). No terminating blank is written
     * to the buffer.
     * <p>
     * 
     * @param record RepositoryLogRecord provided by the caller.
     * @param buffer output buffer where converted entry is appended to.
     */
    protected void createEventHeader(RepositoryLogRecord record, StringBuilder buffer) {
        if (null == record) {
            throw new IllegalArgumentException("Record cannot be null");
        }
        if (null == buffer) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }

        // Create the time stamp
        createEventTimeStamp(record, buffer);

        // Add the formatted threadID
        formatThreadID(record, buffer);

        // Add in the log record Type
        buffer.append(mapLevelToType(record));

        // Add Correlation ID
        ExtensionMapping map = COMPATIBLE_EXTENSIONS.get(0);
        buffer.append(map.newKey).append("=");
        String value = map.getExtension(record);
        if (value != null) {
            buffer.append(value);
        }

        // Add Logger Name
        buffer.append(" source=");
        buffer.append(record.getLoggerName());

        // Add class name if not null
        if (record.getSourceClassName() != null && record.getSourceClassName() != "") {
            buffer.append(" class=");
            buffer.append(record.getSourceClassName());
        }

        // Add method name if not null
        if (record.getSourceMethodName() != null && record.getSourceMethodName() != "") {
            buffer.append(" method=");
            buffer.append(record.getSourceMethodName());
        }

        for (int i = 1; i < COMPATIBLE_EXTENSIONS.size(); i++) {
            map = COMPATIBLE_EXTENSIONS.get(i);
            value = map.getExtension(record);
            if (value != null || i == ORG_INDEX || i == PRODUCT_INDEX || i == COMPONENT_INDEX) {
                buffer.append(" ").append(map.newKey).append("=");
                if (THREAD_INDEX == i) {
                    buffer.append("[");
                }
                buffer.append(value);
                if (THREAD_INDEX == i) {
                    buffer.append("]");
                }
            }
        }

        Map<String, String> extensions = record.getExtensions();
        if (extensions != null) {
            for (Entry<String, String> extension : record.getExtensions().entrySet()) {
                if (!PRIVATE_EXTENSIONS.contains(extension.getKey())) {
                    buffer.append(" ").append(extension.getKey()).append("=[").append(extension.getValue()).append("]");
                }
            }
        }
    }

    @Override
    protected String appendUnusedParms(String message, Object[] args) {
        String returnValue = message;

        // parms were present but message format won't take them.  Append parms as desired.
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            buffer.append(" parm");
            buffer.append(i);
            buffer.append("=");
            if (args[i] == null)
                buffer.append(nullParamString);
            else
                buffer.append(args[i].toString());
        }
        returnValue = returnValue.concat(buffer.toString());
        return returnValue;
    }

    @Override
    public void setLineSeparator(String lineSeparator) {
        super.setLineSeparator(lineSeparator);
        svLineSeparatorPlusAdvancedPadding = lineSeparator + svAdvancedPadding;
    }

}
