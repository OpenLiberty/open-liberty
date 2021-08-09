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
package com.ibm.ws.repository.connections;

import java.net.URL;

import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryIllegalArgumentException;
import com.ibm.ws.repository.transport.client.LoginInfoClientProxy;

/**
 * Class used to define proxy server locations and (if necessary) credentials.
 * Used by {@link RepositoryConnectionList}
 */
public class RestRepositoryConnectionProxy {

    final URL proxyURL;

    /**
     * Constructor for a proxy
     *
     * @param proxyURL - the proxy url including port e.g. http://xxxx.ibm.com:3128
     * @throws RepositoryException
     */
    public RestRepositoryConnectionProxy(URL proxyURL) throws RepositoryIllegalArgumentException {
        this.proxyURL = setURL(proxyURL);
    }

    /**
     * @return the proxyURL
     */
    public URL getProxyURL() {
        return proxyURL;
    }

    /**
     * Transfer the proxy information to the client layer.
     *
     * @return LoginInfoClientProxy
     */
    public LoginInfoClientProxy getLoginInfoClientProxy() {
        return new LoginInfoClientProxy(proxyURL);
    }

    /**
     * Rather than setting the port directly, verify that the proxy URL did contain a port
     * and throw an exception if it did not. This avoids problems later.
     *
     * @param url
     * @return
     */
    private URL setURL(URL url) throws RepositoryIllegalArgumentException {
        int port = url.getPort();
        if (port == -1) {
            throw new RepositoryIllegalArgumentException("Bad proxy URL", new IllegalArgumentException("Proxy URL does not contain a port"));
        }
        String host = url.getHost();
        if (host.equals("")) {
            throw new RepositoryIllegalArgumentException("Bad proxy URL", new IllegalArgumentException("Proxy URL does not contain a host"));
        }
        return url;
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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // convert URL toString before creating the hashcode due to findbugs warnings
        result = prime * result + ((proxyURL == null) ? 0 : proxyURL.toString().hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RestRepositoryConnectionProxy other = (RestRepositoryConnectionProxy) obj;
        if (proxyURL == null) {
            if (other.proxyURL != null)
                return false;
        } // convert URL toString before doing an equals due to findbugs warnings
        else if (!proxyURL.toString().equals(other.proxyURL.toString()))
            return false;
        return true;
    }
}
