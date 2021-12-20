/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport;

import java.util.Map;

/**
 * Encapsulation of steps for starting/stopping an SIP chain in a
 * controlled/predictable manner with a minimum of synchronization.
 */
abstract public class GenericChainBase {

    public enum Type {
        tcp, tls, udp
    };

    /**
     * Enable this chain: this happens automatically for the sip chain, but is
     * delayed on the ssl chain until ssl support becomes available. This does not
     * change the chain's state. The caller should make subsequent calls to perform
     * actions on the chain.
     */
    public abstract void enable();

    public abstract int getActivePort();

    public abstract String getActiveHost();

    /**
     * @return name of this chain
     */
    abstract protected String getName();

    /**
     * Setup event propertied - OSGI
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
     * Create channels for this Chain
     * 
     * @param newConfig
     */
    abstract protected void createChannels(ActiveConfiguration newConfig);

    /**
     * Rebuild all channels that related to this specific chain
     * 
     * @param oldConfig
     * @param newConfig
     */
    abstract protected void rebuildTheChannel(ActiveConfiguration oldConfig, ActiveConfiguration newConfig);

    /**
     * Update/start the chain configuration.
     */
    abstract public void update();
    
    abstract public void stop();
    
    abstract public Type getType();

    abstract public String getTransport();
}
