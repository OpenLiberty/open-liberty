/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

public class SSLRedirectBasicAuthClientWithRetry extends SSLRedirectBasicAuthClient {

    public SSLRedirectBasicAuthClientWithRetry(LibertyServer server, String realm,
                                               String servletName, String contextRoot) {
        super(server, realm, servletName, contextRoot);
        retryMode = true;
    }

    public SSLRedirectBasicAuthClientWithRetry(LibertyServer server) {
        super(server, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
        retryMode = true;
    }
}
