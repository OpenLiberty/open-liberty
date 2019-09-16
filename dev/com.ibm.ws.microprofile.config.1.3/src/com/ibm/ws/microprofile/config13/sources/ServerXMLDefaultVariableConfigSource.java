/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.sources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.sources.DynamicConfigSource;
import com.ibm.ws.microprofile.config.sources.InternalConfigSource;
import com.ibm.ws.microprofile.config13.interfaces.Config13Constants;

/**
 * A ConfigSource which returns default values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" defaultValue="my_value" />
 *
 */
public class ServerXMLDefaultVariableConfigSource extends InternalConfigSource implements DynamicConfigSource {

    private static final TraceComponent tc = Tr.register(ServerXMLDefaultVariableConfigSource.class);
    private BundleContext bundleContext;

    public ServerXMLDefaultVariableConfigSource() {
        super(Config13Constants.SERVER_XML_DEFAULT_VARIABLE_ORDINAL, Tr.formatMessage(tc, "server.xml.variable.config.source"));
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {

        Map<String, String> props = new HashMap<>();
        Map<String, String> serverXMLDefaultVariables = getServerXMLVariables();
        if (serverXMLDefaultVariables != null) {
            props.putAll(serverXMLDefaultVariables);
        }

        return props;
    }

    private Map<String, String> getServerXMLVariables() {

        PrivilegedAction<Map<String, String>> configAction = () -> {
            if (bundleContext == null) {
                bundleContext = OSGiConfigUtils.getBundleContext(getClass());
            }

            Map<String, String> serverXMLDefaultVariables = null;
            if (bundleContext != null) { //bundleContext could be null if not inside an OSGi framework, e.g. unit test
                serverXMLDefaultVariables = OSGiConfigUtils.getDefaultVariablesFromServerXML(bundleContext);
            }

            return serverXMLDefaultVariables;
        };

        Map<String, String> props = AccessController.doPrivileged(configAction);

        return props;
    }

}
