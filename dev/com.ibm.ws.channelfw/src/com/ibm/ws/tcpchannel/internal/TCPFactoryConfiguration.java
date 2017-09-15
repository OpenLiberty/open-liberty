/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;

/**
 * Configuration for a single TCP channel factory.
 */
public class TCPFactoryConfiguration implements FFDCSelfIntrospectable {

    private static final String CLASS_NAME = TCPFactoryConfiguration.class.getName();

    private static final TraceComponent tc = Tr.register(TCPFactoryConfiguration.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected static final String KEYS_PER_SELECTOR = "maxKeysPerSelector";
    protected static final String SELECTOR_IDLE_TIMEOUT = "channelSelectorIdleTimeout";
    protected static final String SELECTOR_TERM_TIMEOUT = "channelSelectorWaitToTerminate";
    protected static final String SELECTOR_YIELD = "selectorYield";
    protected static final String SELECTOR_WAKEUP = "selectorWakeup";
    protected static final String CANCEL_KEY_ON_CLOSE = "cancelKeyOnClose";
    protected static final String COMBINE_SELECTORS = "combineSelectors";
    protected static final String COMM_CLASS = "commClass";
    protected static final String EARLY_BINDS = "earlyBinds";

    private Map<Object, Object> commonProperties = null;

    private static int maxKeysPerSelector = 500;
    private static long channelSelectorIdleTimeout = 300000L; // use msec in code
    private static long channelSelectorWaitToTerminate = 10000L; // use msec in code

    private static int selectorWakeup = ValidateUtils.SELECTOR_WAKEUP_WHEN_NEEDED;
    private static boolean selectorYield = false;
    private static boolean cancelKeyOnClose = false;
    private static boolean combineSelectors = false;

    private final static String commClassAio = "com.ibm.ws.tcpchannel.internal.AioTCPChannel";
    private final static String commClassNio = "com.ibm.ws.tcpchannel.internal.NioTCPChannel";
    private static String commClass = commClassNio;

    private static HashMap earlyBinds = null;

    private final String channelName = "*";

    private static String osName = null;

    /**
     * Constructor.
     * 
     * @param properties
     * @throws ChannelFactoryException
     */
    public TCPFactoryConfiguration(Map<Object, Object> properties) throws ChannelFactoryException {
        setValues(properties);
    }

    protected void setValues(Map<Object, Object> oCommonConfig) throws ChannelFactoryException {
        boolean overrideCancelKeySetting = false;
        this.commonProperties = oCommonConfig;

        if (isWindows()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Running on Windows, defaulting selectorYield to true");
            }
            selectorYield = true;
        } else if (isISeries()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Running on iSeries, defaulting maxKeysPerSelector to 50");
            }
            maxKeysPerSelector = 50;
        }
        // check if we should override cancel key check
        // not supposed to need this on SUN, but Sun fix was too late in cycle to
        // remove. remove in 7.0
        // on HP defect 199041 is suppose to be the fix for this
        else if (isSun()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Running on Sun, defaulting cancelKeyOnCloseSetting to true");
            }
            overrideCancelKeySetting = true;
            cancelKeyOnClose = true;
        }

        if (this.commonProperties != null) {

            Set<Object> keySet = this.commonProperties.keySet();
            Iterator<Object> keys = keySet.iterator();
            String key = null;
            String value = null;
            Object entry = null;
            int result = ValidateUtils.VALIDATE_OK;
            int minValue = 0;
            int maxValue = 0;
            int keyType = 0;

            while (keys.hasNext() && result == ValidateUtils.VALIDATE_OK) {
                key = (String) keys.next();
                entry = this.commonProperties.get(key);

                if (key.equalsIgnoreCase(EARLY_BINDS)) {
                    earlyBinds = (HashMap) entry;
                    keyType = ValidateUtils.KEY_OBJECT;
                    result = ValidateUtils.VALIDATE_OK;
                    continue;
                }

                if (entry instanceof String) {
                    value = (String) entry;

                    try {
                        if (key.equalsIgnoreCase(KEYS_PER_SELECTOR)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_INT;
                            minValue = ValidateUtils.KEYS_PER_SELECTOR_MIN;
                            maxValue = ValidateUtils.KEYS_PER_SELECTOR_MAX;
                            maxKeysPerSelector = Integer.parseInt(value);
                            result = ValidateUtils.testKeysPerSelector(maxKeysPerSelector);
                            continue;
                        }

                        if (key.equalsIgnoreCase(SELECTOR_IDLE_TIMEOUT)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_INT;
                            minValue = ValidateUtils.CHANNEL_SELECTOR_IDLE_TIMEOUT_MIN;
                            maxValue = ValidateUtils.CHANNEL_SELECTOR_IDLE_TIMEOUT_MAX;
                            channelSelectorIdleTimeout = Long.parseLong(value);
                            result = ValidateUtils.testChannelSelectorIdleTimeout(channelSelectorIdleTimeout);
                            continue;
                        }

                        if (key.equalsIgnoreCase(SELECTOR_TERM_TIMEOUT)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_INT;
                            minValue = ValidateUtils.CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MIN;
                            maxValue = ValidateUtils.CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MAX;
                            channelSelectorWaitToTerminate = Long.parseLong(value);
                            result = ValidateUtils.testChannelSelectorWaitToTerminate(channelSelectorWaitToTerminate);
                            if (result == ValidateUtils.VALIDATE_OK) {
                                // convert to milliseconds
                                channelSelectorWaitToTerminate = channelSelectorWaitToTerminate * 1000L;
                            }
                            continue;
                        }

                        if (key.equalsIgnoreCase(CANCEL_KEY_ON_CLOSE)) {
                            // convert and check
                            if (!overrideCancelKeySetting) {
                                keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                                cancelKeyOnClose = Boolean.parseBoolean(value);
                                result = ValidateUtils.VALIDATE_OK;
                            }
                            continue;
                        }

                        if (key.equalsIgnoreCase(SELECTOR_YIELD)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                            selectorYield = Boolean.parseBoolean(value);
                            result = ValidateUtils.VALIDATE_OK;
                            continue;
                        }

                        if (key.equalsIgnoreCase(SELECTOR_WAKEUP)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_INT;
                            minValue = ValidateUtils.SELECTOR_WAKEUP_OPTION_MIN;
                            maxValue = ValidateUtils.SELECTOR_WAKEUP_OPTION_MAX;
                            selectorWakeup = Integer.parseInt(value);
                            result = ValidateUtils.testChannelSelectorWakeupOption(selectorWakeup);
                            continue;
                        }

                        if (key.equalsIgnoreCase(COMBINE_SELECTORS)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                            combineSelectors = Boolean.parseBoolean(value);
                            result = ValidateUtils.VALIDATE_OK;
                            continue;
                        }

                        if (key.equalsIgnoreCase(COMM_CLASS)) {
                            // convert and check
                            keyType = ValidateUtils.KEY_TYPE_STRING;
                            commClass = value;
                            result = ValidateUtils.VALIDATE_OK;
                            continue;
                        }

                        Tr.warning(tc, TCPChannelMessageConstants.CONFIG_KEY_NOT_VALID, new Object[] { this.channelName, key, value });

                    } catch (NumberFormatException x) {
                        Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NUMBER_EXCEPTION, new Object[] { this.channelName, key, value });
                        ChannelFactoryException e = new ChannelFactoryException("The TCP Factory caught a NumberFormatException processing property, Channel Name: "
                                                                                + this.channelName
                                                                                + " Property name: " + key + " value: " + value, x);
                        FFDCFilter.processException(e, CLASS_NAME, "101", this);
                        throw e;
                    }
                }
            } // end-while-keys

            if (result != ValidateUtils.VALIDATE_OK) {
                ChannelFactoryException e = null;
                if (keyType == ValidateUtils.KEY_TYPE_INT) {
                    Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_INT, new Object[] { this.channelName, key, value, "" + minValue, "" + maxValue });
                    e = new ChannelFactoryException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.channelName + " name: "
                                                    + key
                                                    + " value: " + value + "minimum Value: " + minValue + "maximum Value: " + maxValue);
                } else if (keyType == ValidateUtils.KEY_TYPE_BOOLEAN) {
                    Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_BOOLEAN, new Object[] { this.channelName, key, value });
                    e = new ChannelFactoryException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.channelName + " name: "
                                                    + key
                                                    + " value: " + value + " Valid Range: false, true");
                } else {
                    if (value == null) {
                        Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_NULL_STRING, new Object[] { this.channelName, key });
                        e = new ChannelFactoryException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.channelName
                                                        + " name: " + key
                                                        + " value: null");
                    } else {
                        Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_STRING, new Object[] { this.channelName, key, value });
                        e = new ChannelFactoryException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.channelName
                                                        + " name: " + key
                                                        + " value: " + value);
                    }
                }
                FFDCFilter.processException(e, CLASS_NAME, "102", this);
                throw e;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                outputConfigToTrace();
            }
        }
    }

    protected void checkAndSetValues(Map<Object, Object> oCommonConfig) throws ChannelFactoryException {
        setValues(oCommonConfig);
    }

    protected static int getMaxKeysPerSelector() {
        return maxKeysPerSelector;
    }

    protected static long getChannelSelectorIdleTimeout() {
        return channelSelectorIdleTimeout;
    }

    // If an idle selector decides to shut down but cannot, the amount
    // of time it will wait before making the next check to see if it
    // can shutdown (milliseconds).
    protected static long getChannelSelectorWaitToTerminate() {
        return channelSelectorWaitToTerminate;
    }

    protected static boolean getSelectorYield() {
        return selectorYield;
    }

    protected static boolean getCancelKeyOnClose() {
        return cancelKeyOnClose;
    }

    protected static int getSelectorWakeup() {
        return selectorWakeup;
    }

    protected static boolean getCombineSelectors() {
        return combineSelectors;
    }

    /**
     * Access the TCP communication class to use (NIO, AIO, etc)
     * 
     * @return String
     */
    public static String getCommClass(boolean asyncIOEnabled) {
        // Check to see if the TCP class has been changed from its default value.
        // If it hasn't and the native z/OS AIO support is ready and able then update the
        // class to use the native z/OS support.
        if ((commClass != null) && commClass.equals(commClassNio) && asyncIOEnabled) {
            commClass = commClassAio;
        }

        return commClass;
    }

    protected static Map getEarlyBinds() {
        return earlyBinds;
    }

    protected void outputConfigToTrace() {
        Tr.debug(tc, "Config parameters for TCP Channel Factory: ");
        Tr.debug(tc, KEYS_PER_SELECTOR + ": " + maxKeysPerSelector);
        Tr.debug(tc, SELECTOR_IDLE_TIMEOUT + ": " + channelSelectorIdleTimeout);
        Tr.debug(tc, SELECTOR_TERM_TIMEOUT + ": " + channelSelectorWaitToTerminate);
        Tr.debug(tc, SELECTOR_YIELD + ": " + selectorYield);
        Tr.debug(tc, CANCEL_KEY_ON_CLOSE + ": " + cancelKeyOnClose);
        Tr.debug(tc, COMBINE_SELECTORS + ": " + combineSelectors);
        Tr.debug(tc, COMM_CLASS + ": " + commClass);
    }

    @Override
    public String[] introspectSelf() {
        String[] rc = new String[7];
        rc[0] = KEYS_PER_SELECTOR + maxKeysPerSelector;
        rc[1] = SELECTOR_IDLE_TIMEOUT + channelSelectorIdleTimeout;
        rc[2] = SELECTOR_TERM_TIMEOUT + channelSelectorWaitToTerminate;
        rc[3] = SELECTOR_YIELD + selectorYield;
        rc[4] = CANCEL_KEY_ON_CLOSE + cancelKeyOnClose;
        rc[5] = COMBINE_SELECTORS + combineSelectors;
        rc[6] = COMM_CLASS + commClass;
        return rc;
    }

    protected static String getOSName() {
        if (osName == null) {
            osName = System.getProperty("os.name", "unknown");
        }
        return osName;
    }

    protected static boolean isSun() {
        String name = getOSName();
        if ((name.indexOf("SunOS")) == -1 && (name.indexOf("Solaris")) == -1) {
            return false;
        }
        return true;
    }

    protected static boolean isHPUX() {
        String name = getOSName();
        return (name.indexOf("HP-UX") != -1);
    }

    protected static boolean isWindows() {
        String name = getOSName();
        return name.toLowerCase().startsWith("windows");
    }

    protected static boolean isISeries() {
        String name = getOSName();
        return name.equalsIgnoreCase("os/400");
    }

}
