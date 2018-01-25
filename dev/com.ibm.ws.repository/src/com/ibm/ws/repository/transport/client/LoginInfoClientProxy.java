/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.client;

import java.net.URL;

/**
 *
 */
public class LoginInfoClientProxy {

    URL proxyURL;

    /**
     * The URL has been validated by the resource layer no need to check it again.
     *
     * @param proxyURL - the proxy url and port
     */
    public LoginInfoClientProxy(URL proxyURL) {
        this.proxyURL = proxyURL;
    }

    /**
     * @return the proxyURL
     */
    public URL getProxyURL() {
        return proxyURL;
    }

    /**
     * Is this an http/https proxy ?
     *
     * @return boolean - whether the proxyURL is http/https
     */
    public boolean isHTTPorHTTPS() {
        if (proxyURL.getProtocol().equalsIgnoreCase("HTTP") ||
            proxyURL.getProtocol().equalsIgnoreCase("HTTPS")) {
            return true;
        } else {
            return false;
        }
    }
}
