/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.test.servlets;

import java.util.logging.Logger;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class SSLBasicAuthClient extends BasicAuthClient implements SSLServletClient {
    private static final Class<?> c = SSLBasicAuthClient.class;

    public SSLBasicAuthClient(String host, int port) {
        this(host, port, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLBasicAuthClient(String host, int port, String realm) {
        this(host, port, realm, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLBasicAuthClient(String host, int port, String realm,
                              String servletName, String contextRoot) {
        this(host, port, realm, servletName, contextRoot, null, null, null, null);
    }

    public SSLBasicAuthClient(String host, int port, String realm,
                              String servletName, String contextRoot,
                              String ksPath, String ksPassword,
                              String tsPath, String tsPassword) {
        this(host, port, realm, servletName, contextRoot, ksPath, ksPassword, tsPath, tsPassword, null);
    }

    public SSLBasicAuthClient(String host, int port, String realm,
                              String servletName, String contextRoot,
                              String ksPath, String ksPassword,
                              String tsPath, String tsPassword,
                              String sslProtocol) {
        super(host, port, true, realm, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);

        SSLHelper.establishSSLContext(client, port, server, ksPath, ksPassword, tsPath, tsPassword, sslProtocol);
    }

    public SSLBasicAuthClient(LibertyServer server) {
        this(server, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLBasicAuthClient(LibertyServer server, String realm) {
        this(server, realm, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLBasicAuthClient(LibertyServer server, String realm,
                              String servletName, String contextRoot) {
        this(server, realm, servletName, contextRoot, null, null, null, null);
    }

    public SSLBasicAuthClient(LibertyServer server, String realm,
                              String servletName, String contextRoot,
                              String ksPath, String ksPassword,
                              String tsPath, String tsPassword) {
        this(server, realm, servletName, contextRoot, ksPath, ksPassword, tsPath, tsPassword, null);
    }

    public SSLBasicAuthClient(LibertyServer server, String realm,
                              String servletName, String contextRoot,
                              String ksPath, String ksPassword,
                              String tsPath, String tsPassword,
                              String sslProtocol) {
        super(server, true, realm, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);

        SSLHelper.establishSSLContext(client, port, server, ksPath, ksPassword, tsPath, tsPassword, sslProtocol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookResetClientState() {
        SSLHelper.establishSSLContext(client, port, server);
    }

}
