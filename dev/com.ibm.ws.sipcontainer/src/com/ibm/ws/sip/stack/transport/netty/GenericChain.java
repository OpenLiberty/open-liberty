/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transport.*;
import com.ibm.ws.sip.stack.transport.sip.netty.SipInboundChannelFactoryWs;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.netty.internal.*;
import io.openliberty.netty.internal.exception.NettyException;

/**
 * Encapsulation of steps for starting/stopping an SIP chain in a
 * controlled/predictable manner with a minimum of synchronization.
 */
abstract public class GenericChain extends GenericChainBase {

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(GenericChain.class);

    /** "localhost" string */
    protected static final String LOCALHOST = "localhost";

    /** "id" string */
    protected static final String ID = "id";

    /** SipChannel prefix name */
    protected static String SIP_Channel = "SIPChannel_";

    /** Chain prefix name */
    protected static String CHAIN = "Chain";

    /** this member is increased for each new chain when it is created. */
    protected static int s_chains = 0;

    /** Endpoint name */
    protected String endpointName;

    /** SipChannel name */
    protected String sipChannelName;

    /** Chain name */
    protected String chainName;

    /**
     * Toggled by enable/disable methods. This serves only to block activity of some
     * operations (start/update on disabled chain should no-op).
     */
    protected volatile boolean enabled = false;

    /**
     * A snapshot of the configuration (collection of properties objects) last used
     * for a start/update operation.
     */
    protected volatile ActiveConfiguration currentConfig = null;

    /** Owner of this Chain - Endpoint Impl  */
    protected final GenericEndpointImpl owner;

    /** Reference to Netty bundle */
    protected NettyFramework nettyBundle;

    /**
     * The TCP based bootstrap.
     */
    protected ServerBootstrapExtended serverBootstrap;
    /**
     * UDP based bootstrap.
     */
    protected BootstrapExtended bootstrap;

    protected void activate(Map<String, Object> properties) throws NettyException {
    }

    protected void deactivate(Map<String, Object> properties, int reason) {
        serverBootstrap = null;
        bootstrap = null;
    }

    /**
     * Create the new chain with it's parent endpoint
     * 
     * @param sipEndpointImpl the owning endpoint: used for notifications
     * @param isTls           true if this is to be an TLS chain.
     */
    public GenericChain(GenericEndpointImpl owner) {
        this.owner = owner;
    }

    /**
     * Returns the Endpoint Name
     * 
     * @return
     */
    protected String getEndpointName() {
        return endpointName;
    }

    /**
     * Initialize this chain manager: Channel and chain names shouldn't fluctuate as
     * config changes, so come up with names associated with this set of
     * channels/chains that will be reused regardless of
     * start/stop/enable/disable/modify
     * 
     * @param endpointId  The id of the sipEndpoint
     * @param componentId The DS component id
     * @param cfw         Channel framework
     */
    public void init(String endpointId, Object componentId, NettyFramework nettyBundle, String name) {

        String chainNumber = String.valueOf(s_chains++);

        this.nettyBundle = nettyBundle;

        endpointName = endpointId;

        sipChannelName = SIP_Channel + getName() + "_" + endpointId + "_" + chainNumber;

        chainName = CHAIN + endpointId + "_" + chainNumber;
    }

    /**
     * returns ChainName
     * 
     * @return
     */
    public String getChainName() {
        return chainName;
    }

