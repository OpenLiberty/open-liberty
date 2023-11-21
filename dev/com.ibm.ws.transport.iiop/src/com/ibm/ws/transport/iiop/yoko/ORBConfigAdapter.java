/*******************************************************************************
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.yoko;

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.config.ConfigException;
import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.OrbConfigurator;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

/**
 * A ConfigAdapter instance for the Apache Yoko CORBA support.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "orb.type=yoko", "service.ranking:Integer=5" })
public class ORBConfigAdapter implements com.ibm.ws.transport.iiop.config.ConfigAdapter {
    private static final TraceComponent tc = Tr.register(ORBConfigAdapter.class);

    /**
     * Create an ORB for a CORBABean server context.
     * 
     * @param server The CORBABean that owns this ORB's configuration.
     * 
     * @return An ORB instance configured for the CORBABean.
     * @exception ConfigException
     */
    @Override
    public ORB createServerORB(Map<String, Object> config, Map<String, Object> extraConfig, List<IIOPEndpoint> endpoints, Collection<OrbConfigurator> orbConfigurators) throws ConfigException {
        ORB orb = createORB(translateToTargetArgs(config, orbConfigurators), translateToTargetProps(config, extraConfig, endpoints, orbConfigurators));
        return orb;
    }

    /**
     * Create an ORB for a CSSBean client context.
     * 
     * @return An ORB instance configured for this client access.
     * @exception ConfigException
     */
    @Override
    public ORB createClientORB(Map<String, Object> clientProps, Collection<OrbConfigurator> subsystemFactories)  {
        return createORB(translateToClientArgs(clientProps, subsystemFactories), translateToClientProps(clientProps, subsystemFactories));
    }

    /**
     * Create an ORB instance using the configured argument
     * and property bundles.
     * 
     * @param args The String arguments passed to ORB.init().
     * @param props The property bundle passed to ORB.init().
     * 
     * @return An ORB constructed from the provided args and properties.
     */
    private ORB createORB(String[] args, Properties props) {
        return ORB.init(args, props);
    }

    /**
     * Generate an array of arguments used to initialise an ORB instance.
     */
    private String[] translateToTargetArgs(Map<String, Object> props, Collection<OrbConfigurator> orbConfigurators) {
        final ArrayList<String> list = new ArrayList<>();
        orbConfigurators.forEach(oc -> oc.addTargetORBInitArgs(props, list));
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Configargs: " + list);
        return list.stream().toArray(String[]::new);
    }

    private Properties translateToTargetProps(Map<String, Object> config, Map<String, Object> extraConfig, List<IIOPEndpoint> endpoints,
                                              Collection<OrbConfigurator> subsystemFactories) {
        Properties result = createYokoORBProperties();
        for (OrbConfigurator sf : subsystemFactories) {
            addInitializerPropertyForSubsystem(result, sf, true);
            sf.addTargetORBInitProperties(result, config, endpoints, extraConfig);
        }
        if (!result.containsKey("yoko.orb.oa.endpoint") && !endpoints.isEmpty()) {
            // don't specify the port if we're allowing this to default.
            IIOPEndpoint endpoint = endpoints.get(0);
            int port = endpoint.getIiopPort();
            String host = endpoint.getHost();

            if (port > 0) {
                result.put("yoko.orb.oa.endpoint", "iiop --bind " + host + " --host " + host + " --port " + port);
            }
            else {
                result.put("yoko.orb.oa.endpoint", "iiop --bind " + host + " --host " + host);
            }
        }
        return result;
    }

    private Properties createYokoORBProperties() {
        Properties result = new Properties();
        result.put("org.omg.CORBA.ORBClass", "org.apache.yoko.orb.CORBA.ORB");
        result.put("org.omg.CORBA.ORBSingletonClass", "org.apache.yoko.orb.CORBA.ORBSingleton");
        return result;
    }

    private void addInitializerPropertyForSubsystem(Properties props, OrbConfigurator orbConfigurators, boolean endpoint) {
        String initializerClassName = orbConfigurators.getInitializerClassName(endpoint);
        if (initializerClassName == null) return;
        props.put("org.omg.PortableInterceptor.ORBInitializerClass." + initializerClassName, "");
    }

    /**
     * Translate client configuration into the
     * argument bundle needed to instantiate the
     * client ORB instance.
     * 
     * @return A String array to be passed to ORB.init().
     * @exception ConfigException if configuration cannot be interpreted
     */
    private String[] translateToClientArgs(Map<String, Object> clientProps, Collection<OrbConfigurator> orbConfigurators) {
        final ArrayList<String> list = new ArrayList<>();
        orbConfigurators.forEach(oc -> oc.addClientORBInitArgs(clientProps, list));
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Configargs: " + list);
        return list.stream().toArray(String[]::new);
    }

    /**
     * Translate client configuration into the
     * property bundle necessary to configure the
     * client ORB instance.
     * 
     * @param clientProps configuration properties
     * @param orbConfigurators configured subsystem factories
     * 
     * @return A property bundle that can be passed to ORB.init();
     * @exception ConfigException if configuration cannot be interpreted
     */
    private Properties translateToClientProps(Map<String, Object> clientProps, Collection<OrbConfigurator> orbConfigurators) {
        Properties result = createYokoORBProperties();
        orbConfigurators.forEach(oc -> {
            addInitializerPropertyForSubsystem(result, oc, false);
            oc.addClientORBInitProperties(result, clientProps);
        });
        return result;
    }
}
