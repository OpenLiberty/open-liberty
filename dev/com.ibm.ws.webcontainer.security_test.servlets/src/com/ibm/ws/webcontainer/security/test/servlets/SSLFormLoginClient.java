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
public class SSLFormLoginClient extends FormLoginClient implements SSLServletClient {
    private static final Class<?> c = SSLFormLoginClient.class;

    public SSLFormLoginClient(String host, int port) {
        this(host, port, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLFormLoginClient(String host, int port, String servletName, String contextRoot) {
        super(host, port, true, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);

        SSLHelper.establishSSLContext(client, port, server);
    }

    public SSLFormLoginClient(LibertyServer server) {
        this(server, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLFormLoginClient(LibertyServer server, String servletName, String contextRoot) {
        super(server, true, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);

        SSLHelper.establishSSLContext(client, port, server);
    }

    /**
     * @param myServer
     * @param defaultJspName
     * @param defaultJspContextRoot
     * @param serlvetSpec31
     * @param httpProtocol11
     */
    public SSLFormLoginClient(LibertyServer server, String servletName, String contextRoot, String servletSpec, String httpProtocol) {
        super(server, true, servletName, contextRoot, servletSpec, httpProtocol);

        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);

        SSLHelper.establishSSLContext(client, port, server);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookResetClientState() {
        SSLHelper.establishSSLContext(client, port, server);
    }

}
