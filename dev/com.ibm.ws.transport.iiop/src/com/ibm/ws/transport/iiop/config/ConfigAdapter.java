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
package com.ibm.ws.transport.iiop.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.omg.CORBA.ORB;

import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

/**
 * Translates TSS and CSS configurations into CORBA startup args and properties.
 */
public interface ConfigAdapter {
    /**
     * Create an ORB for a CORBABean server context.
     * 
     * @param endpoints TODO
     * @param subsystemFactories TODO
     * @param server The CORBABean that owns this ORB's configuration.
     * 
     * @return An ORB instance configured for the CORBABean.
     * @exception ConfigException
     */
    public ORB createServerORB(Map<String, Object> config, Map<String, Object> extraConfig, List<IIOPEndpoint> endpoints, Collection<SubsystemFactory> subsystemFactories) throws ConfigException;

    /**
     * Create an ORB for a CSSBean client context.
     * 
     * @param client The configured CSSBean used for access.
     * @param subsystemFactories TODO
     * 
     * @return An ORB instance configured for this client access.
     * @exception ConfigException
     */
    public ORB createClientORB(Map<String, Object> clientProperties, Collection<SubsystemFactory> subsystemFactories) throws ConfigException;
}
