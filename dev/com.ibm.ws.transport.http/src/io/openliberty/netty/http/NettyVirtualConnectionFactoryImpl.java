/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.http;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.wsspi.channelfw.InboundVirtualConnectionFactory;
import com.ibm.wwspi.channelfw.VirtualConnection;

/**
 * Factory object for Netty virtual connections
 */
public class NettyVirtualConnectionFactoryImpl implements InboundVirtualConnectionFactory {

    public NettyVirtualConnectionFactoryImpl() {

    }

    public VirtualConnection createConnection() {
        NettyVirtualConnectionImpl connection = new NettyVirtualConnectionImpl();
        connection.init();
        return connection;
    }

    public FlowType getType() {
        return FlowType.INBOUND;
    }

    public String getName() {
        return "inbound";
    }

    public void destroy() {

    }

}
