/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine.factory;

import javax.naming.Reference;

import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionScope;

/**
 * Instances of this interface are used to create Reference objects with
 * JNDI lookup information for Resource References, which the caller then binds
 * to a JNDI name space. When the object is looked up, the associated factory
 * uses the JNDI information and component or environment specific information
 * to obtain an initial JNDI context and perform a lookup on the initial context.
 * <p>
 * This factory is very similar to a javax.naming.IndirectJndiLookupFactory,
 * but is intended specifically for Resource References, and allows the lookup
 * to take into account the environment of the caller, such as the current
 * ComponentMetaData (server) or local naming context (client). <p>
 **/
public interface ResRefReferenceFactory
{
    /**
     * This method creates a Resource Ref JNDI Lookup Reference based on a
     * JNDI lookup name, for the specified Resource Ref or Resource annotation
     * name. <p>
     *
     * @param compNSConfig the component namespace configuration
     * @param scope the scope of the resource reference
     * @param resRef the resource reference
     *
     * @return the Resource Ref JNDI Lookup Reference that was created.
     * @throws InjectionException
     **/
    public Reference createResRefJndiLookup(ComponentNameSpaceConfiguration compNSConfig,
                                            InjectionScope scope,
                                            ResourceRefInfo resRef)
                    throws InjectionException;
}
