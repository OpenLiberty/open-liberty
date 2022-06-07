/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.internal.discovery;

import javax.net.ssl.SSLSocketFactory;

/**
 *
 */
public class DiscoveryHandler {

    SSLSocketFactory sslSocketFactory;

    public DiscoveryHandler(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public void fetchDiscoveryData(String discoveryUrl, boolean hostNameVerificationEnabled) {
        if (hostNameVerificationEnabled) {

        } else {

        }
        if (!isValidDiscoveryUrl(discoveryUrl)) {
            // log error
        }

    }

    private boolean isValidDiscoveryUrl(String discoveryUrl) {
        return discoveryUrl != null && discoveryUrl.startsWith("https");
    }

}
