/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import com.ibm.ejs.ras.hpel.HpelHelper;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.logging.hpel.reader.HpelFormatter;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.ws.logging.hpel.impl.LogRepositoryBaseImpl;
import com.ibm.ws.logging.internal.impl.LogProviderConfigImpl;
import com.ibm.ws.logging.internal.impl.LoggingConfigUtils;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 *
 */
public class HpelTraceServiceConfig extends LogProviderConfigImpl {
    private final static long MILLIS_IN_HOURS = 1000 * 60 * 60;
    private final static long ONE_MEG = 1024 * 1024;

    protected final String ivServerName; // server name
    // log
    protected final LogState ivLog = new LogState(HpelConstants.LOG_PREFIX);
    // trace
    protected final TraceState ivTrace = new TraceState(HpelConstants.TRACE_PREFIX);

    // text
//    protected final TextState ivText = new TextState(HpelConstants.CONSOLE_PREFIX);

    public static enum OutOfSpaceAction {
        StopServer, PurgeOld, StopLogging
    }

    public static enum OutputFormat {
        Basic, Advanced
    }

    private static String pid = null;

    /**
     * Retrieves process ID for the current process.
     * 
     * @return pid string retrieved from the process name.
     */
    public static String getPid() {
        if (pid == null) {
            String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
            if (runtimeName == null) {
                pid = "unknown";
            } else {
                int index = runtimeName.indexOf('@');
                if (index < 0) {
                    pid = runtimeName;
                } else {
                    pid = runtimeName.substring(0, index);
                }
            }
        }
        return pid;
    }

