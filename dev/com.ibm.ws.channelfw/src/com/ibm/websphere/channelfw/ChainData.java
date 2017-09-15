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
package com.ibm.websphere.channelfw;

import java.io.Serializable;

/**
 * ChainData is a representation of the configuration information
 * about a specific Transport Chain. A Transport Chain can be viewed
 * as a protocol stack. A Transport Chain is composed of Transport
 * Channels and is used as a client or server transport.
 * <p>
 * This API can be used to get more information about a specific
 * Transport Channel from the runtime.
 * 
 * @ibm-api
 */
public interface ChainData extends Serializable {

    /**
     * Fetch the name of this chain as it was named in the configuration
     * or on creation.
     * 
     * @return String
     */
    String getName();

    /**
     * Get the type of chain (inbound or outbound).
     * 
     * @see com.ibm.websphere.channelfw.FlowType
     * 
     * @return FlowType
     */
    FlowType getType();

    /**
     * Get a list of the channel names in order from closest to connection
     * initiator to farthest.
     * <p>
     * On a client (outbound) transport, the connection initiator is normally a
     * higher level protocol (i.e. HTTP Channel).
     * <p>
     * On a server (inbound) transport, the connection initiator is often the
     * lowest level channel like the TCPChannel.
     * 
     * @return ChannelData[]
     */
    ChannelData[] getChannelList();

    /**
     * Check whether the configuration of this chain marked it as enabled
     * or not. If it is not enabled, then it should not be started.
     * 
     * @return boolean
     */
    boolean isEnabled();

    /**
     * Set the flag on whether this chain is enabled or not based on the
     * configuration.
     * 
     * @param flag
     */
    void setEnabled(boolean flag);
}
