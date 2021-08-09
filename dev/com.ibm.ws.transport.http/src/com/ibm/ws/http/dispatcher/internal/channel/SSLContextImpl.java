/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.channel;

import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.http.SSLContext;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;

/**
 * Implementation of the publicly accessible SSL information for a secure
 * HTTP connection.
 */
@Trivial
public class SSLContextImpl implements SSLContext {
    private SSLConnectionContext context = null;

    /**
     * Constructor wrapping a given SSL context object.
     * 
     * @param ssl
     */
    public SSLContextImpl(SSLConnectionContext ssl) {
        this.context = ssl;
    }

    /*
     * @see com.ibm.websphere.http.SSLContext#getEnabledCipherSuites()
     */
    @Override
    public String[] getEnabledCipherSuites() {
        return this.context.getEnabledCipherSuites();
    }

    /*
     * @see com.ibm.websphere.http.SSLContext#getEnabledProtocols()
     */
    @Override
    public String[] getEnabledProtocols() {
        return this.context.getEnabledProtocols();
    }

    /*
     * @see com.ibm.websphere.http.SSLContext#getNeedClientAuth()
     */
    @Override
    public boolean getNeedClientAuth() {
        return this.context.getNeedClientAuth();
    }

    /*
     * @see com.ibm.websphere.http.SSLContext#getSession()
     */
    @Override
    public SSLSession getSession() {
        return this.context.getSession();
    }

    /*
     * @see com.ibm.websphere.http.SSLContext#getUseClientMode()
     */
    @Override
    public boolean getUseClientMode() {
        return this.context.getUseClientMode();
    }

    /*
     * @see com.ibm.websphere.http.SSLContext#getWantClientAuth()
     */
    @Override
    public boolean getWantClientAuth() {
        return this.context.getWantClientAuth();
    }

}
