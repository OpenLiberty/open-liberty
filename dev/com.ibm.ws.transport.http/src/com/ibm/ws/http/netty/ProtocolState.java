/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import com.ibm.ws.http.internal.netty.protocol.ProtocolChangedEvent;
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.AttributeKey;

/** Stores each connection's HTTP protocol and enforces legal transitions. */
public final class ProtocolState {

    private static final AttributeKey<ProtocolName> PROTOCOL = AttributeKey.valueOf("protocol");

    private ProtocolState() {
    }

    /** How the connection protocol was selected. */
    public enum ProtocolSource {
        ALPN_HTTP1(ProtocolName.HTTP1),
        ALPN_HTTP2(ProtocolName.HTTP2),
        CLEARTEXT_HTTP10(ProtocolName.HTTP10),
        CLEARTEXT_HTTP1(ProtocolName.HTTP1),
        TLS_HTTP10(ProtocolName.HTTP10),
        TLS_HTTP1(ProtocolName.HTTP1),
        OUTBOUND_HTTP1_HANDSHAKE(ProtocolName.HTTP1),
        H2C_UPGRADE(ProtocolName.HTTP2),
        H2C_PRIOR_KNOWLEDGE(ProtocolName.HTTP2),
        WEBSOCKET_UPGRADE(ProtocolName.WEBSOCKET);

        private final ProtocolName protocol;

        ProtocolSource(ProtocolName protocol) {
            this.protocol = protocol;
        }
    }

    /** Read the current connection protocol. */
    public static ProtocolName current(Channel channel) {
        ProtocolName protocol = connectionChannel(channel).attr(PROTOCOL).get();
        return protocol == null ? ProtocolName.UNKNOWN : protocol;
    }

    /**
     * Set the connection protocol or apply a legal upgrade. Invalid transitions close
     * the connection before throwing.
     */
    public static ProtocolName establish(Channel channel, ProtocolName next,
                                         ProtocolSource source) {
        Channel connection = connectionChannel(channel);
        if (next == null || source == null || source.protocol != next) {
            return rejectProtocolTransition(connection, current(connection), next, source);
        }

        ProtocolName current = current(connection);
        if (current == next) {
            return current;
        }

        boolean legal = current == ProtocolName.UNKNOWN
                        || (current == ProtocolName.HTTP1 && next == ProtocolName.HTTP2
                            && source == ProtocolSource.H2C_UPGRADE)
                        || ((current == ProtocolName.HTTP1 || current == ProtocolName.HTTP10)
                            && next == ProtocolName.WEBSOCKET
                            && source == ProtocolSource.WEBSOCKET_UPGRADE);
        if (!legal || (current == ProtocolName.UNKNOWN && next == ProtocolName.WEBSOCKET)) {
            return rejectProtocolTransition(connection, current, next, source);
        }

        connection.attr(PROTOCOL).set(next);
        connection.pipeline().fireUserEventTriggered(new ProtocolChangedEvent(current, next, source));
        return next;
    }

    private static Channel connectionChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        return channel instanceof Http2StreamChannel ? channel.parent() : channel;
    }

    private static ProtocolName rejectProtocolTransition(Channel connection, ProtocolName current,
                                                         ProtocolName next, ProtocolSource source) {
        connection.close();
        throw new IllegalStateException("Contradictory protocol transition " + current + " -> " + next
                                        + " from " + source);
    }
}
