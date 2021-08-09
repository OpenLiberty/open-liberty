/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.chaincoherency;

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundProtocolLink;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Connection link object for the test channel.
 */
public class ConnectorChannelLink extends InboundProtocolLink {
    private TCPConnectionContext myTSC = null;
    private InboundChannel myChannel = null;

    /**
     * Constructor.
     * 
     * @param vc
     * @param channel
     */
    public ConnectorChannelLink(VirtualConnection vc, InboundChannel channel) {
        init(vc);
        myChannel = channel;
    }

    /**
     * @see ConnectionLink#getChannelAccessor()
     */
    public Object getChannelAccessor() {
        return myTSC;
    }

    /**
     * @see ConnectionReadyCallback#ready(VirtualConnection)
     */
    public void ready(VirtualConnection inVC) {
        myTSC = (TCPConnectionContext) getDeviceLink().getChannelAccessor();
        ConnectionReadyCallback linkOnApplicationSide = getApplicationCallback();
        if (linkOnApplicationSide != null) {
            linkOnApplicationSide.ready(inVC);
        } else {
            DiscriminationProcess dp = myChannel.getDiscriminationProcess();
            try {
                dp.discriminate(inVC, myTSC, this);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Exception caught doing discriminate, " + e);
            }
            getApplicationCallback().ready(inVC);
        }
    }

}
