/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 * This class is created for use with the com.ibm.ws.webcontainer.security_fat.features and com.ibm.ws.ejbcontainer.security_fat.features
 * dynamic security tests. With BasicAuthClientWithRetry, if the servlet access returns with an error 404 and the
 * 404 is not expected then servlet access is retried once.
 */
public class BasicAuthClientWithRetry extends BasicAuthClient {

    public BasicAuthClientWithRetry(LibertyServer server, String realm,
                                    String servletName, String contextRoot) {
        super(server, realm, servletName, contextRoot);
        retryMode = true;
    }

    public BasicAuthClientWithRetry(LibertyServer server) {
        super(server, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
        retryMode = true;
    }

}