    private static void setupHpel(Map<String, String> config) {
        // Set HpelHelper with the right PID.
        HpelHelper.setPid(getPid());

        // Add FATAL AUDIT DETAIL levels for HPEL text formatter
        HpelFormatter.addCustomLevel(WsLevel.FATAL, "F");
        HpelFormatter.addCustomLevel(WsLevel.AUDIT, "A");
        HpelFormatter.addCustomLevel(WsLevel.DETAIL, "D");

        // Add "WebSphere like " header for Text copy
        HpelHelper.setCustomHeaderFormat(HpelHeader.getLibertyRuntimeHeader());

        // Set WAS type of properties
        final Properties result = new Properties();

        result.put(ServerInstanceLogRecordList.HEADER_VERSION, config.get(HpelConstants.BOOTPROP_PRODUCT_INFO));
        //result.put(ServerInstanceLogRecordList.HEADER_VERBOSE_VERSION, RasHelper.getVerboseVersion());
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_NAME, config.get(HpelConstants.INTERNAL_SERVER_NAME));
        result.put(ServerInstanceLogRecordList.HEADER_PROCESSID, HpelHelper.getProcessId());
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_TIMEZONE, TimeZone.getDefault().getID());
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_LOCALE_LANGUAGE, Locale.getDefault().getLanguage());
        result.put(ServerInstanceLogRecordList.HEADER_SERVER_LOCALE_COUNTRY, Locale.getDefault().getCountry());

        addIfPresent(result, config, "java.fullversion");
        addIfPresent(result, config, "java.version");
        addIfPresent(result, config, "os.name");
        addIfPresent(result, config, "os.version");
        addIfPresent(result, config, "java.compiler");
        addIfPresent(result, config, "java.vm.name");
        addIfPresent(result, config, "was.install.root");
        addIfPresent(result, config, "user.install.root");
        addIfPresent(result, config, "java.home");
        addIfPresent(result, config, "ws.ext.dirs");
        addIfPresent(result, config, "java.class.path");
        addIfPresent(result, config, "java.library.path");
        addIfPresent(result, config, "sun.management.compiler");

        // Add CBE related values
        addIfPresent(result, config, "os.arch");
        try {
            AccessController.doPrivileged
                            (new PrivilegedExceptionAction<Object>() {
                                @Override
                                public Object run() throws UnknownHostException {
                                    InetAddress localHost = InetAddress.getLocalHost();
                                    result.put(ServerInstanceLogRecordList.HEADER_HOSTNAME, localHost.getHostName());
                                    result.put(ServerInstanceLogRecordList.HEADER_HOSTADDRESS, localHost.getHostAddress());
                                    String type = "";
                                    if (localHost instanceof Inet4Address) {
                                        type = "IPV4";
                                    } else if (localHost instanceof Inet6Address) {
                                        type = "IPV6";
                                    }
                                    result.put(ServerInstanceLogRecordList.HEADER_HOSTTYPE, type);
                                    return null;
                                }
                            });
        } catch (Throwable t) {
            // Ignore just don't put anything.
        }
        result.put(ServerInstanceLogRecordList.HEADER_ISSERVER, "Y");

        HpelHelper.setCustomHeaderProperties(result);
    }

    private static void addIfPresent(Properties result, Map<String, String> config, final String key) {
        String value = config.get(key);
        if (value != null) {
            result.put(key, value);
        }
    }

    /**
     * Parameters related to log
     */
    protected class LogState implements Cloneable {
        protected final String prefix;
        String ivDataDirectory = null;
        int ivPurgeMaxSize = -1; // in MB, disabled until configuration applied
        int ivPurgeMinTime = -1; // in hours, disabled until configuration applied
        OutOfSpaceAction ivOutOfSpaceAction = OutOfSpaceAction.StopLogging;
        boolean ivBufferingEnabled = true;
        int ivFileSwitchTime = -1; // Hour of the day

        LogState(String prefix) {
            this.prefix = prefix;
        }

        /** {@inheritDoc} */
        @Override
        public LogState clone() {
            try {
                return (LogState) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Cannot use Java with no support for clone() on Object class");
            }
        }

        public File getLocation() {
            return new File(ivDataDirectory, LogRepositoryBaseImpl.DEFAULT_LOCATION);
        }

        public long getPurgeMaxSize() {
            return ivPurgeMaxSize < 0 ? -1 : (ivPurgeMaxSize * ONE_MEG);
        }

        public long getPurgeMinTime() {
            return ivPurgeMinTime < 0 ? -1 : (ivPurgeMinTime * MILLIS_IN_HOURS);
        }

        public void updateLoggingAttributes() {
            // The only attribute we care about from logging is the logDirectory
            ivDataDirectory = getLogDirectory().getAbsolutePath();
        }

        /**
         * Reads initial bootstrap values or changes in server.xml file.
         * 
         * @param prefix common string for all attributes or <code>null</code> if changes are in server.xml
         * @param config bootstrap or server.xml map of values
         */
        public void readState(String prefix, Map<String, ?> config) {
            if (prefix == null) {
                prefix = "";
            } else {
                prefix += this.prefix;
            }

            ivDataDirectory = getLogDirectory().getAbsolutePath();

            Object obj = config.get(prefix + HpelConstants.PURGE_MAXSIZE);
            ivPurgeMaxSize = LoggingConfigUtils.getIntValue(obj, ivPurgeMaxSize);

            obj = config.get(prefix + HpelConstants.PURGE_MINTIME);
//            ivPurgeMinTime = LoggingConfigUtils.getIntValue(obj, ivPurgeMinTime);
            ivPurgeMinTime = getLongValue(obj, ivPurgeMinTime);

            String value = (String) config.get(prefix + HpelConstants.OUTOFSPACE_ACTION);
            if (value != null) {
                try {
                    ivOutOfSpaceAction = OutOfSpaceAction.valueOf(value);
                } catch (IllegalArgumentException ex) {
                    // Leave it as StopLogging
                }
            }

            obj = config.get(prefix + HpelConstants.BUFFERING);
            if (obj instanceof Boolean) {
                ivBufferingEnabled = ((Boolean) obj).booleanValue();
            } else if (obj instanceof String) {
                ivBufferingEnabled = Boolean.valueOf((String) obj);
            }

            obj = config.get(prefix + HpelConstants.FILESWITCH_TIME);
            ivFileSwitchTime = LoggingConfigUtils.getIntValue(obj, ivFileSwitchTime);
        }

        public void writeState(StringBuilder sb) {
            sb.append(prefix).append(HpelConstants.DATA_DIRECTORY).append("=").append(ivDataDirectory).append(",");
            sb.append(prefix).append(HpelConstants.PURGE_MAXSIZE).append("=").append(Long.toString(ivPurgeMaxSize)).append(",");
            sb.append(prefix).append(HpelConstants.PURGE_MINTIME).append("=").append(Long.toString(ivPurgeMinTime)).append(",");
            sb.append(prefix).append(HpelConstants.OUTOFSPACE_ACTION).append("=").append(ivOutOfSpaceAction.toString()).append(",");
            sb.append(prefix).append(HpelConstants.BUFFERING).append("=").append(Boolean.toString(ivBufferingEnabled)).append(",");
            sb.append(prefix).append(HpelConstants.FILESWITCH_TIME).append("=").append(Integer.toString(ivFileSwitchTime));
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (ivBufferingEnabled ? 1231 : 1237);
            result = prime * result + ((ivDataDirectory == null) ? 0 : ivDataDirectory.hashCode());
            result = prime * result + ivFileSwitchTime;
            result = prime * result + ((ivOutOfSpaceAction == null) ? 0 : ivOutOfSpaceAction.hashCode());
            result = prime * result + ivPurgeMaxSize;
            result = prime * result + ivPurgeMinTime;
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof LogState))
                return false;
            LogState other = (LogState) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (ivBufferingEnabled != other.ivBufferingEnabled)
                return false;
            if (ivDataDirectory == null) {
                if (other.ivDataDirectory != null)
                    return false;
            } else if (!ivDataDirectory.equals(other.ivDataDirectory))
                return false;
            if (ivFileSwitchTime != other.ivFileSwitchTime)
                return false;
            if (ivOutOfSpaceAction == null) {
                if (other.ivOutOfSpaceAction != null)
                    return false;
            } else if (!ivOutOfSpaceAction.equals(other.ivOutOfSpaceAction))
                return false;
            if (ivPurgeMaxSize != other.ivPurgeMaxSize)
                return false;
            if (ivPurgeMinTime != other.ivPurgeMinTime)
                return false;
            return true;
        }

        private HpelTraceServiceConfig getOuterType() {
            return HpelTraceServiceConfig.this;
        }
    }

    /**
     * Parameters related to trace
     */
    protected class TraceState extends LogState {
        int ivMemoryBufferSize = -1; // in MB

        TraceState(String prefix) {
            super(prefix);
            ivOutOfSpaceAction = OutOfSpaceAction.PurgeOld;
        }

        /** {@inheritDoc} */
        @Override
        public TraceState clone() {
            return (TraceState) super.clone();
        }

        @Override
        public File getLocation() {
            return new File(ivDataDirectory, LogRepositoryBaseImpl.TRACE_LOCATION);
        }

        public long getMemoryBufferSize() {
            return ivMemoryBufferSize < 0 ? -1 : (ivMemoryBufferSize * ONE_MEG);
        }

        /** {@inheritDoc} */
        @Override
        public void readState(String prefix, Map<String, ?> config) {
            super.readState(prefix, config);
            if (prefix == null) {
                prefix = "";
            } else {
                prefix += this.prefix;
            }

            Object obj = config.get(prefix + HpelConstants.MEMORYBUFFER_SIZE);
            ivMemoryBufferSize = LoggingConfigUtils.getIntValue(obj, ivMemoryBufferSize);
        }

        /** {@inheritDoc} */
        @Override
        public void writeState(StringBuilder sb) {
            super.writeState(sb);
            sb.append(",");
            sb.append(prefix).append(HpelConstants.MEMORYBUFFER_SIZE).append("=").append(Integer.toString(ivMemoryBufferSize));
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ivMemoryBufferSize;
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (!(obj instanceof TraceState))
                return false;
            TraceState other = (TraceState) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (ivMemoryBufferSize != other.ivMemoryBufferSize)
                return false;
            return true;
        }

        private HpelTraceServiceConfig getOuterType() {
            return HpelTraceServiceConfig.this;
        }
    }

