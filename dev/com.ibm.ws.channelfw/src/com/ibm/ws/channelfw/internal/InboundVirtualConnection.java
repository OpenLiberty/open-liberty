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
package com.ibm.ws.channelfw.internal;

import com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * This represents an inbound or outbound connection using the channel
 * framework. It basically is used to track the state for individual channels
 * in the chain associated with this connection. Each channel in the chain
 * keeps a ConnectionLink object in this connection.
 */
public interface InboundVirtualConnection extends VirtualConnection {

    /**
     * Returns Discrimination Status. Gets the integers associated with the saved
     * status from the last
     * discrimination. This is used only used internally by the channel framework.
     * 
     * @return integer array that represents status
     */
    int[] getDiscriminatorStatus();

    /**
     * Set Discrimination Status. This sets the status in the discrimination. This
     * is
     * used when the discrimination has been done but is returning a
     * "needs more data". This
     * can be used to save the state information to free ourselves from yet
     * another connection
     * specific state object.
     * 
     * @param status
     *            to keep for the next time we are called.
     */
    void setDiscriminatorStatus(int[] status);

    /**
     * Set the discrimination process used for this discriminator state. This
     * validates the
     * discriminators haven't changes since last status was obtained.
     * 
     * @param dp
     */
    void setDiscriminationGroup(DiscriminationGroup dp);

    /**
     * gets the discrimination process for comparisions to validate the
     * discriminator state.
     * 
     * @return discrimination group
     */
    DiscriminationGroup getDiscriminationGroup();

}
