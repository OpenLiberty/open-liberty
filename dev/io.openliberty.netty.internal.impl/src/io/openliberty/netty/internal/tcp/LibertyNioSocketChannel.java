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

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.OptionalLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

/**  
 * This class retains all functionality of Netty's {@link NioSocketChannel}, while also storing
 * the underlying socket as a channel attribute. 
 **/
public class LibertyNioSocketChannel extends NioSocketChannel{

    private static final TraceComponent tc = Tr.register(LibertyNioSocketChannel.class, TCPMessageConstants.NETTY_TRACE_NAME,
                                                         TCPMessageConstants.TCP_BUNDLE);

    private final AttributeKey<Socket> SOCKET_HANDLE = AttributeKey.valueOf("SocketHandleKey");

    public LibertyNioSocketChannel(){
        super();
        installHandle();
    }

    public LibertyNioSocketChannel(SocketChannel socket){
        super(socket);
        installHandle();
    }

    public LibertyNioSocketChannel(Channel parent, SocketChannel socket){
        super(parent, socket);
        installHandle();
    }

    private void installHandle(){
        SocketChannel ch = javaChannel();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Installing socket handle to NIO channel:  " + ch);
        }
        attr(SOCKET_HANDLE).set(ch.socket());
    }    
}
