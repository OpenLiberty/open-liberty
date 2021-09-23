/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 * A ConfigSource which returns values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" value="my_value" />
 *
 */
public class ServerXMLVariableConfigSource extends InternalConfigSource implements ConfigSource {

    private static final TraceComponent tc = Tr.register(ServerXMLVariableConfigSource.class);
    private final GetServerXMLVariablesAction getServerXMLVariablesAction = new GetServerXMLVariablesAction();
    private final String name;
    private BundleContext bundleContext;
    private ConfigVariables configVariables;

    @Trivial
    public ServerXMLVariableConfigSource() {
        name = Tr.formatMessage(tc, "server.xml.variables.config.source");
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ServerXMLConstants.SERVER_XML_VARIABLE_ORDINAL;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        if (System.getSecurityManager() == null) {
            return getServerXMLVariables();
        } else {
            return AccessController.doPrivileged(getServerXMLVariablesAction);
        }
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

    private class GetServerXMLVariablesAction implements PrivilegedAction<Map<String, String>> {
        /** {@inheritDoc} */
        @Override
        public Map<String, String> run() {
            return getServerXMLVariables();
        }

    }

    protected Map<String, String> getServerXMLVariables() {
        Map<String, String> props;
        ConfigVariables configVariables = getConfigVariables();
        if (configVariables != null) {//configVariables could be null if not inside an OSGi framework (e.g. unit test) or if framework is shutting down
            props = OSGiConfigUtils.getVariablesFromServerXML(configVariables);
        } else {
            props = Collections.emptyMap();
        }
        return props;
    }

}
