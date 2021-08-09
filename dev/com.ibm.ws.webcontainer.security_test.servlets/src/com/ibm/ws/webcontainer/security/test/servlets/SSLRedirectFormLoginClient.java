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
public class SSLRedirectFormLoginClient extends FormLoginClient implements SSLServletClient {
    private static final Class<?> c = SSLRedirectFormLoginClient.class;

    public SSLRedirectFormLoginClient(String host, int port) {
        this(host, port, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLRedirectFormLoginClient(String host, int port, String servletName, String contextRoot) {
        super(host, port, true, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);

        SSLHelper.establishSSLContext(client, port, server);
    }

    public SSLRedirectFormLoginClient(LibertyServer server) {
        this(server, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public SSLRedirectFormLoginClient(LibertyServer server, String servletName, String contextRoot) {
        super(server, true, servletName, contextRoot);
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