    /**
     * Enable this chain: this happens automatically for the sip chain, but is
     * delayed on the ssl chain until ssl support becomes available. This does not
     * change the chain's state. The caller should make subsequent calls to perform
     * actions on the chain.
     */
    public void enable() {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("enable chain " + this);
        }
        enabled = true;
    }

    /**
     * Disable this chain. This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void disable() {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("disable chain " + this);
        }
        enabled = false;
    }

    /**
     * retuns if this chain is enabled
     * 
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the Endpoint owner of this Chain
     * 
     * @return
     */
    protected GenericEndpointImpl getOwner() {
        return owner;
    }

    /**
     * 
     * @return
     */
    public int getActivePort() {
        ActiveConfiguration cfg = currentConfig;
        if (cfg != null)
            return cfg.getActivePort();
        return -1;
    }

    /**
     *
     * @return
     */
    public String getActiveHost() {
        ActiveConfiguration cfg = currentConfig;
        if (cfg != null) {
            return cfg.getConfigHost();
        }
        return null;
    }

    /**
     * Setup event properties - OSGI
     * 
     * @param eventProps
     */
    abstract protected void setupEventProps(Map<String, Object> eventProps);

    /**
     * Create active configuration for this chain
     * 
     * @param cfg
     */
    abstract protected ActiveConfiguration createActiveConfiguration();

    /**
     * Rebuild all channels that related to this specific chain
     * 
     * @param oldConfig
     * @param newConfig
     */
    abstract protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig);

    protected SipInboundChannelFactoryWs sipInboundChannelFactory = new SipInboundChannelFactoryWs();

    /**
     * Return current configuration
     * 
     * @return
     */
    protected ActiveConfiguration getCurrentConfig() {
        return currentConfig;
    }

    /**
     * Sets the current configuration
     * 
     * @param currentConfig
     */
    protected void setCurrentConfig(ActiveConfiguration currentConfig) {
        this.currentConfig = currentConfig;
    }

    /**
     * Update/start the chain configuration.
     */
    // TODO Liberty - if we are not going to support configuration runtime change -
    // remove unnecessary code.
    @Override
    public synchronized void update() {
        if (c_logger.isEventEnabled()) {
            c_logger.event("update chain " + this);
        }

        // Don't update or start the chain if it is disabled.
        if (!isEnabled() || FrameworkState.isStopping())
            return;

        final ActiveConfiguration oldConfig = getCurrentConfig();

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.validConfiguration;
        final ActiveConfiguration newConfig = createActiveConfiguration();

        if (newConfig.configPort < 0 || !newConfig.isReady()) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            setCurrentConfig(newConfig);

            return;
        }

        if (validOldConfig && newConfig.unchanged(oldConfig)) {
            // If the old config was valid & the new one is the same..
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Configuration is unchanged " + newConfig);
            }
            // If configurations are identical, see if the listening port is also the same
            // which would indicate that the chain is running with the unchanged configuration
            int port = newConfig.getActivePort();
            if (port == oldConfig.getActivePort() && port != -1) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug("Chain is already started " + oldConfig);
                }
                return;
            }

            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("Existing config must be started " + newConfig);
            }
        } else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("New/changed chain configuration "
						+ newConfig);
			}
		}

        if (validOldConfig) {
            rebuildTheChannel(oldConfig, newConfig);
        }
        else
           createChannels(newConfig);
        // save the new/changed configuration before we start setting up the
        // new chain
        setCurrentConfig(newConfig);
        currentConfig.validConfiguration = true;
    }

    /**
     * ChainEventListener method. This method can not be synchronized (deadlock with
     * update/stop). Rely on CFW synchronization of chain operations.
     * 
     */
    public synchronized void chainStarted() {

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("Chain " + toString() + " is started");
        }

        final ActiveConfiguration cfg = currentConfig;
        final int port = cfg.getActivePort();

        if (port > 0) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug("New configuration started " + cfg);
            }

            // Post an endpoint started event to anyone listening
            String topic = owner.getEventTopic() + GenericServiceConstants.ENDPOINT_STARTED;
            postEvent(topic, cfg, null);
        }
    }

    /**
     * Publish an event relating to a chain starting/stopping with the given
     * properties set about the chain.
     */
    protected void postEvent(String t, ActiveConfiguration c, Exception e) {
        Map<String, Object> eventProps = new HashMap<String, Object>(4);

        if (c.activeHost != null) {
            eventProps.put(GenericServiceConstants.ENDPOINT_ACTIVE_HOST, c.activeHost);
        }

        eventProps.put(GenericServiceConstants.ENDPOINT_ACTIVE_PORT, c.configPort); //was configHost?
        eventProps.put(GenericServiceConstants.ENDPOINT_CONFIG_HOST, c.configHost);
        eventProps.put(GenericServiceConstants.ENDPOINT_CONFIG_PORT, c.configPort);

        setupEventProps(eventProps);

        if (e != null) {
            eventProps.put(GenericServiceConstants.ENDPOINT_EXCEPTION, e.toString());
        }

        EventAdmin engine = GenericEndpointImpl.getEventAdmin();
        if (engine != null) {
            Event event = new Event(t, eventProps);
            engine.postEvent(event);
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.getClass().getSimpleName() + "[@=" + System.identityHashCode(this) + ",enabled=" + enabled
                + ",chainName=" + chainName + ",config="
                + currentConfig + "]";
    }


    @Override
    protected void createChannels(ActiveConfiguration newConfig) {
    }

    @Override
    public void stop() {
    }
}
