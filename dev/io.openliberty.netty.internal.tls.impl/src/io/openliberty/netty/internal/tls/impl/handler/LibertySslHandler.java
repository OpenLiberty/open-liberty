/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tls.impl.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.openliberty.netty.internal.tls.impl.options.SslOption;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
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
    private static final AttributeKey<Long> SSL_EXCEPTION_LOG_ENTRIES = AttributeKey.valueOf("SSL_EXCEPTION_LOG_ENTRIES");
    private static final AttributeKey<AtomicBoolean> SSL_EXCEPTION_LOGGING_STOPPED = AttributeKey.valueOf("SSL_EXCEPTION_LOGGING_STOPPED");

    /**
     * Constructs a new LibertySslHandler.
     * 
     * @param engine The SSLEngine to be used by this handler.
     * @param sslOptions A map containing SSL configuration options.
     */
    public LibertySslHandler(SSLEngine engine, Map<String, Object> sslOptions, Channel channel) {
        super(engine);
        Boolean enforceCipherOrder = SslOption.ENFORCE_CIPHER_ORDER.parse(sslOptions);
        engine().getSSLParameters().setUseCipherSuitesOrder(enforceCipherOrder);
        this.suppressLogError = SslOption.SUPPRESS_HANDSHAKE_ERRORS.parse(sslOptions);
        this.maxLogEntries = SslOption.SUPPRESS_HANDSHAKE_ERRORS_COUNT.parse(sslOptions);
        if(Objects.nonNull(channel.parent())) {
            channel.parent().attr(SSL_EXCEPTION_LOG_ENTRIES).setIfAbsent(new Long(0));
            channel.parent().attr(SSL_EXCEPTION_LOGGING_STOPPED).setIfAbsent(new AtomicBoolean(false));
        } else {
            channel.attr(SSL_EXCEPTION_LOG_ENTRIES).setIfAbsent(new Long(0));
            channel.attr(SSL_EXCEPTION_LOGGING_STOPPED).setIfAbsent(new AtomicBoolean(false));
        }
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
            noteHandshakeError((Exception) cause, local.getAddress(), local.getPort(), remote.getAddress(), remote.getPort(), ctx.channel());
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
    private void noteHandshakeError(Exception failure, InetAddress localAddr, int localPort, InetAddress remoteAddr, int remotePort, Channel channel) {
        long logCount;
        AtomicBoolean loggingStopped;
        if(Objects.nonNull(channel.parent())) {
            logCount = channel.parent().attr(SSL_EXCEPTION_LOG_ENTRIES).get();
            channel.parent().attr(SSL_EXCEPTION_LOG_ENTRIES).set(++logCount);
            loggingStopped = channel.parent().attr(SSL_EXCEPTION_LOGGING_STOPPED).get();
        } else {
            logCount = channel.attr(SSL_EXCEPTION_LOG_ENTRIES).get();
            channel.attr(SSL_EXCEPTION_LOG_ENTRIES).set(++logCount);
            loggingStopped = channel.parent().attr(SSL_EXCEPTION_LOGGING_STOPPED).get();
        }
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
                Tr.error(tc, "handshake.failure", ssle, remoteAddr.getHostAddress(), remotePort, localAddr.getHostAddress(), localPort);
            } else if (!loggingStopped.get() && (logCount > maxLogEntries)) {
                loggingStopped.set(true);
                Tr.info(tc, "handshake.failure.stop.logging");
                
            }
        }
    }

}