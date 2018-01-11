/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.test.servlets;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class FormLoginJSPClient extends FormLoginClient {
    public FormLoginJSPClient(String host, int port) {
        super(host, port);
    }

    public FormLoginJSPClient(String host, int port, String servletName, String contextRoot) {
        super(host, port, servletName, contextRoot);
    }

    FormLoginJSPClient(String host, int port, boolean isSSL, String servletName, String contextRoot) {
        super(host, port, isSSL, servletName, contextRoot);
    }

    public FormLoginJSPClient(LibertyServer server) {
        super(server);
    }

    public FormLoginJSPClient(LibertyServer server, String servletName, String contextRoot) {
        super(server, servletName, contextRoot);
    }

    FormLoginJSPClient(LibertyServer server, boolean isSSL, String servletName, String contextRoot) {
        super(server, isSSL, servletName, contextRoot);
    }

    public FormLoginJSPClient(LibertyServer server, boolean isSSL, String servletName, String contextRoot, String servletSpec, String httpProtocol) {
        super(server, isSSL, servletName, contextRoot, servletSpec, httpProtocol);
    }

    /** {@inheritDoc} */
    @Override
    protected String servletURLForLogout() {
        return super.servletURLForLogout().replaceFirst("/JSP", "");
    }
}
