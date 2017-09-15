/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.yoko;

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
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

/**
 * A ConfigAdapter instance for the Apache Yoko
 * CORBA support.
 * 
 * @version $Revision: 497125 $ $Date: 2007-01-17 10:51:30 -0800 (Wed, 17 Jan 2007) $
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
    public ORB createServerORB(Map<String, Object> config, Map<String, Object> extraConfig, List<IIOPEndpoint> endpoints, Collection<SubsystemFactory> subsystemFactories) throws ConfigException {
        ORB orb = createORB(translateToTargetArgs(config, subsystemFactories), translateToTargetProps(config, extraConfig, endpoints, subsystemFactories));
        return orb;
    }

    /**
     * Create an ORB for a CSSBean client context.
     * 
     * @return An ORB instance configured for this client access.
     * @exception ConfigException
     */
    @Override
    public ORB createClientORB(Map<String, Object> clientProps, Collection<SubsystemFactory> subsystemFactories) throws ConfigException {
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
     * Translate a CORBABean configuration into an
     * array of arguments used to configure the ORB
     * instance.
     * 
     * @param server The IiopEndpoint we're creating an ORB instance for.
     * @param subsystemFactories subsystem factories to translate configuration
     * 
     * @return A String{} array containing the initialization
     *         arguments.
     * @exception ConfigException if configuration cannot be interpreted
     */
    private String[] translateToTargetArgs(Map<String, Object> props, Collection<SubsystemFactory> subsystemFactories) throws ConfigException {
        ArrayList<String> list = new ArrayList<String>();

        for (SubsystemFactory sf : subsystemFactories) {
            sf.addTargetORBInitArgs(props, list);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configargs: " + list);
        }

        return list.toArray(new String[list.size()]);
    }

    private Properties translateToTargetProps(Map<String, Object> config, Map<String, Object> extraConfig, List<IIOPEndpoint> endpoints,
                                              Collection<SubsystemFactory> subsystemFactories) throws ConfigException {
        Properties result = createYokoORBProperties();
        for (SubsystemFactory sf : subsystemFactories) {
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

    private void addInitializerPropertyForSubsystem(Properties props, SubsystemFactory subsystemFactory, boolean endpoint) {
        String initializerClassName = subsystemFactory.getInitializerClassName(endpoint);
        if (initializerClassName != null) {
            props.put("org.omg.PortableInterceptor.ORBInitializerClass." + initializerClassName, "");
        }
    }

    /**
     * Translate client configuration into the
     * argument bundle needed to instantiate the
     * client ORB instance.
     * 
     * @param clientProps configuration properties
     * @param subsystemFactories configured subsystem factories
     * @return A String array to be passed to ORB.init().
     * @exception ConfigException if configuration cannot be interpreted
     */
    private String[] translateToClientArgs(Map<String, Object> clientProps, Collection<SubsystemFactory> subsystemFactories) throws ConfigException {
        ArrayList<String> list = new ArrayList<String>();

        for (SubsystemFactory sf : subsystemFactories) {
            sf.addClientORBInitArgs(clientProps, list);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configargs: " + list);
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Translate client configuration into the
     * property bundle necessary to configure the
     * client ORB instance.
     * 
     * @param clientProps configuration properties
     * @param subsystemFactories configured subsystem factories
     * 
     * @return A property bundle that can be passed to ORB.init();
     * @exception ConfigException if configuration cannot be interpreted
     */
    private Properties translateToClientProps(Map<String, Object> clientProps, Collection<SubsystemFactory> subsystemFactories) throws ConfigException {
        Properties result = createYokoORBProperties();
        for (SubsystemFactory sf : subsystemFactories) {
            addInitializerPropertyForSubsystem(result, sf, false);
            sf.addClientORBInitProperties(result, clientProps);
        }
        return result;
    }

}
