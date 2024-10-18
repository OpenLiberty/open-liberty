/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandler;
import io.openliberty.http.options.SslOption;
import io.openliberty.http.utils.HttpConfigUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is a custom SSL handler for Liberty that extends Netty's SslHandler.
 * This handler provides additional functionalities for the server's SSL configurations. 
 */
public class LibertySslHandler extends SslHandler {
    private static final TraceComponent tc = Tr.register(LibertySslHandler.class, "SSLChannel", "com.ibm.ws.channel.ssl.internal.resources.SSLChannelMessages");

    private final boolean suppressLogError;
    private final long maxLogEntries;
    //TODO: should this be serverwide or endpointwide? 
    private static final AtomicLong numberOfLogEntries = new AtomicLong(0);
    private static final AtomicBoolean loggingStopped = new AtomicBoolean(false);

    /**
     * Constructs a new LibertySslHandler.
     * 
     * @param engine The SSLEngine to be used by this handler.
     * @param sslOptions A map containing SSL configuration options.
     */
    public LibertySslHandler(SSLEngine engine, NettyHttpChannelConfig config) {
        super(engine);

        this.suppressLogError = config.suppressHandshakeError();
        this.maxLogEntries = config.suppressHandshakeErrorCount();
    }

    /**
     * Handles exceptions caught during SSL processing.
     * If the exception is related to the SSL handshake, the noteHandshakeError will
     * handle its logging. 
     * 
     * @param ctx The ChannelHandlerContext in which the exception was caught.
     * @param cause The Throwable representing the caught exception.
     * @throws Exception if there's an error during exception handling.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof DecoderException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof SSLHandshakeException) {
            InetSocketAddress local = (InetSocketAddress) ctx.channel().localAddress();
            InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
            noteHandshakeError((Exception) cause, local.getAddress(), local.getPort(), remote.getAddress(), remote.getPort());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Non-SSL exception caught: " + cause.getMessage());
            }
        }
        super.exceptionCaught(ctx, cause);
    }

    /**
     * This method manages the number of log entries created for SSL handshake errors. 
     * If the error suggests a plaintext connection attempt on a secure port, a more
     * specific error message is provided.
     * 
     * @param failure The exception that occurred during the SSL handshake.
     * @param localAddr The local address involved in the failed handshake.
     * @param localPort The local port involved in the failed handshake.
     * @param remoteAddr The remote address involved in the failed handshake.
     * @param remotePort The remote address involved in the failed handshake.
     */
    private void noteHandshakeError(Exception failure, InetAddress localAddr, int localPort, InetAddress remoteAddr, int remotePort) {
        //TODO: set limit?
        long logCount = numberOfLogEntries.incrementAndGet();
        Exception ssle = failure;

        if (failure.getMessage().contains("plaintext connection?")) {
            ssle = new SSLException("The WebSphere server received an unencrypted inbound communication on a secure connection. " +
                                    "This does not indicate a problem with the WebSphere server. To resolve the issue, configure " +
                                    "the client to use SSL or to connect to a port on the WebSphere server that does not require SSL.", failure.getCause());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "noteHandshakeError (" + logCount + "): " + ssle.getMessage(), ssle);
        }

        if (!suppressLogError) {
            if (logCount <= maxLogEntries) {
                Tr.error(tc, "CWWKO0801E", ssle, remoteAddr.getHostAddress(), remotePort, localAddr.getHostAddress(), localPort);
            } else if (!loggingStopped.get() && (logCount > maxLogEntries)) {
                loggingStopped.set(true);
                Tr.info(tc, "CWWKO0804I");
            }
        }
    }

}