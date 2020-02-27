/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import java.net.URL;

import componenttest.topology.impl.LibertyServer;

/**
 * A utility class for JSF tests.
 */
public class JSFUtils {

    protected static final Class<?> c = JSFUtils.class;

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server      - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path        - Additional path information for the request.
     * @return - A fully formed URL.
     * @throws Exception
     */
    public static URL createHttpUrl(LibertyServer server, String contextRoot, String path) throws Exception {
        URL result = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                             "/" + contextRoot + "/" + path);

        return result;
    }
}
