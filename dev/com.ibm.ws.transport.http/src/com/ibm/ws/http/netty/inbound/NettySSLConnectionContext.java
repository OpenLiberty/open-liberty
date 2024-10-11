/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

/**
 *
 */
public class NettySSLConnectionContext implements SSLConnectionContext {

    /** Trace component for WAS */
    protected static final TraceComponent tc = Tr.register(NettySSLConnectionContext.class,
                                                           "SSLChannel",
                                                           "com.ibm.ws.channel.ssl.internal.resources.SSLChannelMessages");

    private final Channel channel;
    private final boolean isOutbound;
    private String alpnProtocol;

    public NettySSLConnectionContext(Channel channel, boolean isOutbound) {
        this.channel = channel;
        this.isOutbound = isOutbound;
    }

    private SslHandler getSslHandler() {
        return channel.pipeline().get(SslHandler.class);
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return getSslHandler().engine().getEnabledCipherSuites();
    }

    @Override
    public String[] getEnabledProtocols() {
        return getSslHandler().engine().getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        getSslHandler().engine().setEnabledProtocols(protocols);

    }

    @Override
    public boolean getEnableSessionCreation() {
        return getSslHandler().engine().getEnableSessionCreation();
    }

    @Override
    public boolean getNeedClientAuth() {
        return getSslHandler().engine().getNeedClientAuth();
    }

    @Override
    public SSLSession getSession() {
        return getSslHandler().engine().getSession();
    }

    @Override
    public boolean getUseClientMode() {
        return getSslHandler().engine().getUseClientMode();
    }

    @Override
    public boolean getWantClientAuth() {
        return getSslHandler().engine().getWantClientAuth();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) throws SSLException {
        if (isOutbound) {
            getSslHandler().engine().setEnabledCipherSuites(suites);
        } else {
            throw new SSLException("Error, established setting cannot be modified.");
        }

    }

    @Override
    public void setEnableSessionCreation(boolean flag) throws SSLException {
        if (isOutbound) {
            getSslHandler().engine().setEnableSessionCreation(flag);
        } else {
            throw new SSLException("error, established setting cannot be modified.");
        }

    }

    @Override
    public void setNeedClientAuth(boolean need) throws SSLException {
        if (isOutbound) {
            getSslHandler().engine().setNeedClientAuth(need);
        } else {
            throw new SSLException("Error, established setting cannot be modified.");
        }

    }

    @Override
    public void setWantClientAuth(boolean want) throws SSLException {
        if (isOutbound) {
            getSslHandler().engine().setWantClientAuth(want);
        } else {
            throw new SSLException("Error, established setting cannot be modificed.");
        }

    }

    @Override
    public void setUseClientMode(boolean mode) throws SSLException {
        if (isOutbound) {
            getSslHandler().engine().setUseClientMode(mode);
        } else {
            throw new SSLException("Error, established setting cannot be modified.");
        }

    }

    @Override
    public void renegotiate() {
        try {
            getSslHandler().engine().beginHandshake();
        } catch (SSLException e) {
            FFDCFilter.processException(e,
                                        getClass().getName() + ".renegotiate", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error while attempting handshake renegotiation; " + e);
            }
        }

    }

    @Override
    public String getAlpnProtocol() {
        return alpnProtocol;
    }

    @Override
    public void setAlpnProtocol(String protocol) {
        this.alpnProtocol = protocol;

    }

}
