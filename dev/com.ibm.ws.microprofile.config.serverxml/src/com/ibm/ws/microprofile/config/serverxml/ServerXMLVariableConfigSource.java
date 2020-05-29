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
package com.ibm.ws.microprofile.config.serverxml;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.common.InternalConfigSource;

/**
 * A ConfigSource which returns values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" value="my_value" />
 *
 */
public class ServerXMLVariableConfigSource extends InternalConfigSource implements ConfigSource {

    private static final TraceComponent tc = Tr.register(ServerXMLVariableConfigSource.class);
    private final ConfigAction configAction = new ConfigAction();
    private BundleContext bundleContext;
    private ConfigVariables configVariables;

    public ServerXMLVariableConfigSource() {
        this(Config13Constants.SERVER_XML_VARIABLE_ORDINAL, Tr.formatMessage(tc, "server.xml.variables.config.source"));
    }

    protected ServerXMLVariableConfigSource(int ordinal, String id) {
        super(ordinal, id);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {

        Map<String, String> props = new HashMap<>();

        Map<String, String> serverXMLVariables = null;

        if (System.getSecurityManager() == null) {
            serverXMLVariables = getServerXMLVariables();
        } else {
            serverXMLVariables = AccessController.doPrivileged(configAction);
        }

        if (serverXMLVariables != null) {
            props.putAll(serverXMLVariables);
        }

        return props;
    }

    private BundleContext getBundleContext() {
        if (this.bundleContext == null) {
            this.bundleContext = OSGiConfigUtils.getBundleContext(getClass());
        }
        return this.bundleContext;
    }

    @FFDCIgnore(InvalidFrameworkStateException.class)
    protected ConfigVariables getConfigVariables() {
        if (this.configVariables == null) {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext != null) {
                try {
                    this.configVariables = OSGiConfigUtils.getConfigVariables(bundleContext);
                } catch (InvalidFrameworkStateException e) {
                    //OSGi framework is shutting down, ignore and return null;
                }
            }
        }
        return this.configVariables;
    }

    private class ConfigAction implements PrivilegedAction<Map<String, String>> {
        /** {@inheritDoc} */
        @Override
        public Map<String, String> run() {
            return getServerXMLVariables();
        }

    }

    protected Map<String, String> getServerXMLVariables() {
        Map<String, String> props = new HashMap<>();
        ConfigVariables configVariables = getConfigVariables();
        if (configVariables != null) {//configVariables could be null if not inside an OSGi framework (e.g. unit test) or if framework is shutting down
            props = OSGiConfigUtils.getVariablesFromServerXML(configVariables);
        }
        return props;
    }

}
