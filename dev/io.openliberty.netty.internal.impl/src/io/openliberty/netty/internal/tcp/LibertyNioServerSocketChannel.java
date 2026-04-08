/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.nio.channels.SocketChannel;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Retains all functionality of Netty's {@link NioServerSocketChannel}, but the 
 * socket channel initialized will be the wrapper {@link LibertyNioSocketChannel}
 * instead of the default {@link NioSocketChannel}.
 */
public class LibertyNioServerSocketChannel extends NioServerSocketChannel{

    private static final TraceComponent tc = Tr.register(LibertyNioServerSocketChannel.class, TCPMessageConstants.NETTY_TRACE_NAME,
                                                         TCPMessageConstants.TCP_BUNDLE);

    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = javaChannel().accept();
        try {
            if(ch != null){
                buf.add(new LibertyNioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to create a new channel from an accepted socket. " + t);
            }

            try {
                ch.close();
            } catch (Throwable t2){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to close a socket. " + t2);
                }
            }
        }

        return 0;
    }
    
}
