/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.resource;

/**
 * Factory for resources accessible to applications. Implementations should
 * be registered in the OSGi service registry with the {@link #JNDI_NAME} and {@link #CREATES_OBJECT_CLASS} properties.
 */
public interface ResourceFactory
{
    /**
     * The service registry property that specifies the JNDI lookup name of the
     * factory.
     */
    String JNDI_NAME = "jndiName";

    /**
     * The service registry property that specifies the String class (or
     * String[] classes) that the factory supports creating.
     */
    String CREATES_OBJECT_CLASS = "creates.objectClass";

    /**
     * Creates a resource handle of the specified type that respects the
     * specified resource information. The {@link ResourceInfo#getType} must
     * match the {@link #CREATES_OBJECT_CLASS} property. If the caller does not
     * provide resource information, the implementation can use {@link ResourceConfigFactory#createResourceConfig} to create a default.
     *
     * @param info the resource information, or null if unavailable
     * @return the resource handle
     * @throws Exception a resource-specific exception
     * @see ResourceConfigFactory#createResourceConfig
     */
    Object createResource(ResourceInfo info)
                    throws Exception;
}
