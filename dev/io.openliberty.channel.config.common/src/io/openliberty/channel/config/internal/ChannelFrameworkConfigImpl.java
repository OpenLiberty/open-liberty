/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.update.ServerElementConfig;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

import io.openliberty.channel.config.ChannelFrameworkConfig;

/*
 * This class represents the `channelfw` configuration for the Channel Framework. Currently, only the chainQuiesceTimeout is
 * necessary for NettyFrameworkImpl to access. However, the other properties were inlcuded for completeness.
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
    private static final TraceComponent tc = Tr.register(ChannelFrameworkConfigImpl.class,
                                                         "ChannelFramework",
                                                         "io.openliberty.channel.config.internal.resources.ChannelConfigMessages");

    /** Property name for the default chain quiesce timeout length */
    public final String PROPERTY_CHAIN_QUIESCETIMEOUT = "chainQuiesceTimeout";
    /** Property name for the missing config warning message delay */
    public final String PROPERTY_MISSING_CONFIG_WARNING = "warningWaitTime";
    /** Alias used in metatype */
    public final String PROPERTY_CONFIG_ALIAS = "channelfw";

    /** Default chain quiesce timeout value in milliseconds (must match metatype.xml default) */
    private static final long DEFAULT_CHAIN_QUIESCE_TIMEOUT = 30000L;

    /** Custom property for timed delay before warning about missing config in milliseconds */
    private long missingConfigWarning = 10000L;
    /** Property for the chain quiescetimeout to default to for various paths in milliseconds */
    private long chainQuiesceTimeout = DEFAULT_CHAIN_QUIESCE_TIMEOUT;

    /** Reference to ServerElementConfig service for accessing server-level quiesceTimeout */
    private final ServerElementConfig serverElementConfig;

    @Activate
    public ChannelFrameworkConfigImpl(@Reference ServerElementConfig config) {
        this.serverElementConfig = config;
    }

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
        Object value = cfwConfiguration.get(PROPERTY_CHAIN_QUIESCETIMEOUT);
        if (null != value) {
            setDefaultChainQuiesceTimeout(value);
        }
        value = cfwConfiguration.get(PROPERTY_MISSING_CONFIG_WARNING);
        if (null != value) {
            setMissingConfigWarning(value);
        }
    }

    /**
     * Set the default chain quiesce timeout property from config.
     * If quiesceTimeout is explicitly configured on the server element,
     * it will always override chainQuiesceTimeout. An informational message
     * is logged only if chainQuiesceTimeout was configured to a value other
     * than the default. Unfortunately, we cannot easily detect when
     * chainQuiesceTimeout is explicitly configured. So we compromise by
     * checking if the default value was changed. In that case we know it
     * was explicitly configured and we inform the user it is being ignored.
     *
     * @param value the configured chainQuiesceTimeout value (null if not configured)
     */
    private void setDefaultChainQuiesceTimeout(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting default chain quiesce timeout [" + value + "]");
        }

        // Parse the configured value
        long configuredTimeout = DEFAULT_CHAIN_QUIESCE_TIMEOUT;
        try {
            configuredTimeout = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_QUIESCETIMEOUT, value, DEFAULT_CHAIN_QUIESCE_TIMEOUT);
        } catch (NumberFormatException e) {
            // Use default value
        }

        // If quiesceTimeout is explicitly configured on the server element,
        // it overrides chainQuiesceTimeout
        if (serverElementConfig.isQuiesceTimeoutExplicitlyConfigured()) {

            long serverQuiesceTimeout = serverElementConfig.getQuiesceTimeoutMillis();

            // Only emit info message if chainQuiesceTimeout is NOT the default value
            // AND the configured value differs from the server quiesceTimeout
            if (configuredTimeout != DEFAULT_CHAIN_QUIESCE_TIMEOUT && configuredTimeout != serverQuiesceTimeout) {
                Tr.info(tc, "QUIESCE_TIMEOUT_OVERRIDE", configuredTimeout / 1000); //value in seconds
            }
            // override
            chainQuiesceTimeout = serverQuiesceTimeout;
            return;
        }

        // Original logic - use chainQuiesceTimeout if configured, otherwise keep default
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

        // Log the final value being used for testing purposes
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "Final chainQuiesceTimeout value: " + this.chainQuiesceTimeout + "ms");
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

    /*
     * Query the default chain quiesce timeout to use.
     *
     * @return long
     */
    @Override
    public long getDefaultChainQuiesceTimeout() {
        return chainQuiesceTimeout;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChannelFrameworkConfigImpl [");
        sb.append(PROPERTY_CHAIN_QUIESCETIMEOUT).append("=").append(chainQuiesceTimeout).append(", ");
        sb.append(PROPERTY_MISSING_CONFIG_WARNING).append("=").append(missingConfigWarning);
        sb.append("]");
        return sb.toString();
    }

}
