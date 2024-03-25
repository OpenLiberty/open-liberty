/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ssl;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Properties;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.ssl.config.ProtocolHelper;

/**
 * A wrapper class that binds Liberty's SSL configuration in the {@code server.xml} to the
 * eventual {@link Socket}.
 *
 * This class should preserve the functionality of the current {@link SSLEngine} and
 * only apply apply the config to the eventual {@link Socket}.
 */
public class LibertySSLEngineWrapper extends SSLEngine {

    private static final TraceComponent tc = Tr.register(LibertySSLEngineWrapper.class, "SSL", "com.ibm.ws.ssl");

    // The actual SSLEngine that does all of the work
    private SSLEngine delegate = null;

    private final Properties props;

    private final ProtocolHelper protocolHelper = new ProtocolHelper();

    public LibertySSLEngineWrapper(SSLEngine sslEngine) {
        delegate = sslEngine;
        props = SSLPropertyUtils.lookupProperties();

        // set default enabled ciphers if they are set in config
        String[] ciphers = getEnabledCipherSuitesFromConfig(props);
        if (ciphers != null) {
            delegate.setEnabledCipherSuites(ciphers);
        }

        // set default enabled protocols if they are set in config
        String[] protocols = getEnabledProtocolsFromConfig(props);
        if (protocols != null) {
            delegate.setEnabledProtocols(protocols);
        }
    }

    public LibertySSLEngineWrapper(SSLEngine sslEngine, String alias) {
        delegate = sslEngine;
        props = SSLPropertyUtils.lookupProperties(alias);

        // set default enabled ciphers if they are set in config
        String[] ciphers = getEnabledCipherSuitesFromConfig(props);
        if (ciphers != null) {
            delegate.setEnabledCipherSuites(ciphers);
        }

        // set default enabled protocols if they are set in config
        String[] protocols = getEnabledProtocolsFromConfig(props);
        if (protocols != null) {
            delegate.setEnabledProtocols(protocols);
        }
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
        return delegate.wrap(srcs, offset, length, dst);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
        return delegate.unwrap(src, dsts, offset, length);
    }

    @Override
    public Runnable getDelegatedTask() {
        return delegate.getDelegatedTask();
    }

    @Override
    public void closeInbound() throws SSLException {
        delegate.closeInbound();
    }

    @Override
    public boolean isInboundDone() {
        return delegate.isInboundDone();
    }

    @Override
    public void closeOutbound() {
        delegate.closeOutbound();
    }

    @Override
    public boolean isOutboundDone() {
        return delegate.isOutboundDone();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        String securityLevel = props.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "securityLevel from properties is " + securityLevel);
        if (securityLevel == null)
            securityLevel = "HIGH";

        return Constants.adjustSupportedCiphersToSecurityLevel(delegate.getSupportedCipherSuites(), securityLevel);
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        delegate.setEnabledCipherSuites(suites);
    }

    /*
     * This is how we parse the cipher suites in {@link SSLConfigManager.getCipherList()}
     */
    private String[] getEnabledCipherSuitesFromConfig(Properties props) {
        String cipherString = props.getProperty(Constants.SSLPROP_ENABLED_CIPHERS);

        if (cipherString != null) {
            return cipherString.split("\\s+");
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "com.ibm.ssl.enabledCipherSuites has not been configured.");

            return null;
        }
    }

    @Override
    public String[] getSupportedProtocols() {
        return delegate.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        delegate.setEnabledProtocols(protocols);
    }

    private String[] getEnabledProtocolsFromConfig(Properties props) {
        String sslProtocol = props.getProperty(Constants.SSLPROP_PROTOCOL);
        return protocolHelper.getSSLProtocol(sslProtocol);
    }

    @Override
    public SSLSession getSession() {
        return delegate.getSession();
    }

    @Override
    public void beginHandshake() throws SSLException {
        delegate.beginHandshake();
    }

    @Override
    public HandshakeStatus getHandshakeStatus() {
        return delegate.getHandshakeStatus();
    }

    @Override
    public void setUseClientMode(boolean mode) {
        delegate.setUseClientMode(mode);
    }

    @Override
    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        delegate.setNeedClientAuth(need);
    }

    @Override
    public boolean getNeedClientAuth() {
        return delegate.getNeedClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean want) {
        delegate.setWantClientAuth(want);
    }

    @Override
    public boolean getWantClientAuth() {
        return delegate.getWantClientAuth();
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        delegate.setEnableSessionCreation(flag);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }
}
