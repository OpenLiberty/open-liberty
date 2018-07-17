/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channel.ssl.internal.SSLAlpnNegotiator.ThirdPartyAlpnNegotiator;

/**
 *
 */
public class AlpnSupportUtils {

    private static final TraceComponent tc = Tr.register(AlpnSupportUtils.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    private static SSLAlpnNegotiator alpnNegotiator = new SSLAlpnNegotiator();

    /**
     * Try to register supported ALPN protocols for a connection. This checks that:
     * 1. HTTP/2 support is enabled
     * 2. an ALPN provider is available - either via IBM JDK8, JDK9+, grizzly-npn, or jetty-alpn
     *
     * If the conditions are met, "h2" will be registered as a supported protocol for negotiation
     *
     * @param SSLConnectionLink
     * @param engSSLEngineine
     */
    protected static void registerAlpnSupport(SSLConnectionLink connLink, SSLEngine engine) {
        ThirdPartyAlpnNegotiator negotiator = null;
        boolean useAlpn = isH2Active(connLink);
        // try to register with one of the alpn providers.  If useAlpn is false, tell the active ALPN handler to skip negotiation
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "registerAlpnSupportDuringHandshake, h2 protocol enabled: " + useAlpn);
        }
        negotiator = alpnNegotiator.tryToRegisterAlpnNegotiator(engine, connLink, useAlpn);
        connLink.setAlpnNegotiator(negotiator);
    }

    /**
     * This must be called after the SSL handshake has completed. If an ALPN protocol was selected by the available provider,
     * that protocol will be set on the SSLConnectionLink. Also, additional cleanup will be done for some ALPN providers.
     *
     * @param SSLEngine
     * @param SSLConnectionLink
     */
    protected static void getAlpnResult(SSLEngine engine, SSLConnectionLink link) {
        alpnNegotiator.tryToRemoveAlpnNegotiator(link.getAlpnNegotiator(), engine, link);
    }

    /**
     * @param SSLConnectionLink
     * @return true if HTTP/2 is enabled for the SSLConnectionLink
     */
    private static boolean isH2Active(SSLConnectionLink connLink) {
        boolean useAlpn = connLink.isAlpnEnabled();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isH2Active, h2 protocol enabled: " + useAlpn);
        }
        return useAlpn;
    }
}
