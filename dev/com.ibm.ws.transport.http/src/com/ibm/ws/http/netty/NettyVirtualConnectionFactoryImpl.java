/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.wsspi.channelfw.InboundVirtualConnectionFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 *
 */
public class NettyVirtualConnectionFactoryImpl implements InboundVirtualConnectionFactory {

    public NettyVirtualConnectionFactoryImpl() {

    }

    @Override
    public String getName() {

        return "inbound";
    }

    @Override
    public FlowType getType() {
        return FlowType.INBOUND;
    }

    @Override
    public void destroy() throws ChannelException, ChainException {

    }

    @Override
    public VirtualConnection createConnection() {
        NettyVirtualConnectionImpl connection = new NettyVirtualConnectionImpl();
        connection.init();
        return connection;
    }

}
