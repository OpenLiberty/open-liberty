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
package com.ibm.wsspi.channelfw;

import com.ibm.websphere.channelfw.*;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * The VirtualConnectionFactory is used to create virtual connections.
 * <p>
 * These factories should be obtained from the ChannelFramework.
 */
public interface VirtualConnectionFactory {
    /**
     * Create a VirtualConnection.
     * 
     * @return VirtualConnection
     * @throws ChannelException
     * @throws ChainException
     */
    VirtualConnection createConnection() throws ChannelException, ChainException;

    /**
     * Get the channel or chain name associated with this factory.
     * 
     * @return String
     */
    String getName();

    /**
     * Get the type (inbound || outbound) this factory is associated with.
     * 
     * @return FlowType
     */
    FlowType getType();

    /**
     * Assert that the virtual connection factory will no longer be used
     * and can be cleaned up.
     * 
     * @throws ChannelException
     * @throws ChainException
     */
    void destroy() throws ChannelException, ChainException;
}
