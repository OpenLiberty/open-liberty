/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channel.ssl.internal.exception.SocketEstablishedSSLException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;

/**
 * This class represents the SSL context.
 */
public class SSLConnectionContextImpl implements SSLConnectionContext {

    /** Trace component for WAS */
    protected static final TraceComponent tc =
                    Tr.register(SSLConnectionContextImpl.class,
                                SSLChannelConstants.SSL_TRACE_NAME,
                                SSLChannelConstants.SSL_BUNDLE);

    // Note: cannot save ref to sslEngine since it could change
    /** SSL connection link associated with this connection. */
    private SSLConnectionLink sslConnLink = null;
    /** Whether this connection is inbound or outbound. */
    private boolean isOutbound = false;

    /**
     * Constructor.
     * 
     * @param connLink
     * @param connectionOutbound
     */
    public SSLConnectionContextImpl(SSLConnectionLink connLink, boolean connectionOutbound) {
        this.sslConnLink = connLink;
        this.isOutbound = connectionOutbound;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getEnabledCipherSuites()
     */
    public String[] getEnabledCipherSuites() {
        return this.sslConnLink.getSSLEngine().getEnabledCipherSuites();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getEnabledProtocols()
     */
    public String[] getEnabledProtocols() {
        return this.sslConnLink.getSSLEngine().getEnabledProtocols();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#setEnabledProtocols(java.lang.String[])
     */
    public void setEnabledProtocols(String args[]) {
        this.sslConnLink.getSSLEngine().setEnabledProtocols(args);
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getEnableSessionCreation()
     */
    public boolean getEnableSessionCreation() {
        return this.sslConnLink.getSSLEngine().getEnableSessionCreation();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getNeedClientAuth()
     */
    public boolean getNeedClientAuth() {
        return this.sslConnLink.getSSLEngine().getNeedClientAuth();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getSession()
     */
    public SSLSession getSession() {
        return this.sslConnLink.getSSLEngine().getSession();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getUseClientMode()
     */
    public boolean getUseClientMode() {
        return this.sslConnLink.getSSLEngine().getUseClientMode();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#getWantClientAuth()
     */
    public boolean getWantClientAuth() {
        return this.sslConnLink.getSSLEngine().getWantClientAuth();
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#setEnabledCipherSuites(java.lang.String[])
     */
    public void setEnabledCipherSuites(String[] suites) throws SSLException {
        if (this.isOutbound) {
            this.sslConnLink.getSSLEngine().setEnabledCipherSuites(suites);
        } else {
            throw new SocketEstablishedSSLException("Error, established setting cannot be modified.");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#setEnableSessionCreation(boolean)
     */
    public void setEnableSessionCreation(boolean flag) throws SSLException {
        if (this.isOutbound) {
            this.sslConnLink.getSSLEngine().setEnableSessionCreation(flag);
        } else {
            throw new SocketEstablishedSSLException("Error, established setting cannot be modified.");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#setNeedClientAuth(boolean)
     */
    public void setNeedClientAuth(boolean flag) throws SSLException {
        if (this.isOutbound) {
            this.sslConnLink.getSSLEngine().setNeedClientAuth(flag);
        } else {
            throw new SocketEstablishedSSLException("Error, established setting cannot be modified.");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#setWantClientAuth(boolean)
     */
    public void setWantClientAuth(boolean flag) throws SSLException {
        if (this.isOutbound) {
            this.sslConnLink.getSSLEngine().setWantClientAuth(flag);
        } else {
            throw new SocketEstablishedSSLException("Error, established setting cannot be modified.");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#setUseClientMode(boolean)
     */
    public void setUseClientMode(boolean flag) throws SSLException {
        if (this.isOutbound) {
            this.sslConnLink.getSSLEngine().setUseClientMode(flag);
        } else {
            throw new SocketEstablishedSSLException("Error, established setting cannot be modified.");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.SSLConnectionContext#renegotiate()
     */
    public void renegotiate() {
        try {
            this.sslConnLink.getSSLEngine().beginHandshake();
        } catch (SSLException se) {
            FFDCFilter.processException(se,
                                        getClass().getName() + ".renegotiate", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error while attempting handshake renegotiation; " + se);
            }
        }
    }

}