//    /**
//     * Parameters related to text copy
//     */
//    protected class TextState extends LogState {
//        volatile boolean ivEnabled = false;
//        OutputFormat ivFormat = OutputFormat.Basic;
//        boolean ivIncludeTrace = false;
//
//        TextState(String prefix) {
//            super(prefix);
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public TextState clone() {
//            return (TextState) super.clone();
//        }
//
//        @Override
//        public File getLocation() {
//            return new File(ivDataDirectory);
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public void readState(String prefix, Map<String, ?> config) {
//            super.readState(prefix, config);
//            if (prefix == null) {
//                prefix = "";
//            } else {
//                prefix += this.prefix;
//            }
//
//            String value = (String) config.get(prefix + HpelConstants.OUTPUT_FORMAT);
//            if (value != null) {
//                try {
//                    ivFormat = OutputFormat.valueOf(value);
//                } catch (IllegalArgumentException ex) {
//                    // Leave current value
//                }
//            }
//
//            Object obj = config.get(prefix + HpelConstants.INCLUDE_TRACE);
//            if (obj instanceof Boolean) {
//                ivIncludeTrace = (Boolean) obj;
//            } else if (obj instanceof String) {
//                ivIncludeTrace = Boolean.valueOf((String) obj);
//            }
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public void writeState(StringBuilder sb) {
//            if (ivEnabled) {
//                super.writeState(sb);
//                sb.append(",");
//                sb.append(prefix).append(HpelConstants.OUTPUT_FORMAT).append("=").append(ivFormat.toString()).append(",");
//                sb.append(prefix).append(HpelConstants.INCLUDE_TRACE).append("=").append(Boolean.toString(ivIncludeTrace));
//            } else {
//                sb.append(prefix).append("disabled");
//            }
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int result = super.hashCode();
//            result = prime * result + getOuterType().hashCode();
//            result = prime * result + (ivEnabled ? 1231 : 1237);
//            result = prime * result + ((ivFormat == null) ? 0 : ivFormat.hashCode());
//            result = prime * result + (ivIncludeTrace ? 1231 : 1237);
//            return result;
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj)
//                return true;
//            if (!super.equals(obj))
//                return false;
//            if (!(obj instanceof TextState))
//                return false;
//            TextState other = (TextState) obj;
//            if (!getOuterType().equals(other.getOuterType()))
//                return false;
//            if (ivEnabled != other.ivEnabled)
//                return false;
//            if (ivFormat == null) {
//                if (other.ivFormat != null)
//                    return false;
//            } else if (!ivFormat.equals(other.ivFormat))
//                return false;
//            if (ivIncludeTrace != other.ivIncludeTrace)
//                return false;
//            return true;
//        }
//
//        private HpelTraceServiceConfig getOuterType() {
//            return HpelTraceServiceConfig.this;
//        }
//
//    }

    /**
     * @param config
     * @param logLocation
     * @param factory
     */
    public HpelTraceServiceConfig(Map<String, String> config, File logLocation, TextFileOutputStreamFactory factory) {
        super(config, logLocation, factory);
        setupHpel(config);
        ivServerName = config.get(HpelConstants.INTERNAL_SERVER_NAME);
        if (ivServerName == null) {
            throw new RuntimeException("Boot parameters are missing required '" + HpelConstants.INTERNAL_SERVER_NAME + "' attribute.");
        }
        ivLog.readState(HpelConstants.PROP_PREFIX, config);
        ivTrace.readState(HpelConstants.PROP_PREFIX, config);
//        ivText.readState(HpelConstants.PROP_PREFIX, config);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void update(Map<String, Object> config) {
        // Update called by TrConfigurator and FFDCConfigurator
        super.update(config);
        ivLog.updateLoggingAttributes();
        ivTrace.updateLoggingAttributes();
//        ivText.ivEnabled = config.containsKey(HpelConstants.TEXT_LOG);
    }

    public synchronized void updateLog(Map<String, Object> config) {
        ivLog.readState(null, config);
    }

    public synchronized void updateTrace(Map<String, Object> config) {
        ivTrace.readState(null, config);
    }

//    public synchronized void updateText(Map<String, Object> config) {
//        ivText.readState(null, config);
//    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[");
        ivLog.writeState(sb);
        sb.append(",");
        ivTrace.writeState(sb);
//        sb.append(",");
//        ivText.writeState(sb);
        sb.append("]");
        return sb.toString();
    }

    /**
     * @param obj
     * @param defaultValue
     * @return
     */
    private int getLongValue(Object obj, int defaultValue) {
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException ex) {
            }
        } else if (obj instanceof Long)
            try {
                return ((Long) obj).intValue();
            } catch (Exception ex) {
            }
        return defaultValue;
    }
}
