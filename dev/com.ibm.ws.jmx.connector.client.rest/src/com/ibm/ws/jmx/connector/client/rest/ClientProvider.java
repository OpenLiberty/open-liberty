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
package com.ibm.ws.jmx.connector.client.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;
import com.ibm.ws.jmx.connector.client.rest.internal.Connector;

public class ClientProvider implements JMXConnectorProvider, ConnectorSettings {

    public static final String REST_PROTOCOL = "rest";

    public static final String CLIENT_DOMAIN = "com.ibm.ws.jmx.connector.client";
    public static final String REST_CLIENT_DOMAIN = "com.ibm.ws.jmx.connector.client.rest";

    public static final String CONNECTION_TEMPORARILY_LOST = REST_CLIENT_DOMAIN + ".connectionTemporarilyLost";
    public static final String CONNECTION_RESTORED = REST_CLIENT_DOMAIN + ".connectionRestored";
    public static final String CONNECTION_RESTORED_WITH_EXCEPTIONS = REST_CLIENT_DOMAIN + ".connectionRestoredWithExceptions";

    public static final String CONNECTION_ROUTING_DOMAIN = "WebSphere";
    public static final String CONNECTION_ROUTING_NAME = "RoutingContext";
    public static final String CONNECTION_ROUTING_OPERATION_ASSIGN_SERVER = "assignServerContext";
    public static final String CONNECTION_ROUTING_OPERATION_ASSIGN_HOST = "assignHostContext";

    public static final String FILE_TRANSFER_DOMAIN = "WebSphere";
    public static final String FILE_TRANSFER_NAME = "FileTransfer";

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
     */
    @Override
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
        if (serviceURL == null || environment == null)
            throw new NullPointerException();
        if (!ClientProvider.REST_PROTOCOL.equals(serviceURL.getProtocol()))
            throw new MalformedURLException();
        return new Connector(serviceURL, environment);
    }

}
