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
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

class DynamicURL {

    private static final Logger logger = Logger.getLogger(DynamicURL.class.getName());

    private final String name;
    private final Connector connector;

    DynamicURL(Connector connector, String name) {
        this.name = name;
        this.connector = connector;
    }

    URL getURL() throws MalformedURLException {
        String[] endpoint = RESTMBeanServerConnection.splitEndpoint(connector.getCurrentEndpoint());
        URL retURL = new URL("https", endpoint[0], Integer.valueOf(endpoint[1]), getName());

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "getURL", "URL: " + retURL.toString());
        }

        return retURL;
    }

    String getName() {
        return name;
    }
}
