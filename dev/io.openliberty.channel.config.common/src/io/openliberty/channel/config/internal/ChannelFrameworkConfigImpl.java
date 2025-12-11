/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.channel.config.internal;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

import io.openliberty.channel.config.ChannelFrameworkConfig;

/*
 * This class represents the `channelfw` configuration for the Channel Framework. Currently, only the chainQuiesceTimeout is 
 * necessary for NettyFrameworkImpl to access. However, the other three properties were inlcuded for completeness.
 * 
 * Elements of this class are adapated from CHFWBundle and ChannelFrameworkImpl from the 'com.ibm.ws.channelfw' bundle.
 */
@Component(configurationPid = "com.ibm.ws.channelfw",
           service = { ChannelFrameworkConfig.class },
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class ChannelFrameworkConfigImpl implements ChannelFrameworkConfig {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(ChannelFrameworkConfigImpl.class);

    /** Property name for the chain restart interval timer */
    public final String PROPERTY_CHAIN_START_RETRY_INTERVAL = "chainStartRetryInterval";
    /** Property name for the chain start retry attempt counter */
    public final String PROPERTY_CHAIN_START_RETRY_ATTEMPTS = "chainStartRetryAttempts";
    /** Property name for the default chain quiesce timeout length */
    public final String PROPERTY_CHAIN_QUIESCETIMEOUT = "chainQuiesceTimeout";
    /** Property name for the missing config warning message delay */
    public final String PROPERTY_MISSING_CONFIG_WARNING = "warningWaitTime";
    /** Alias used in metatype */
    public final String PROPERTY_CONFIG_ALIAS = "channelfw";

    /**
     * Custom property configured in the framework for the length of time in
     * milliseconds between chain start retries when a RetryableChannelException
     * results from starting a chain, in milliseconds
     */
    private long chainStartRetryInterval = 5000L;

    /**
     * Custom property configured in the framework for the number of chain start
     * retries
     * that can happen when a RetryableChannelException results from starting a
     * chain.
     */
    private int chainStartRetryAttempts = 60;
    /** Custom property for timed delay before warning about missing config in milliseconds */
    private long missingConfigWarning = 10000L;
    /** Property for the chain quiescetimeout to default to for various paths in milliseconds */
    private long chainQuiesceTimeout = 30000L;

    /**
     * DS method for activating this component.
     *
     * @param context
     */
    @Activate
    protected void activate(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating ", config);
        }
        // set properties from config
        modified(config);
    }

    /**
     * DS method for deactivating this component.
     *
     * @param context
     */
    @Deactivate
    protected void deactivate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating");
        }
    }

    /**
     * Modified method. This method is called when the
     * service properties associated with the service are updated through a
     * configuration change.
     *
     * @param cfwConfiguration
     *                             the configuration data
     */
    @Modified
    protected synchronized void modified(Map<String, Object> cfwConfiguration) {
        if (null == cfwConfiguration) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing config", cfwConfiguration);
        }
        Object value = cfwConfiguration.get(PROPERTY_CHAIN_START_RETRY_ATTEMPTS);
        if (null != value) {
            setChainStartRetryAttempts(value);
        }
        value = cfwConfiguration.get(PROPERTY_CHAIN_START_RETRY_INTERVAL);
        if (null != value) {
            setChainStartRetryInterval(value);
        }
        value = cfwConfiguration.get(PROPERTY_CHAIN_QUIESCETIMEOUT);
        if (null != value) {
            setDefaultChainQuiesceTimeout(value);
        }
        value = cfwConfiguration.get(PROPERTY_MISSING_CONFIG_WARNING);
        if (null != value) {
            setMissingConfigWarning(value);
        }
    }

    /**
     * Setter method for the interval of time between chain restart attempts.
     *
     * @param value
     * @throws NumberFormatException
     *                                   if the value is not a number or is less than zero
     */
    private void setChainStartRetryInterval(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting chain start retry interval [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_START_RETRY_INTERVAL, value, chainStartRetryInterval);
            if (0L <= num) {
                this.chainStartRetryInterval = num;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Value is too low");
                }
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Value is not a number");
            }
        }
    }

    /**
     * Setter method for the number of chain restart attempts.
     *
     * @param value
     * @throws NumberFormatException
     *                                   if the value is not a number or is less than zero
     */
    private void setChainStartRetryAttempts(Object value) throws NumberFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting chain start retry attempts [" + value + "]");
        }
        try {
            int num = MetatypeUtils.parseInteger(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_START_RETRY_ATTEMPTS, value, chainStartRetryAttempts);
            if (-1 <= num) {
                this.chainStartRetryAttempts = num;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Value too low");
                }
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Vaue is not a number");
            }
        }
    }

    /**
     * Set the default chain quiesce timeout property from config.
     *
     * @param value
     */
    private void setDefaultChainQuiesceTimeout(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting default chain quiesce timeout [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_QUIESCETIMEOUT, value, chainQuiesceTimeout);
            if (0 < num) {
                this.chainQuiesceTimeout = num;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Timeout is too low");
                }
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Timeout is not a number");
            }
        }
    }

    /**
     * Set the custom property for the delay before warning about missing
     * configuration values.
     *
     * @param value
     */
    private void setMissingConfigWarning(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting missing config warning delay to [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_MISSING_CONFIG_WARNING, value, missingConfigWarning);
            if (0L <= num) {
                this.missingConfigWarning = num;
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Value is not a number");
            }
        }
    }

    /**
     * Query the amount of delay before warning the user about missing
     * configuration items.
     *
     * @return long
     */
    @Override
    public long getMissingConfigWarning() {
        return missingConfigWarning;
    }

    /**
     * Return the maximum number of attempts that will be made to
     * restart a chain when it fails to start.
     *
     * @return long
     */
    @Override
    public long getChainStartRetryAttempts() {
        return chainStartRetryAttempts;
    }

    /**
     * Return the length of time which will be waiting between restarts
     * of a chain after it fails to start.
     *
     * @return long
     */
    @Override
    public long getChainStartRetryInterval() {
        return chainStartRetryInterval;
    }

    /*
     * Query the default chain quiesce timeout to use.
     * 
     * @return long
     */
    @Override
    public long getDefaultChainQuiesceTimeout() {
        return chainQuiesceTimeout;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChannelFrameworkConfigImpl [");
        sb.append(PROPERTY_CHAIN_START_RETRY_ATTEMPTS).append("=").append(chainStartRetryAttempts).append(", ");
        sb.append(PROPERTY_CHAIN_START_RETRY_INTERVAL).append("=").append(chainStartRetryInterval).append(", ");
        sb.append(PROPERTY_CHAIN_QUIESCETIMEOUT).append("=").append(chainQuiesceTimeout).append(", ");
        sb.append(PROPERTY_MISSING_CONFIG_WARNING).append("=").append(missingConfigWarning);
        sb.append("]");
        return sb.toString();
    }

}
