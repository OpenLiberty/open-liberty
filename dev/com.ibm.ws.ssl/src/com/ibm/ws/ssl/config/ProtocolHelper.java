/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.config;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * CertificateEnvHelper
 * <p>
 * This class handles getting certificate from an environment variable
 * </p>
 *
 */
public class ProtocolHelper {
    private static final TraceComponent tc = Tr.register(ProtocolHelper.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    /**
     * Constructor.
     */
    public ProtocolHelper() {
        // do nothing
    }

    // collect the good protocols as we run across them
    private static final List<String> validatedProtocols = new ArrayList<>();

    // get disabled protocols
    private static final List<String> disabledList = new ArrayList<>();

    // build disabled protocols list
    private static boolean builtDisabledList = false;

    /**
     *
     * Method called to check the protocol value provided is good. True is returned if the protocol value is good and false
     * otherwise. If a multi-protocol is provided then each protocol value in the list is checked. If there is a protocol
     * value that is not acceptable for a list or a protocol that is not valid for the environment a SSLException is thrown.
     *
     *
     * @param sslProtocol
     * @return
     * @throws SSLException
     */
    public void checkProtocolValueGood(String sslProtocol) throws SSLException, UnsupportedOperationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkProtocolValueGood", sslProtocol);

        String[] protocols = sslProtocol.split(",");

        List<String> allowedProtocols;
        boolean fips140_3Enabled = CryptoUtils.isFips140_3Enabled();
        if (fips140_3Enabled) {
            Tr.debug(tc, "FIPS is enabled, only allowing TLSv1.2 and TLSv1.3");
            allowedProtocols = Constants.FIPS_140_3_PROTOCOLS;
        } else {
            allowedProtocols = Constants.MULTI_PROTOCOL_LIST;
        }

        if (protocols.length > 1) {
            for (String protocol : protocols) {
                if (allowedProtocols.contains(protocol)) {
                    if (validatedProtocols.contains(protocol))
                        continue;
                    else {
                        checkProtocol(protocol);
                        validatedProtocols.add(protocol);
                    }
                } else {
                    Tr.error(tc, "ssl.protocol.error.CWPKI0832E", protocol);
                    throw new SSLException("Protocol provided is not appropriate for a protocol list.");
                }
            }
        } else {
            String protocol = protocols[0];
            if (!validatedProtocols.contains(protocol)) {
//                 TODO: uncomment the following once SSL tests have been updated for FIPS 140-3
//                if (fips140_3Enabled && !allowedProtocols.contains(protocol)) {
//                    Tr.error(tc, "ssl.protocol.error.CWPKI0832E", protocol);
//                    throw new SSLException("Protocol provided is not appropriate for a protocol list.");
//                }
                checkProtocol(protocol);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkProtocolValueGood");
        return;
    }

    /**
     *
     * Method to see if the protocol is valid to this JVM. Just calling SSLContext getInstance with the protocol value.
     * If the protocol is not good on this JVM then an throwable is thrown.
     *
     * @param protocol
     * @return
     * @throws SSLException
     */
    private void checkProtocol(String protocol) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkProtocol", protocol);

        try {
            SSLContext.getInstance(protocol);
        } catch (Throwable t) {
            // Just continue
            String tMsg = t.getMessage();
            Tr.error(tc, "ssl.protocol.error.CWPKI0831E", new Object[] { protocol, tMsg });
            throw new SSLException("Error checking checking for valid protocol: " + tMsg);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkProtocol", protocol);
    }

    /**
     * Check to see if this protocol is disabled. Get the list of disabled algorithms
     * then see if any of the single valued protocols is in it.
     */
    private boolean isProtocolDisabled(String protocol) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isProtocolDisabled", protocol);
        boolean isDisabled = false;

        if (!builtDisabledList) {
            String disabled = Security.getProperty("jdk.tls.disabledAlgorithms");
            String[] algorithms = disabled.split(",");
            for (String algorithm : algorithms) {
                if (Constants.MULTI_PROTOCOL_LIST.contains(algorithm.trim())) {
                    disabledList.add(algorithm.trim());
                }
            }
            builtDisabledList = true;
        }

        if (!disabledList.isEmpty() && disabledList.contains(protocol)) {
            isDisabled = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isProtocolDisabled", protocol);
        return isDisabled;
    }

    public String[] getSSLProtocol(String protocol) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSSLProtocol");
        }

        // protocol(s) need to be in an array
        String[] protocols = protocol.split(",");

        // we only want to set the protocol on the socket if it a specific protocol name
        // don't set to TLS or SSL
        if (protocols.length == 1) {
            if (protocols[0].equals(Constants.PROTOCOL_TLS) || protocols[0].equals(Constants.PROTOCOL_SSL)) {
                protocols = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSSLProtocol " + protocols);
        }
        return protocols;
    }
}
