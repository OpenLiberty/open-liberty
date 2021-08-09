/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
public interface ResAutoLinkReferenceFactory
{
    /**
     * This method creates a Resource JNDI Lookup Reference based on a
     * JNDI lookup name, for the specified resource.
     */
    public Reference createResAutoLinkReference(ResourceInfo resourceInfo);

}
