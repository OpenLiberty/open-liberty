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

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * Work queue implementation for AIO, which only creates the outbound
 * connect selectors.
 */
public class AioWorkQueueManager extends WorkQueueManager {

    private static final TraceComponent tc = Tr.register(AioWorkQueueManager.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected void startSelectors(boolean inBound) throws ChannelException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startSelectors: " + inBound);
        }

        // This routine is called when the channel gets created via the
        // TCPChannelFactory.createChannel method. That method does not need
        // to be thread safe, since the framework will not have multiple calls
        // outstanding. Therefore we don't have to be thread safe looking at
        // these objects to determine if we have created them or not.

        if (connect != null) {
            // previously created the outbound connect selectors
            return;
        }

        try {
            if (!inBound) {
                connect = new ConnectChannelSelector[maxChannelSelectorsPerFlow];
                connectCount = new int[maxChannelSelectorsPerFlow];
                for (int i = 0; i < maxChannelSelectorsPerFlow; i++) {
                    connectCount[i] = CS_NULL;
                }

                connect[0] = new ConnectChannelSelector(this, 0, CS_CONNECTOR);
                createNewThread(connect[0], CS_CONNECTOR, 1);
                connectCount[0] = CS_OK;
            }
        } catch (IOException ioe) {
            FFDCFilter.processException(ioe, getClass().getName(), "100", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error creating selectors: " + ioe);
            }
            ChannelException ce = new ChannelException("Unable to start the TCP Channel Connect Selector", ioe);
            throw ce;
        }
    }

}
