/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.nio.channels.DatagramChannel;

/**
 * @author mjohnson
 */
public class NIOChannelModRequest {
    //
    // What is this request?
    //
    public static final int ADD_REQUEST = 1;
    public static final int REMOVE_REQUEST = 2;
    public static final int MODIFY_REQUEST = 3;

    //
    // If it is a modify request, do I need to AND with the mask
    // or OR with the mask?
    //
    public static final int AND_OPERATOR = 1;
    public static final int OR_OPERATOR = 2;

    private int requestType = 0;
    private int interestMask = 0;
    private int interestOperand = 0;
    private DatagramChannel channel = null;
    private UDPNetworkLayer networkLayer = null;

    NIOChannelModRequest(int requestType, DatagramChannel channel, int interestMask, UDPNetworkLayer networkLayer) {
        init(requestType, channel, interestMask, networkLayer);
    }

    NIOChannelModRequest(int requestType, DatagramChannel channel, int interestMask, int interestOperand, UDPNetworkLayer networkLayer) {
        init(requestType, channel, interestMask, networkLayer);

        this.interestOperand = interestOperand;
    }

    private void init(int type, DatagramChannel channel, int mask, UDPNetworkLayer layer) {
        this.requestType = type;
        this.interestMask = mask;
        this.channel = channel;
        this.networkLayer = layer;
    }

    /**
     * @return Returns the channel.
     */
    public DatagramChannel getChannel() {
        return channel;
    }

    /**
     * @return Returns the interestOps.
     */
    public int getInterestMask() {
        return interestMask;
    }

    /**
     * @return Returns the networkLayer.
     */
    public UDPNetworkLayer getNetworkLayer() {
        return networkLayer;
    }

    /**
     * @return Returns the requestType.
     */
    public int getRequestType() {
        return requestType;
    }

    /**
     * @return Returns the interestOperator.
     */
    public int getInterestOperator() {
        return interestOperand;
    }
}
